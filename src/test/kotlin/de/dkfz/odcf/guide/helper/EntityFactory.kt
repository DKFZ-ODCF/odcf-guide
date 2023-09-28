package de.dkfz.odcf.guide.helper

import de.dkfz.odcf.guide.dtoObjects.FileGuiDto
import de.dkfz.odcf.guide.dtoObjects.SampleGuiDto
import de.dkfz.odcf.guide.entity.Feedback
import de.dkfz.odcf.guide.entity.MetaDataColumn
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.cluster.ClusterJobTemplate
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.metadata.SequencingTechnology
import de.dkfz.odcf.guide.entity.options.RuntimeOptions
import de.dkfz.odcf.guide.entity.otpCached.OtpCachedProject
import de.dkfz.odcf.guide.entity.parser.Parser
import de.dkfz.odcf.guide.entity.parser.ParserComponent
import de.dkfz.odcf.guide.entity.parser.ParserField
import de.dkfz.odcf.guide.entity.requestedValues.FieldRequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.SeqTypeRequestedValue
import de.dkfz.odcf.guide.entity.submissionData.*
import de.dkfz.odcf.guide.entity.validation.Validation
import de.dkfz.odcf.guide.entity.validation.ValidationLevel
import de.dkfz.odcf.guide.helperObjects.ParserForm
import de.dkfz.odcf.guide.helperObjects.importObjects.ExternalIlseSubmissionImportObject
import de.dkfz.odcf.guide.helperObjects.importObjects.IlseSampleImportObject
import de.dkfz.odcf.guide.helperObjects.importObjects.SampleImportObject
import de.dkfz.odcf.guide.helperObjects.importObjects.SubmissionImportObject
import java.util.*
import kotlin.random.Random.Default.nextInt

class EntityFactory {

    var number: Int = 1
    var colNumber: Int = 1

    fun getApiSubmission(): ApiSubmission {
        return ApiSubmission(
            String.format("i%07d", nextInt(10000, 99999)),
            UUID.randomUUID(),
            "ticketNumber",
            "samples"
        )
    }

    fun getUploadSubmission(status: Submission.Status): UploadSubmission {
        val submission = getUploadSubmission()
        submission.status = status
        return submission
    }

    fun getUploadSubmission(): UploadSubmission {
        return UploadSubmission(
            String.format("o%07d", nextInt(100000, 999999)),
            UUID.randomUUID(),
            "ticketNumber",
            "samples"
        )
    }

    fun getApiSubmission(state: Submission.Status): ApiSubmission {
        val submission = getApiSubmission()
        submission.status = state
        return submission
    }

    fun getApiSubmission(state: Submission.Status, identifierPrefix: String?): ApiSubmission {
        val submission = getApiSubmission(state)
        if (!identifierPrefix.isNullOrBlank()) {
            submission.identifier = submission.identifier.replace("i", identifierPrefix)
        }
        return submission
    }

    fun getSample(): Sample {
        return getSample(getApiSubmission())
    }

    fun getSample(submission: Submission): Sample {
        val sample = Sample(submission)
        sample.parseIdentifier = "parseIdentifier"
        sample.abstractSampleId = "abstractSampleId"
        sample.antibody = "antibody"
        sample.antibodyTarget = "antibodyTarget"
        sample.comment = "comment"
        sample.libraryPreparationKit = "libraryPreparationKit"
        sample.indexType = "indexType"
        sample.phenotype = ""
        sample.pid = "prefix_pid"
        sample.project = "project"
        sample.name = "sampleIdentifier"
        sample.speciesWithStrain = "Human (Homo sapiens) [No strain available]+Mouse (Mus musculus) [Unknown]"
        sample.tagmentationLibrary = "tagmentationLibrary"
        sample.setSex("m")
        sample.sampleType = "sample-type01"
        sample.seqType = getSeqType()
        sample.setLibraryLayout("paired")
        sample.singleCellPlate = "singleCellPlate"
        sample.singleCellWellPosition = "singleCellWellPosition"
        return sample
    }

    fun getTechnicalSample(): TechnicalSample {
        val technicalSample = TechnicalSample()
        technicalSample.center = "center"
        technicalSample.externalSubmissionId = "ilseNo"
        technicalSample.barcode = "barcode"
        technicalSample.instrumentModel = "instrModel"
        technicalSample.instrumentPlatform = "instrPlatform"
        technicalSample.lane = 1
        technicalSample.pipelineVersion = "pipeline"
        technicalSample.readCount = 1
        technicalSample.runDate = "2000-01-01"
        technicalSample.runId = "runId"
        technicalSample.sequencingKit = "sequencingKit"
        return technicalSample
    }

