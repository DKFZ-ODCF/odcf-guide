package de.dkfz.odcf.guide.service.implementation.importer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.Submission.Status.AUTO_CLOSED
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.exceptions.UserNotFoundException
import de.dkfz.odcf.guide.helperObjects.importObjects.ExternalIlseSubmissionImportObject
import de.dkfz.odcf.guide.helperObjects.importObjects.IlseSampleImportObject
import de.dkfz.odcf.guide.helperObjects.mapDistinctAndNotNullOrBlank
import de.dkfz.odcf.guide.service.interfaces.*
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.IlseApiService
import de.dkfz.odcf.guide.service.interfaces.importer.IlseImportService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.concurrent.thread

@Service
open class IlseImportServiceImpl(
    private val importService: ImportService,
    private val seqTypeMappingService: SeqTypeMappingService,
    private val ldapService: LdapService,
    private val ilseService: IlseApiService,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val submissionService: SubmissionService,
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val pseudonymService: PseudonymService,
    private val speciesService: SpeciesService,
    private val sequencingTechnologyService: SequencingTechnologyService,
    private val sampleRepository: SampleRepository,
    private val submissionRepository: SubmissionRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val sequencingTechnologyRepository: SequencingTechnologyRepository,
    private val validationLevelRepository: ValidationLevelRepository,
) : IlseImportService {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val otherProject = "000_OTHER"

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @Transactional(rollbackFor = [Exception::class])
    @Throws(
        DuplicateKeyException::class,
        ExternalApiReadException::class,
        JsonProcessingException::class,
        JsonMappingException::class
    )
    override fun import(ilse: Int, ticketNumber: String): Submission? {
        val identifier: String = importService.generateIlseIdentifier(ilse)
        if (submissionRepository.findByIdentifier(identifier) != null) {
            throw DuplicateKeyException("Identifier: [$identifier] already exists.")
        }
        val ilseObject = ilseService.getSubmissionImportObjectFromApi(ilse, true)
        detectUnknownFields(ilseObject, ilse)
        return saveSubmission(identifier, ilseObject, ticketNumber)
    }

    @Transactional(rollbackFor = [Exception::class])
    @Throws(IllegalStateException::class)
    override fun reimport(submission: Submission) {
        if (submission.resettable) {
            submissionService.changeSubmissionState(submission, Submission.Status.LOCKED, ldapService.getPerson().username, "in preparation for reset of submission")
            sampleRepository.deleteAll(sampleRepository.findAllBySubmission(submission))

            val ilseId = submission.identifier.substring(1).toInt()
            saveSamples(submission, ilseService.getSubmissionImportObjectFromApi(ilseId, true))
            submissionService.changeSubmissionState(submission, Submission.Status.RESET, ldapService.getPerson().username)
        } else {
            throw IllegalStateException("Could not reset submission ${submission.identifier}")
        }
    }

    override fun saveSubmission(
        identifier: String,
        importObject: ExternalIlseSubmissionImportObject,
        ticketNumber: String
    ): Submission {
        val submission = ApiSubmission(
            identifier,
            UUID.randomUUID(),
            ticketNumber.ifBlank {
                importService.createTicket(
                    "S#${importObject.samples!!.first().submission_id}",
                    importObject.samples!!.map { it.odcf_project }.distinct()
                )
            },
            null
        )
        submission.importDate = Date()
        if (importObject.samples == null) {
            submissionRepository.saveAndFlush(submission)
            return submission
        }
        val sample = importObject.samples!![0]
        submission.submitter = try {
            ldapService.getPersonByUsername(sample.submitter)
        } catch (e: UserNotFoundException) {
            ldapService.getPersonByUsername("guide")
        }
        submission.setType(sample.submission_type)
        submission.fasttrack = sample.fasttrack
        val sequencingTechnology = sequencingTechnologyService.getSequencingTechnology(sample.sequencing_type)
        submission.sequencingTechnology = if (sequencingTechnology != null) {
            sequencingTechnology
        } else {
            mailSenderService.sendMailToTicketSystem(
                subject = mailBundle.getString("importer.sequencingTechnologyUnknownSubject")
                    .replace("{0}", mailContentGeneratorService.getTicketSubjectPrefix(submission)),
                body = mailBundle.getString("importer.sequencingTechnologyUnknownBody")
                    .replace("{0}", submission.identifier)
                    .replace("{1}", sample.sequencing_type)
            )
            sequencingTechnologyRepository.findByDefaultObjectIsTrue().single()
        }
        submission.validationLevel = sequencingTechnology?.validationLevel ?: validationLevelRepository.findByDefaultObjectIsTrue().single()
        submissionRepository.saveAndFlush(submission)
        saveSamples(submission, importObject)
        if (externalMetadataSourceService.getSingleValue("isParsableProject", mapOf("project" to sample.odcf_project)).toBoolean()) {
            submissionService.changeSubmissionState(submission, AUTO_CLOSED, "")
        }
        return submission
    }

    /**
     * Saves multiple samples to a given submission.
     * If there is an unknown species, puts the submission on hold and sends a mail to the ticket system.
     *
     * @param submission Submission object to which the samples belong to
     * @param importObject List of multiple sample import objects containing all information about the imported samples to be saved
     * @return Newly saved submission object
     */
    fun saveSamples(submission: Submission, importObject: ExternalIlseSubmissionImportObject): Submission {
        if (importObject.samples == null) {
            return submission
        }
        val unknown = emptyMap<String, MutableSet<String>>().toMutableMap()
        var originProjectSet = emptySet<String>()
        for (ilseSample in importObject.samples!!) {
            val sample = saveSample(submission, ilseSample)
            detectUnknownValues(ilseSample, unknown)
            if (sample.project != otherProject) {
                originProjectSet = originProjectSet.plus(sample.project)
            }
        }
        submission.originProjects = originProjectSet.joinToString(";")
        submissionRepository.saveAndFlush(submission)
        if (sampleRepository.findAllBySubmission(submission).size != importObject.samples!!.size) {
            logger.error("number of saved samples different to number of ilse samples")
            throw IllegalStateException("number of saved samples different to number of ilse samples")
        }

        if (unknown.isNotEmpty()) {
            if (unknown["species"].isNullOrEmpty().not()) {
                submissionService.changeSubmissionState(
                    submission = submission,
                    status = Submission.Status.ON_HOLD,
                    stateComment = "Found unknown species [${unknown["species"]!!.joinToString()}]. It has to be verified by the ODCF."
                )
            }
            val subject = "${mailContentGeneratorService.getTicketSubjectPrefix(submission)} I found unknown field values"
            mailSenderService.sendMailToTicketSystem(subject, unknown.toString())
        }
        return submission
    }

    /**
     * Creates a new Sample object and saves all the imported data about it.
     *
     * @param submission Submission object to which the sample belongs to
     * @param sampleImportObject Object containing all information about an imported sample to be saved
     * @return Newly created Sample object
     */
    fun saveSample(submission: Submission, sampleImportObject: IlseSampleImportObject): Sample {
        val seqType = seqTypeMappingService.getSeqType(sampleImportObject.type)
        val sample = Sample(submission)
        sample.pid = sampleImportObject.pseudonym
        sample.abstractSampleId = sampleImportObject.asid
        sample.setSex(sampleImportObject.sex)
        sample.name = sampleImportObject.sampleName
        sample.project = sampleImportObject.odcf_project.takeIf { it != otherProject && externalMetadataSourceService.getSetOfValues("projectsWithAliases").contains(it) }.orEmpty()
        sample.parseIdentifier = sampleImportObject.sampleName
        sample.seqType = seqType
        sample.read1Length = sampleImportObject.read_1_length
        sample.read2Length = sampleImportObject.read_2_length
        sample.setLibraryLayout(if (sampleImportObject.read_2_length > 0) "paired" else "single")
        sample.tagmentationLibrary = importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleName)
        sample.antibodyTarget = sampleImportObject.antibody_target
        sample.comment = sampleImportObject.odcf_comment
        sample.tissue = sampleImportObject.tissue
        sample.requestedSequencingInfo = sampleImportObject.sequencing_type
        sample.baseMaterial = sampleImportObject.base_material
        sample.requestedLanes = sampleImportObject.lanes
        sample.libraryPreparationKit = sampleImportObject.libprepKit
        sample.indexType = sampleImportObject.indexType
        sample.protocol = sampleImportObject.protocol
        sample.setProceed(sampleImportObject.proceed)
        sample.speciesWithStrain = speciesService.getSpeciesWithStrainForSpecies(sampleImportObject.species)
        sample.unknownValues = if (sampleImportObject.unknown.isNotEmpty()) sampleImportObject.unknown.mapValues { it.value.toString() } else null

        if (sampleImportObject.odcf_single_cell_well_label.contains("-")) {
            val singleCellValues = sampleImportObject.odcf_single_cell_well_label.split("-")
            sample.singleCellPlate = singleCellValues[0]
            sample.singleCellWellPosition = singleCellValues[1]
        } else {
            sample.singleCellWellPosition = sampleImportObject.odcf_single_cell_well_label
        }

        sampleRepository.saveAndFlush(sample)
        return sample
    }

    override fun detectUnknownFields(values: ExternalIlseSubmissionImportObject, ilseId: Int) {
        val unknownFields = emptySet<String>().toMutableSet()
        if (values.samples != null) {
            values.samples!!.forEach {
                if (it.unknown.size > 0) {
                    unknownFields.addAll(it.unknown.map { it1 -> it1.key })
                }
            }
            if (unknownFields.isNotEmpty()) {
                mailSenderService.sendMailToTicketSystem(
                    "Unknown field detected for S#$ilseId",
                    "Hello,\n\nI detected new field(s) (${unknownFields.joinToString()}) while importing from ILSe API. " +
                        "Please have a look at this."
                )
            }
        }
    }

    /**
     * Checks the seqType, project and species values of the sampleImportObject and
     * adds them to the unknown values map if they are not found to the GUIDE.
     *
     * @param sampleImportObject Object containing all information about an imported sample to be saved
     * @param unknown Map of unknown values from the sampleImportObject
     */
    override fun detectUnknownValues(sampleImportObject: IlseSampleImportObject, unknown: MutableMap<String, MutableSet<String>>) {
        if (seqTypeMappingService.getSeqType(sampleImportObject.type) == null) {
            if (unknown["seqType"].isNullOrEmpty()) {
                unknown["seqType"] = mutableSetOf(sampleImportObject.type)
            } else {
                unknown["seqType"]!!.add(sampleImportObject.type)
            }
        }
        if (!externalMetadataSourceService.getSetOfValues("projectsWithAliases").contains(sampleImportObject.odcf_project)) {
            unknown["project"] = mutableSetOf(sampleImportObject.odcf_project)
        }
        val acceptedSpecies = runtimeOptionsRepository.findByName("unregisteredButAcceptedForImportSpecies")?.value.orEmpty().split("+")
        val unknownSpecies = sampleImportObject.species.split("+")
            .map { it.trim() }
            .filterNot { speciesService.getSpeciesForImport().contains(it) || acceptedSpecies.contains(it) }
        if (unknownSpecies.isNotEmpty()) {
            if (unknown["species"].isNullOrEmpty()) {
                unknown["species"] = unknownSpecies.toMutableSet()
            } else {
                unknown["species"]!!.addAll(unknownSpecies)
            }
        }
    }

    override fun summariesAfterImport(submission: ApiSubmission) {
        if (submission.isAutoClosed) {
            thread(start = true) {
                pseudonymService.checkSimilarPidsAndSendMail(submission)
            }
        }
        val projects = sampleRepository.findAllBySubmission(submission).mapDistinctAndNotNullOrBlank { it.project }.joinToString(transform = { "'$it'" })
        if (submission.fasttrack && externalMetadataSourceService.getSingleValue("projectNotificationStatus", mapOf("projects" to projects)).toBoolean()) {
            mailSenderService.sendMailFasttrackImported(submission)
        }
    }
}
