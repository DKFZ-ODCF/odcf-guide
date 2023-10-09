package de.dkfz.odcf.guide.service.implementation.importer

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.*
import de.dkfz.odcf.guide.entity.metadata.*
import de.dkfz.odcf.guide.entity.submissionData.*
import de.dkfz.odcf.guide.exceptions.*
import de.dkfz.odcf.guide.service.interfaces.*
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.importer.CsvImportService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import de.dkfz.odcf.guide.service.interfaces.validator.SampleService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.*
import javax.management.relation.RelationException
import javax.persistence.EntityManager
import kotlin.concurrent.thread
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

@Service
open class CsvImportServiceImpl(
    private val importService: ImportService,
    private val seqTypeMappingService: SeqTypeMappingService,
    private val ldapService: LdapService,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val fileService: FileService,
    private val metaDataColumnService: MetaDataColumnService,
    private val collectorService: CollectorService,
    private val sampleService: SampleService,
    private val mailService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val speciesService: SpeciesService,
    private val metaDataColumnRepository: MetaDataColumnRepository,
    private val sampleRepository: SampleRepository,
    private val technicalSampleRepository: TechnicalSampleRepository,
    private val fileRepository: FileRepository,
    private val submissionRepository: SubmissionRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val validationLevelRepository: ValidationLevelRepository,
    private val deletionService: DeletionService,
    private val submissionService: SubmissionService,
    private val entityManager: EntityManager
) : CsvImportService {

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())
    private val FILENAME_TO_IGNORE = "Undetermined"

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Throws(
        DuplicateKeyException::class,
        IOException::class,
        RelationException::class,
        ColumnNotFoundException::class,
        Md5SumFoundInDifferentSubmission::class,
        GuideRuntimeException::class
    )
    @Transactional(rollbackFor = [Exception::class])
    override fun import(file: MultipartFile, ticket: String, email: String, customName: String, comment: String, ignoreMd5Check: Boolean): Submission {
        val rows = if (file.originalFilename!!.endsWith(".xlsx")) {
            fileService.readFromXlsx(file.inputStream)
        } else if (file.originalFilename!!.endsWith(".xls")) {
            fileService.readFromXls(file.inputStream)
        } else {
            fileService.readFromCsv(file.inputStream)
        }
        val submission = saveSubmission(ticket, email, customName, comment)
        try {
            if (!ignoreMd5Check) checkMd5Sums(rows)
        } catch (e: Exception) {
            resetSubmissionNumber(submission, e)
        }
        thread(start = true) {
            try {
                submissionService.changeSubmissionState(
                    submission,
                    Submission.Status.ON_HOLD,
                    stateComment = "Submission is importing",
                    logComment = "async submission import started"
                )
                saveFilesAndSamples(submission, rows, override = false, initialUpload = true)
                submissionService.changeSubmissionState(submission, Submission.Status.IMPORTED, logComment = "async submission import finished")
            } catch (e: Exception) {
                val subject = "Failed to transfer metadata table to ODCF validation service"
                val body = mailContentGeneratorService.getMailBody(
                    "mailService.uploadedSubmissionFailedMailBody",
                    mapOf(
                        "{0}" to submission.submitter.fullName,
                        "{1}" to e.localizedMessage,
                    )
                )
                logger.info("async submission import of ${submission.identifier} canceled due to an exception")
                mailService.sendMailToSubmitter(subject, body, submission.submitter.mail)
                deletionService.deleteSubmission(submission, sendMail = false)
                resetSubmissionNumber(submission, e)
            }
            mailService.sendReceivedSubmissionMail(submission, sendToUser = submission.submitter.isAdmin.not())
        }
        return submission
    }

    private fun resetSubmissionNumber(submission: Submission, e: Exception) {
        val identifier = submission.identifier.filter { it.isDigit() }.toInt()
        entityManager.createNativeQuery("select setval('internal_submission_id', $identifier, false)").singleResult
        throw e
    }

    /*@Throws(GuideRuntimeException::class)
    @Transactional(rollbackFor = [Exception::class])
    override fun importAdditional(ilse: Int): Pair<Submission, Set<String>> {
        val pathToMidterm = env.getRequiredProperty("application.midterm").replace("<ILSE>", ilse.toString(), false)
        val rows = fileService.readTsvFile(pathToMidterm)
        var submission = submissionRepository.findByIdentifier(importService.generateIlseIdentifier(ilse))
        if (submission == null) {
            submission = saveSubmission("", ldapService.getPerson().mail)
        }
        if (rows.isEmpty()) {
            throw GuideMergerException("No rows given in the tsv file from midterm")
        }
        val warnings = rows.map { saveFileToSample(submission, it.toMutableMap()) }.flatten().toSet()
        return Pair(submission, warnings)
    }*/

    override fun saveSubmission(ticketNumber: String, email: String, customName: String, comment: String): Submission {
        val identifier = importService.generateInternalIdentifier()
        val submission = UploadSubmission(
            identifier,
            UUID.randomUUID(),
            ticketNumber.ifBlank { importService.createTicket(identifier, listOf("uploaded submission")) },
            ldapService.getPersonByMail(email),
            ""
        )
        submission.customName = customName
        submission.comment = comment
        submission.importDate = Date()
        submission.resettable = false
        submission.validationLevel = validationLevelRepository.findByDefaultObjectIsTrue().single()
        submission.startTerminationPeriod = Date()
        submissionRepository.saveAndFlush(submission)
        return submission
    }

    @Throws(RelationException::class, ColumnNotFoundException::class, Md5SumFoundInDifferentSubmission::class, GuideRuntimeException::class)
    fun checkMd5Sums(csvRows: List<Map<String, String>>?): Boolean {
        if (csvRows.isNullOrEmpty()) {
            return false
        }

        val md5sums = csvRows.map { metaDataColumnService.getValue("md5 sum", it) }
        val filesWithMatchingMd5 = fileRepository.findAllByMd5In(md5sums)
        val foundSubmissionsWithMd5 = if (filesWithMatchingMd5.isNotEmpty()) {
            filesWithMatchingMd5.map { it.sample.submission }
        } else {
            emptyList()
        }

        if (foundSubmissionsWithMd5.isNotEmpty()) {
            val message = bundle.getString("csvImport.md5AlreadyExists")
            val addition = "${filesWithMatchingMd5.groupBy { it.md5 }.size} of ${csvRows.size}"
            val countMd5InSubmissions = foundSubmissionsWithMd5.groupingBy { it }.eachCount()

            throw Md5SumFoundInDifferentSubmission(
                message.replace("<MD5_COUNT>", addition),
                foundSubmissionsWithMd5.distinct(),
                countMd5InSubmissions
            )
        }

        return true
    }

    @Throws(RelationException::class, IOException::class, ColumnNotFoundException::class, GuideRuntimeException::class)
    override fun saveFilesAndSamples(
        submission: Submission,
        csvRows: List<Map<String, String>>?,
        override: Boolean,
        initialUpload: Boolean,
    ): Submission {
        if (csvRows.isNullOrEmpty()) {
            if (override) {
                return submission
            }
            throw GuideRuntimeException("Submission is empty.")
        }
        if (sampleRepository.existsBySubmission(submission) && !override) {
            throw GuideRuntimeException("Submission isn't empty and override is not activated!")
        } else {
            csvRows.forEach { saveFileAndSample(submission, it.toMutableMap(), override, initialUpload) }
            if (!initialUpload) { sampleService.deletedFilesAndSamples(submission) }
        }
        return submission
    }

    /**
     * Saves the file and sample from one given CSV row to a specific submission.
     *
     * @param submission Submission to which the samples and files to be saved belong.
     * @param csvRow One row of the CSV file.
     * @param override Whether to override the existing data of the samples.
     * @param initialUpload Whether this is the first upload of this submission
     *                      (determines whether the UUID should exist in the CSV file).
     *
     * @throws RelationException If the filename property in the CSV row points to multiple associated samples.
     * @throws FastQFileNameRejectedException If the filename property in the CSV row has a suffix (file extension)
     *     that differs from the format "_[R|I][1|2].fastq.gz"
     * @throws ColumnNotFoundException If the columns for the fastq file name or the UUID are not found in the CSV row
     * @throws GuideRuntimeException If something went wrong during the process of saving and the sample object cannot be found
     *
     * @return The newly saved Sample object or `null` if the file associated with the sample object
     * is supposed to be ignored because the filename is "Undetermined"
     */
    @Throws(
        RelationException::class,
        IOException::class,
        FastQFileNameRejectedException::class,
        ColumnNotFoundException::class,
        GuideRuntimeException::class
    )
    fun saveFileAndSample(
        submission: Submission,
        csvRow: MutableMap<String, String>,
        override: Boolean,
        initialUpload: Boolean = false
    ): Sample? {
        val regexSuffix = runtimeOptionsRepository.findByName("fastqFileSuffix")!!.value

        val filename = metaDataColumnService.getValue("fastq file name", csvRow)
        if (filename.startsWith(FILENAME_TO_IGNORE) || csvRow.isEmpty()) {
            return null
        } else if (filename.isBlank() && !override) {
            throw FastQFileNameRejectedException("In one or more row(s), there was no fastq filename found.")
        } else if (!filename.matches(".*$regexSuffix\$".toRegex()) && !override) {
            throw FastQFileNameRejectedException("The fastq filename does not end with '$regexSuffix'")
        }

        val uuid = if (!initialUpload) {
            csvRow.remove("uuid (do not change)") ?: throw ColumnNotFoundException("Column 'uuid (do not change)' not found.")
        } else {
            ""
        }
        val file = if (uuid.isNotBlank()) fileRepository.findByUuid(UUID.fromString(uuid)) else null

        val fastqFile = prepareFileData(csvRow, file)

        var sample = importService.findSampleByFile(fastqFile, submission)
        val noSampleFound = sample == null
        if (noSampleFound || override) {
            sample = sample ?: Sample(submission)

            metaDataColumnRepository.findAll().filter { it.reflectionClassName == "Sample" }.forEach { column ->
                if (column.reflectionPropertyNameImport.isNotEmpty()) {
                    val value = metaDataColumnService.removeValueOrEmptyValue(column.columnName, csvRow)
                    when (column.reflectionPropertyNameImport) {
                        "sampleType" -> sample.sampleType = externalMetadataSourceService.getSingleValue("sampleTypeCaseInsensitive", mapOf("sampleType" to value))
                        "seqType" -> sample.seqType = seqTypeMappingService.getSeqType(value)
                        "speciesWithStrain" -> sample.speciesWithStrain = speciesService.getSpeciesWithStrainForSpecies(value)
                        else -> setValueToSample(sample, value, column)
                    }
                }
            }
        }
        sample ?: throw GuideRuntimeException("Sample not found. Can't continue saving :(")

        val projectForSubmission = collectorService.getImportableProjects(submission).map { it.replace(" (closed)", "") }
        if (!projectForSubmission.contains(sample.project)) {
            sample.project = ""
        }

        var technicalSample = sample.technicalSample
        if (technicalSample == null || override) {
            technicalSample = saveTechnicalSample(csvRow, technicalSample)
            sample.technicalSample = technicalSample
        } else {
            removeTechnicalSampleFromRow(csvRow)
        }

        if (noSampleFound || override) {
            sample.unknownValues = csvRow
        }

        sampleRepository.saveAndFlush(sample)
        fastqFile.sample = sample
        fileRepository.saveAndFlush(fastqFile)
        fastqFile.deletionFlag = false
        sample.deletionFlag = false
        return sample
    }

    /**
     * Saves additional file and technical metadata to an existing sample object.
     *
     * @property submission the submission to which the sample belongs
     * @property csvRow metadata row from the csv file
     * @throws GuideMergerException if an inconsistency is found
     * @throws ColumnNotFoundException if required column is not found
     * @return a set of warnings if something is inconsistent
     */
    /*@Throws(GuideMergerException::class, ColumnNotFoundException::class)
    fun saveFileToSample(submission: Submission, csvRow: MutableMap<String, String>): Set<String> {
        val warnings = emptySet<String>().toMutableSet()

        val filename = metaDataColumnService.getValue("fastq file name", csvRow)
        if (filename.isBlank()) throw GuideMergerException("The column file name is not given or empty. Please provide a filename for each row.")
        if (filename.startsWith(FILENAME_TO_IGNORE)) {
            return emptySet()
        }
        var sample = importService.findSampleByFileName(filename, submission)
        if (sample == null) {
            val sampleName = metaDataColumnService.getValue("sample name", csvRow)
            sample = sampleRepository.findBySubmissionAndName(submission, sampleName)
            if (sample == null) {
                saveFileAndSample(submission, csvRow, override = false, initialUpload = true)
                return emptySet()
            }
        }

        val fastqFile = prepareFileData(csvRow, null)
        fastqFile.sample = sample
        fileRepository.saveAndFlush(fastqFile)
        if (sample.files.isEmpty()) sample.files = listOf(fastqFile) else sample.files.toMutableList().add(fastqFile)

        val baseMaterial = metaDataColumnService.removeValueOrEmptyValue("base material", csvRow)
        if (sample.seqType != null && baseMaterial.lowercase().contains("single.?cell".toRegex()) != sample.seqType?.singleCell) {
            throw GuideMergerException(
                "Single cell inconsistency found in ${submission.identifier} while merging sample with name '${sample.name}'"
            )
        }

        metaDataColumnRepository.findAll().filter { it.reflectionClassName == "Sample" }.forEach { column ->
            if (column.reflectionPropertyNameImport.isNotEmpty()) {
                val value = metaDataColumnService.removeValueOrEmptyValue(column.columnName, csvRow)
                when (column.reflectionPropertyNameImport) {
                    "sampleType" -> if (sample.sampleType.isBlank()) sample.sampleType = externalMetadataSourceService.getSingleValue("sampleTypeCaseInsensitive", mapOf("sampleType" to value))
                    "seqType" -> {
                        val seqTypeFromFile = seqTypeMappingService.getSeqType(value)
                        if (sample.seqType == null) {
                            sample.seqType = seqTypeFromFile
                        } else if (sample.seqType != seqTypeFromFile) {
                            warnings.add(
                                bundle.getString("merging.warning.seqType")
                                    .replace("{0}", seqTypeFromFile!!.name)
                                    .replace("{1}", sample.seqType!!.name)
                            )
                        }
                    }
                    "antibodyTarget" -> {
                        if (sample.antibodyTarget.isEmpty()) {
                            sample.antibodyTarget = value
                        } else if (sample.antibodyTarget != value) {
                            warnings.add(
                                bundle.getString("merging.warning.antibodyTarget")
                                    .replace("{0}", value)
                                    .replace("{1}", sample.antibodyTarget)
                            )
                        }
                    }
                    "speciesWithStrain" -> if (sample.speciesWithStrain.isBlank()) sample.speciesWithStrain = speciesService.getSpeciesWithStrainForSpecies(value)
                    else -> setValueToSample(sample, value, column)
                }
            }
        }

        if (sample.technicalSample == null) sample.technicalSample = saveTechnicalSample(csvRow, null) else removeTechnicalSampleFromRow(csvRow)

        if (csvRow.isNotEmpty()) sample.unknownValues = csvRow

        if (!fastqFile.fileName.startsWith("/")) {
            val ilseNumber = String.format("%06d", submission.identifier.substring(1).toInt())
            val pathTemplate = runtimeOptionsRepository.findByName("fastqPathMidterm")?.value.orEmpty()
            val path = pathTemplate.replace("{1}", ilseNumber)
                .replace("{2}", sample.technicalSample!!.runId)
                .replace("{3}", fastqFile.fileName.split("_")[0])
                .replace("{4}", fastqFile.fileName)
            fastqFile.fileName = path
        }
        fileRepository.saveAndFlush(fastqFile)
        sampleRepository.saveAndFlush(sample)
        return warnings
    }*/

    private fun setValueToSample(sample: Sample, value: String, column: MetaDataColumn) {
        var propertyName = column.reflectionPropertyNameImport
        if (propertyName == "importIdentifier") { propertyName = "name" }
        val property = Sample::class.memberProperties.filterIsInstance<KMutableProperty<*>>().find { it.name == propertyName } ?: return

        val propertyValue = property.call(sample).toString()
        if (propertyValue.isBlank() || "null|false|UNKNOWN".toRegex().matches(propertyValue)) {
            if (column.reflectionPropertyNameImport.isNotBlank()) {
                val setter = property.setter
                if (setter.visibility != KVisibility.PRIVATE) {
                    setter.call(sample, value)
                } else {
                    val customSetter = Sample::class.memberFunctions.filter { it.name.contains(propertyName.replace("\\s".toRegex(), ""), true) && it.name.contains("set") }
                    try {
                        customSetter.single().call(sample, value)
                    } catch (_: Exception) {
                        logger.debug("Found no public or custom setter for property $propertyName")
                    }
                }
            }
        }
    }

    /**
     * Creates or updates a File object and saves the file information from a given CSV row to that object.
     *
     * @param csvRow One row of the CSV file to be imported
     * @param file If the file object already exists, it can be passed on to the function,
     *              otherwise the function creates a new object.
     * @return The newly saved File object
     */
    fun prepareFileData(csvRow: MutableMap<String, String>, file: File?): File {
        val fastqFile = file ?: File()

        metaDataColumnRepository.findAll().filter { it.reflectionClassName == "File" }.forEach { column ->
            if (column.reflectionPropertyNameImport.isNotEmpty()) {
                val propertyName = column.reflectionPropertyNameImport
                val property = File::class.memberProperties.filterIsInstance<KMutableProperty<*>>().find { it.name == propertyName }
                val value = metaDataColumnService.removeValueOrEmptyValue(column.columnName, csvRow)
                val valueToInsert = when (column.reflectionPropertyNameImport) {
                    "baseCount" -> if (value.isNotBlank()) value.toLong() else null
                    "cycleCount" -> if (value.isNotBlank()) value.toLong() else null
                    else -> value
                }
                property?.setter?.call(fastqFile, valueToInsert)
            }
        }
        return fastqFile
    }

    /**
     * Creates or updates a TechnicalSample object and saves the technical sample information from a given CSV row to that object.
     *
     * @param csvRow One row of the CSV file to be imported
     * @param maybeTechnicalSample If the technical sample object already exists, it can be passed on to the function,
     *                              otherwise the function creates a new object.
     * @return The newly saved TechnicalSample object
     */
    fun saveTechnicalSample(csvRow: MutableMap<String, String>, maybeTechnicalSample: TechnicalSample?): TechnicalSample {
        val technicalSample = maybeTechnicalSample ?: TechnicalSample()
        technicalSample.externalSubmissionId = csvRow.remove("ilse no").orEmpty()

        metaDataColumnRepository.findAll().filter { it.reflectionClassName == "TechnicalSample" }.forEach { column ->
            if (column.reflectionPropertyNameImport.isNotEmpty()) {
                val propertyName = column.reflectionPropertyNameImport
                val property = TechnicalSample::class.memberProperties.filterIsInstance<KMutableProperty<*>>().find { it.name == propertyName }
                val value = metaDataColumnService.removeValueOrEmptyValue(column.columnName, csvRow)
                val valueToInsert = when (column.reflectionPropertyNameImport) {
                    "lane" -> if (value != "") value.toInt() else null
                    "pipelineVersion" -> value.takeIf { value.isNotEmpty() } ?: "unknown"
                    "readCount" -> if (value != "") value.toInt() else null
                    else -> value
                }
                property?.setter?.call(technicalSample, valueToInsert)
            }
        }
        technicalSampleRepository.saveAndFlush(technicalSample)
        return technicalSample
    }

    /**
     * Removes the columns containing the technical sample from the given CSV row.
     *
     * @param csvRow One row of the CSV file to be imported
     */
    fun removeTechnicalSampleFromRow(csvRow: MutableMap<String, String>) {
        metaDataColumnRepository.findAll().filter { it.reflectionClassName == "TechnicalSample" }.forEach { column ->
            metaDataColumnService.removeValueOrEmptyValue(column.columnName, csvRow)
        }
    }
}