    fun getTechnicalSample(sample: Sample): TechnicalSample {
        val technicalSample = getTechnicalSample()
        sample.technicalSample = technicalSample
        return technicalSample
    }

    fun getSeqType(): SeqType {
        val seqType = SeqType()
        seqType.basicSeqType = "RNA"
        seqType.name = "RNA${number++}"
        return seqType
    }

    fun getSeqType(name: String, importAliases: String): SeqType {
        val seqType = getSeqType()
        seqType.name = name
        seqType.setImportAliases(importAliases)
        return seqType
    }

    fun getSeqTech(): SequencingTechnology {
        val seqTech = SequencingTechnology()
        seqTech.name = "seqTech"
        seqTech.validationLevel = getValidationLevel()
        return seqTech
    }

    fun getSingleCellSeqType(): SeqType {
        val seqType = SeqType()
        seqType.basicSeqType = "RNA"
        seqType.name = "scRNA${number++}"
        seqType.singleCell = true
        return seqType
    }

    fun getTagmentationCellSeqType(): SeqType {
        val seqType = SeqType()
        seqType.basicSeqType = "RNA"
        seqType.name = "RNA${number++}_TAG"
        seqType.tagmentation = true
        return seqType
    }

    fun getMetaDataColumn(): MetaDataColumn {
        return getMetaDataColumn("column $colNumber", "Sample", "name")
    }

    fun getMetaDataColumn(name: String, className: String, propertyName: String): MetaDataColumn {
        val metaDataColumn = MetaDataColumn()
        metaDataColumn.columnName = name
        metaDataColumn.columnOrder = colNumber++
        metaDataColumn.importAliases = name
        metaDataColumn.reflectionClassName = className
        metaDataColumn.reflectionPropertyNameImport = propertyName
        metaDataColumn.reflectionPropertyNameExport = propertyName
        return metaDataColumn
    }

    fun getExampleCsvRows(): Map<String, String> {
        return mapOf(
            "fastq file name" to "testfile_R1.fastq.gz",
            "sample name" to "testsample",
            "index" to "AGCT",
            "antibody target" to "antibodyTarget",
            "base material" to "genomic RNA",
            "run id" to "some_run_id",
            "species" to "some_species",
            "sample type" to "sample_type",
            "sex" to "o"
        )
    }

    fun getExampleMetadataColumns(): List<MetaDataColumn> {
        return listOf(
            getMetaDataColumn("fastq file name", "File", "fileName"),
            getMetaDataColumn("base count", "File", "baseCount"),
            getMetaDataColumn("cycle count", "File", "cycleCount"),
            getMetaDataColumn("sample name", "Sample", "importIdentifier"),
            getMetaDataColumn("species", "Sample", "speciesWithStrain"),
            getMetaDataColumn("sequencing type", "Sample", "seqType"),
            getMetaDataColumn("antibody target", "Sample", "antibodyTarget"),
            getMetaDataColumn("sample type", "Sample", "sampleType"),
            getMetaDataColumn("sex", "Sample", "sex"),
            getMetaDataColumn("index", "TechnicalSample", "barcode"),
            getMetaDataColumn("lane no", "TechnicalSample", "lane"),
            getMetaDataColumn("fastq generator", "TechnicalSample", "pipelineVersion"),
            getMetaDataColumn("read count", "TechnicalSample", "readCount"),
            getMetaDataColumn("run id", "TechnicalSample", "runId")
        )
    }

    fun getExampleRenamedCsvRows(): List<Map<String, String>> {
        val fileMap = mapOf("fastq file name" to "testfile_R1.fastq.gz", "base count" to "1", "cycle count" to "2")
        val sampleNameMap = mapOf("sample name" to "testsample")
        val tSampleMap = mapOf("index" to "AGCT", "lane no" to "1", "fastq generator" to "unknown", "read count" to "2")
        return listOf(fileMap, sampleNameMap, tSampleMap)
    }

    fun getPerson(): Person {
        val person = Person()
        person.username = "username"
        person.lastName = "lastName"
        person.firstName = "firstName"
        person.mail = "mail"
        person.department = "department"
        person.organizationalUnit = "organizationalUnit"
        person.accountDisabled = false
        return person
    }

