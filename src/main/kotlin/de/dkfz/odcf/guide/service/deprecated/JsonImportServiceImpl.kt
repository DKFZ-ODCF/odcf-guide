package de.dkfz.odcf.guide.service.deprecated

import com.google.gson.Gson
import de.dkfz.odcf.guide.ImportSourceDataRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.ImportSourceData
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helperObjects.importObjects.SampleImportObject
import de.dkfz.odcf.guide.helperObjects.importObjects.SubmissionImportObject
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Deprecated("This importer not used anymore")
@Service
open class JsonImportServiceImpl(
    private val importService: ImportService,
    private val seqTypeMappingService: SeqTypeMappingService,
    private val ldapService: LdapService,
    private val collectorService: CollectorService,
    private val submissionService: SubmissionService,
    private val sampleRepository: SampleRepository,
    private val submissionRepository: SubmissionRepository,
    private val importSourceDataRepository: ImportSourceDataRepository
) : JsonImportService {

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @Throws(DuplicateKeyException::class)
    @Transactional(rollbackFor = [Exception::class])
    override fun import(json: SubmissionImportObject, identifier: String): Submission {
        if (submissionRepository.findByIdentifier(identifier) != null) {
            throw DuplicateKeyException("Identifier: [$identifier] already exists.")
        }
        transferLibraryLayoutToSamples(json)
        val submission = saveSubmission(identifier, json)
        val serialized = Gson().toJson(json)
        val importSourceData = ImportSourceData(submission.identifier, serialized)
        importSourceDataRepository.saveAndFlush(importSourceData)
        return submission
    }

    @Transactional(rollbackFor = [Exception::class])
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    override fun reimport(submission: Submission) {
        if (submission.resettable) {
            submissionService.changeSubmissionState(submission, Submission.Status.LOCKED, ldapService.getPerson().username, "in preparation for reset of submission")
            sampleRepository.deleteAll(submission.samples)
            submission.samples = mutableListOf()
            val importSourceData = importSourceDataRepository.findBySubmissionIdentifier(submission.identifier)
                ?: throw IllegalArgumentException("Did not find import source data for submission with id '${submission.identifier}'.")
            val submissionImportObject = Gson().fromJson(importSourceData.jsonContent, SubmissionImportObject::class.java)
            transferLibraryLayoutToSamples(submissionImportObject)
            saveSamples(submission, submissionImportObject)
            submissionService.changeSubmissionState(submission, Submission.Status.RESET, ldapService.getPerson().username)
        } else {
            throw IllegalStateException(
                bundle.getString("details.resetNotPossible.explanation")
                    .replace("{0}", collectorService.getFormattedIdentifier(submission.identifier))
            )
        }
    }

    override fun saveSubmission(identifier: String, json: SubmissionImportObject): Submission {
        val submission = ApiSubmission(
            identifier,
            UUID.randomUUID(),
            json.otrsTicketNumber,
            ldapService.getPersonByMail(json.userMail),
            json.sequencingType
        )
        submission.importDate = Date()
        submissionRepository.saveAndFlush(submission)
        return if (json.samples == null) {
            submission
        } else saveSamples(submission, json)
    }

    private fun saveSamples(submission: Submission, json: SubmissionImportObject): Submission {
        val originProjectSet = emptySet<String>()
        for (sampleImportObject in json.samples!!) {
            val sample = saveSample(submission, sampleImportObject)
            originProjectSet.plus(sample.project)
        }
        submission.originProjects = originProjectSet.joinToString(";")
        submissionRepository.saveAndFlush(submission)
        return submission
    }

    private fun saveSample(submission: Submission, sampleImportObject: SampleImportObject): Sample {
        val sample = Sample(submission)
        sample.pid = sampleImportObject.pid
        sample.setSex(sampleImportObject.sex)
        sample.sampleType = sampleImportObject.sampleType.lowercase()
        sample.name = sampleImportObject.sampleIdentifier
        sample.project = sampleImportObject.project
        sample.seqType = seqTypeMappingService.getSeqType(sampleImportObject.seqType)
        sample.setLibraryLayout(sampleImportObject.libraryLayout)
        sample.tagmentationLibrary = importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleIdentifier, sampleImportObject.tagmentationLibrary)
        sample.antibodyTarget = sampleImportObject.antibodyTarget
        sampleRepository.saveAndFlush(sample)
        return sample
    }

    private fun transferLibraryLayoutToSamples(json: SubmissionImportObject): SubmissionImportObject {
        if (json.samples != null) {
            for (sample in json.samples!!) {
                sample.libraryLayout = json.libraryLayout
            }
        }
        return json
    }

    private fun getSingleCellStatusFromSeqType(seqType: String): Boolean {
        val st = seqTypeMappingService.getSeqType(seqType) ?: return false
        return st.singleCell
    }

    private fun getTagmentationStatusFromSeqType(seqType: String): Boolean {
        val st = seqTypeMappingService.getSeqType(seqType) ?: return false
        return st.tagmentation
    }
}