    fun getGuideUser(): Person {
        val person = Person()
        person.username = "guide"
        person.mail = "guide-mail"
        return person
    }

    fun getValidation(): Validation {
        val validation = Validation()
        validation.required = true
        validation.field = "field"
        validation.regex = "[a-zA-Z0-9_-]*"
        validation.description = "alphanumerical characters, minus/underscore allowed; can be left empty"
        return validation
    }

    fun getValidation(name: String = "field", regex: String): Validation {
        val validation = Validation()
        validation.required = true
        validation.field = name
        validation.regex = regex
        validation.description = "description"
        return validation
    }

    fun getExternalIlseSubmissionImportObjectWithUnknownValue(): ExternalIlseSubmissionImportObject {
        val externalIlseSubmissionImportObject = ExternalIlseSubmissionImportObject()
        externalIlseSubmissionImportObject.samples = listOf(getIlseSampleImportObjectWithUnknownValue())
        return externalIlseSubmissionImportObject
    }

    fun getIlseSampleImportObject(): IlseSampleImportObject {
        val ilseSampleImportObject = IlseSampleImportObject()
        ilseSampleImportObject.submission_id = "0"
        ilseSampleImportObject.submission_type = "multiplex"
        ilseSampleImportObject.submitter = "submitter"
        ilseSampleImportObject.sequencing_type = "NovaSeq 6000 Paired-End 100bp S1"
        ilseSampleImportObject.read_1_length = 51
        ilseSampleImportObject.read_2_length = 0
        ilseSampleImportObject.lanes = 4.0
        ilseSampleImportObject.iag_id = ""
        ilseSampleImportObject.asid = "386920"
        ilseSampleImportObject.sampleName = "sampleName"
        ilseSampleImportObject.pseudonym = "pseudonym"
        ilseSampleImportObject.sex = "f"
        ilseSampleImportObject.tissue = "N"
        ilseSampleImportObject.odcf_project = "odcf_project"
        ilseSampleImportObject.odcf_comment = ""
        ilseSampleImportObject.odcf_custom_name = ""
        ilseSampleImportObject.odcf_single_cell_well_label = "odcf_single_cell_well_label"
        ilseSampleImportObject.type = "type"
        ilseSampleImportObject.isTagmentation = false
        ilseSampleImportObject.antibody_target = ""
        ilseSampleImportObject.species = "mm10"
        ilseSampleImportObject.base_material = "genomic DNA"
        ilseSampleImportObject.libprepKit = "libprepKit"
        ilseSampleImportObject.indexType = "indexType"
        ilseSampleImportObject.protocol = "protocol"
        ilseSampleImportObject.proceed = "-"
        return ilseSampleImportObject
    }

    fun getIlseSampleImportObjectWithUnknownValue(): IlseSampleImportObject {
        val ilseSampleImportObject = getIlseSampleImportObject()
        ilseSampleImportObject.unknown["unknown"] = "123"
        return ilseSampleImportObject
    }

    fun getFile(): File {
        val file = File()
        file.fileName = "fileName_R1.fastq.gz"
        file.readNumber = "readNumber"
        file.md5 = "md5"
        file.baseCount = 0
        file.cycleCount = 0
        file.sample = getSample()
        return file
    }

    fun getFile(sample: Sample): File {
        val file = getFile()
        file.sample = sample
        return file
    }

    fun getRuntimeOption(): RuntimeOptions {
        return getRuntimeOption("value")
    }

    fun getRuntimeOption(value: String): RuntimeOptions {
        return getRuntimeOption("name", value)
    }

    fun getRuntimeOption(name: String, value: String): RuntimeOptions {
        val runtimeOption = RuntimeOptions()
        runtimeOption.name = name
        runtimeOption.value = value
        return runtimeOption
    }

    fun getOtpCachedProject(): OtpCachedProject {
        val otpCachedProject = OtpCachedProject()
        otpCachedProject.latestUpdate = Date()
        otpCachedProject.name = "project${number++}"
        otpCachedProject.unixGroup = "unixGroup"
        otpCachedProject.pis = "pi"
        otpCachedProject.seqTypes = "WGS"
        otpCachedProject.lastDataReceived = Date().toString()
        otpCachedProject.pathProjectFolder = "/path/to/project"
        otpCachedProject.pathAnalysisFolder = "/path/to/analysis"
        return otpCachedProject
    }

    fun getSubmissionImportObject(): SubmissionImportObject {
        val submissionImportObject = SubmissionImportObject()
        submissionImportObject.userName = "username"
        submissionImportObject.userMail = "a.b@c.de"
        submissionImportObject.otrsTicketNumber = "2456434"
        submissionImportObject.sequencingType = "seqType"
        submissionImportObject.libraryLayout = "paired"

        return submissionImportObject
    }

    fun getSampleImportObject(): SampleImportObject {
        val sampleImportObject = SampleImportObject()
        sampleImportObject.pid = "pid"
        sampleImportObject.sex = "UNKNOWN"
        sampleImportObject.sampleType = "sampleType"
        sampleImportObject.sampleIdentifier = "sampleIdentifier"
        sampleImportObject.project = "project"
        sampleImportObject.seqType = "WGS"
        sampleImportObject.libraryLayout = "PAIRED"
        sampleImportObject.isSingleCell = false
        sampleImportObject.isTagmentation = false
        sampleImportObject.antibodyTarget = "antibodyTarget"
        return sampleImportObject
    }

    fun getSampleGuiDto(): SampleGuiDto {
        val sampleGuiDto = SampleGuiDto()
        sampleGuiDto.id = number++
        sampleGuiDto.name = "name"
        sampleGuiDto.project = "project"
        sampleGuiDto.pid = "pid"
        sampleGuiDto.sampleType = "sampleType"
        sampleGuiDto.xenograft = false
        sampleGuiDto.sampleTypeCategory = "TUMOR"
        sampleGuiDto.sex = "MALE"
        sampleGuiDto.phenotype = "phenotype"
        sampleGuiDto.libraryLayout = "single"
        sampleGuiDto.singleCell = false
        sampleGuiDto.seqType = getSeqType()
        sampleGuiDto.tagmentation = false
        sampleGuiDto.tagmentationLibrary = "tagmentationLibrary"
        sampleGuiDto.antibody = "antibody"
        sampleGuiDto.antibodyTarget = "antibodyTarget"
        sampleGuiDto.libraryPreparationKit = "libraryPreparationKit"
        sampleGuiDto.indexType = "indexType"
        sampleGuiDto.singleCellPlate = "singleCellPlate"
        sampleGuiDto.singleCellWellPosition = "singleCellWellPosition"
        sampleGuiDto.comment = "comment"
        return sampleGuiDto
    }

    fun getFileGuiDto(): FileGuiDto {
        val fileGuiDto = FileGuiDto()
        fileGuiDto.id = number++
        fileGuiDto.fileName = "fileName"
        fileGuiDto.readNumber = "readNumber"
        fileGuiDto.md5 = "md5"
        fileGuiDto.baseCount = 0
        fileGuiDto.cycleCount = 0
        return fileGuiDto
    }

    fun getParser(): Parser {
        val parser = Parser()
        parser.project = "project"
        parser.parserRegex = "[fieldName1]_[fieldName2]"
        return parser
    }

    fun getParser(fields: List<ParserField>): Parser {
        val parser = Parser()
        parser.project = "project"
        parser.parserRegex = fields.joinToString(separator = "_") { "[${it.fieldName}]" }
        return parser
    }

    fun getParserField(): ParserField {
        return getParserField(getParser())
    }

    fun getParserField(parser: Parser): ParserField {
        val parserField = ParserField()
        parserField.parser = parser
        parserField.fieldName = "fieldName$number"
        parserField.columnMapping = "fieldName${number++}"
        parserField.parserComponents = listOf(
            getParserComponent(parserField),
            getParserComponent(parserField)
        )
        return parserField
    }

    fun getParserComponent(): ParserComponent {
        return getParserComponent(getParserField())
    }

    fun getParserComponent(parserField: ParserField): ParserComponent {
        return getParserComponent(parserField, 0)
    }

    fun getParserComponent(parserField: ParserField, numberOfDigits: Int): ParserComponent {
        val parserComponent = ParserComponent()
        parserComponent.parserField = parserField
        parserComponent.componentName = "component${number++}"
        parserComponent.componentRegex = "[componentRegex${number++}]"
        parserComponent.numberOfDigits = numberOfDigits
        parserComponent.parserMappingString = "T=Tumor;M=Metastasis;C=Control"
        return parserComponent
    }

    fun getParserForm(): ParserForm {
        val parserForm = ParserForm()
        parserForm.field = listOf(getParserField(), getParserField())
        parserForm.parser = getParser(parserForm.field!!)
        return parserForm
    }

    fun getFeedback(): Feedback {
        return getFeedback("neutral")
    }

    fun getFeedback(rating: String): Feedback {
        val feedback = Feedback()
        feedback.submission = getApiSubmission()
        feedback.setRating(rating)
        return feedback
    }

    fun getSequencingTechnology(): SequencingTechnology {
        val sequencingTechnology = SequencingTechnology()
        sequencingTechnology.name = "sequencingTechnology"
        sequencingTechnology.validationLevel = getValidationLevel()
        sequencingTechnology.clusterJobTemplate = null
        return sequencingTechnology
    }

    fun getSequencingTechnologyWithClusterJobTemplate(): SequencingTechnology {
        val sequencingTechnology = SequencingTechnology()
        sequencingTechnology.name = "sequencingTechnology"
        sequencingTechnology.validationLevel = getValidationLevel()
        sequencingTechnology.clusterJobTemplate = getClusterJobTemplate()
        sequencingTechnology.checkExternalMetadataSource = false
        return sequencingTechnology
    }

    fun getValidationLevel(): ValidationLevel {
        val validationLevel = ValidationLevel()
        validationLevel.name = "name"
        return validationLevel
    }

    fun getValidationLevel(field: String): ValidationLevel {
        val validationLevel = ValidationLevel()
        validationLevel.name = "name"
        val validationField = getValidation()
        validationField.field = field
        validationLevel.fields = setOf(validationField)
        return validationLevel
    }

    fun getValidationLevel(fields: List<String>): ValidationLevel {
        val validationLevel = ValidationLevel()
        validationLevel.name = "name"
        val validationFields = setOf<Validation>()
        fields.forEach {
            validationFields.plus(getValidation(it, "[a-zA-Z0-9_-]*"))
        }
        validationLevel.fields = validationFields
        return validationLevel
    }

    fun getClusterJob(): ClusterJob {
        return getClusterJob(getApiSubmission())
    }

    fun getClusterJob(submission: Submission): ClusterJob {
        val clusterJob = ClusterJob()
        clusterJob.jobName = "name"
        clusterJob.command = "bsub command"
        clusterJob.pathToLog = "pathToLog"
        clusterJob.submission = submission
        return clusterJob
    }

    fun getClusterJobTemplate(): ClusterJobTemplate {
        val clusterJobTemplate = ClusterJobTemplate()
        clusterJobTemplate.name = "name"
        clusterJobTemplate.command = "bsub command"
        return clusterJobTemplate
    }

    fun getRequestedValue(submission: Submission): FieldRequestedValue {
        val requestedValue = FieldRequestedValue()
        requestedValue.fieldName = "pid"
        requestedValue.className = "Sample"
        requestedValue.requestedValue = "requestedValue"
        requestedValue.requester = getPerson()
        requestedValue.originSubmission = submission
        requestedValue.usedSubmissions = setOf(submission).toMutableSet()
        requestedValue.state = RequestedValue.State.REQUESTED
        return requestedValue
    }

    fun getRequestedValue(reqVal: String, fieldName: String, className: String, submission: Submission): FieldRequestedValue {
        val requestedValue = getRequestedValue(submission)
        requestedValue.requestedValue = reqVal
        requestedValue.fieldName = fieldName
        requestedValue.className = className
        return requestedValue
    }

    fun getRequestedValue(): FieldRequestedValue {
        return getRequestedValue(getUploadSubmission())
    }

    fun getRequestedSeqType(submission: Submission, seqType: SeqType): SeqTypeRequestedValue {
        val requestedValue = SeqTypeRequestedValue()
        requestedValue.requestedValue = "requestedSeqType"
        requestedValue.requestedSeqType = seqType
        requestedValue.requester = getPerson()
        requestedValue.originSubmission = submission
        requestedValue.usedSubmissions = setOf(submission).toMutableSet()
        requestedValue.state = RequestedValue.State.REQUESTED
        return requestedValue
    }

    fun getRequestedSeqType(): SeqTypeRequestedValue {
        return getRequestedSeqType(getUploadSubmission(), getSeqType())
    }
}
