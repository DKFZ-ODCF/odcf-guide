package de.dkfz.odcf.guide.service.validator

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.validator.SampleServiceImpl
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.RequestedValueService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.ModificationService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class SampleServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var sampleServiceMock: SampleServiceImpl

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var technicalSampleRepository: TechnicalSampleRepository

    @Mock
    lateinit var validationRepository: ValidationRepository

    @Mock
    lateinit var fileRepository: FileRepository

    @Mock
    lateinit var seqTypeRepository: SeqTypeRepository

    @Mock
    lateinit var seqTypeRequestedValuesRepository: SeqTypeRequestedValuesRepository

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var modificationService: ModificationService

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var fileService: FileService

    @Mock
    lateinit var submissionService: SubmissionService

    @Mock
    lateinit var requestedValueService: RequestedValueService

    @Mock
    lateinit var ldapService: LdapService

    @Test
    fun `validate sample without errors`() {
        val sample = entityFactory.getSampleGuiDto()
        val validationLevel = entityFactory.getValidationLevel(
            listOf(
                "pid",
                "sampleType",
                "project",
                "sex",
                "seqType",
                "tagmentationLibrary",
                "singleCellPlate",
                "singleCellWellPosition",
                "readCount",
                "barcode",
                "externalSubmissionId",
                "lane",
                "comment",
            )
        )

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate sample with illegal chars`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.pid += "$$$"
        val validationLevel = entityFactory.getValidationLevel("pid")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isNotEmpty
        assertThat(errors).isEqualTo(mapOf("pid" to true))
    }

    @Test
    fun `validate sample with blank field`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.pid = ""
        val validationLevel = entityFactory.getValidationLevel("pid")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isNotEmpty
        assertThat(errors).isEqualTo(mapOf("pid" to true))
    }

    @Test
    fun `validate sample with sample type with no number at the end, but should have`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.sampleType = "sampleType"
        val validationLevel = entityFactory.getValidationLevel("sampleType")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())
        `when`(validationRepository.findByField("sampleType")).thenReturn(entityFactory.getValidation(regex = "^[a-z0-9+-]*[0-9]+$"))
        `when`(validationRepository.findByField("oldSampleType")).thenReturn(entityFactory.getValidation(regex = "^[a-z0-9+-]+$"))

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isNotEmpty
        assertThat(errors).isEqualTo(mapOf("sampleType" to true))
    }

    @Test
    fun `validate sample with sample type with no number at the end and can have`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.sampleType = "sample-type"
        val validationLevel = entityFactory.getValidationLevel("sampleType")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType", "sampleType"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())
        `when`(validationRepository.findByField("sampleType")).thenReturn(entityFactory.getValidation(regex = "^[a-z0-9+-]*[0-9]+$"))

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate sample with empty seqType`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.seqType = null
        val validationLevel = entityFactory.getValidationLevel("seqType")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isNotEmpty
        assertThat(errors).isEqualTo(mapOf("seqType" to true))
    }

    @Test
    fun `validate sample with single cell seq type`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.seqType = entityFactory.getSingleCellSeqType()
        val validationLevel = entityFactory.getValidationLevel("seqType")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate sample with tagmentation seq type`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.seqType = entityFactory.getTagmentationCellSeqType()
        val validationLevel = entityFactory.getValidationLevel("seqType")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate samples without errors`() {
        val sample = entityFactory.getSampleGuiDto()
        val sample1 = entityFactory.getSampleGuiDto()
        val validationLevel = entityFactory.getValidationLevel()

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSamples(listOf(sample, sample1), validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate samples with error`() {
        val sample = entityFactory.getSampleGuiDto()
        val sample1 = entityFactory.getSampleGuiDto()
        sample.pid += "$$$"
        val validationLevel = entityFactory.getValidationLevel("pid")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSamples(listOf(sample, sample1), validationLevel)

        assertThat(errors).isNotEmpty
        assertThat(errors[sample.id]).isEqualTo(mapOf("pid" to true))
    }

    @Test
    fun `validate invalid non-proceed sample without error`() {
        val sample = entityFactory.getSampleGuiDto()
        val sample1 = entityFactory.getSampleGuiDto()
        sample.pid += "$$$"
        sample.id = 0 // samples with proceed=false have id=0
        val validationLevel = entityFactory.getValidationLevel("pid")

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSamples(listOf(sample, sample1), validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate technical sample without errors`() {
        val sample = entityFactory.getSampleGuiDto()
        sample.technicalSample = entityFactory.getTechnicalSample()
        val validationLevel = entityFactory.getValidationLevel(listOf("readCount", "barcode", "externalSubmissionId", "lane"))

        `when`(externalMetadataSourceService.getValuesAsSet(matches("sampleTypesByProject"), anyMap())).thenReturn(setOf("sampleType01", "sampleType02"))
        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation())

        val errors = sampleServiceMock.validateSample(sample, validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate file without errors`() {
        val file = entityFactory.getFileGuiDto()
        val validationLevel = entityFactory.getValidationLevel(listOf("fileName", "md5", "baseCount", "cycleCount"))

        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation(regex = "[a-zA-Z0-9._-]*"))

        val errors = sampleServiceMock.validateFile(file, validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate files without errors`() {
        val file1 = entityFactory.getFileGuiDto()
        val file2 = entityFactory.getFileGuiDto()
        val validationLevel = entityFactory.getValidationLevel(listOf("fileName", "md5", "baseCount", "cycleCount"))

        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation(regex = "[a-zA-Z0-9._-]*"))

        val errors = sampleServiceMock.validateFiles(listOf(file1, file2), validationLevel)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `validate files with error`() {
        val file1 = entityFactory.getFileGuiDto()
        val file2 = entityFactory.getFileGuiDto()
        file1.fileName += "$$$"
        val validationLevel = entityFactory.getValidationLevel("fileName")

        `when`(validationRepository.findByField(ArgumentMatchers.anyString())).thenReturn(entityFactory.getValidation(regex = "[a-zA-Z0-9._-]*"))

        val errors = sampleServiceMock.validateFiles(listOf(file1, file2), validationLevel)

        assertThat(errors).isNotEmpty
        assertThat(errors[file1.id]).isEqualTo(mapOf("fileName" to true))
    }

    @Test
    fun `convert sample gui dto to sample with existing sample`() {
        val sampleGuiDto = entityFactory.getSampleGuiDto()
        val sample = entityFactory.getSample()

        `when`(sampleRepository.getOne(sampleGuiDto.id)).thenReturn(sample)

        val resultSample = sampleServiceMock.convertToEntity(sampleGuiDto)

        assertThat(resultSample.id).isEqualTo(sampleGuiDto.id)
        assertThat(resultSample.name).isEqualTo(sampleGuiDto.name)
        assertThat(resultSample.project).isEqualTo(sampleGuiDto.project)
        assertThat(resultSample.pid).isEqualTo(sampleGuiDto.pid)
        assertThat(resultSample.sampleType).isEqualTo(sampleGuiDto.sampleType.lowercase())
        assertThat(resultSample.xenograft).isEqualTo(sampleGuiDto.xenograft)
        assertThat(resultSample.sex.toString().lowercase()).isEqualTo(sampleGuiDto.sex.lowercase())
        assertThat(resultSample.phenotype).isEqualTo(sampleGuiDto.phenotype)
        assertThat(resultSample.libraryLayout.toString().lowercase()).isEqualTo(sampleGuiDto.libraryLayout.lowercase())
        assertThat(resultSample.singleCell).isEqualTo(sampleGuiDto.singleCell)
        assertThat(resultSample.seqType).isEqualTo(sampleGuiDto.seqType)
        assertThat(resultSample.tagmentationLibrary).isEqualTo(sampleGuiDto.tagmentationLibrary)
        assertThat(resultSample.antibody).isEqualTo(sampleGuiDto.antibody)
        assertThat(resultSample.antibodyTarget).isEqualTo(sampleGuiDto.antibodyTarget)
        assertThat(resultSample.libraryPreparationKit).isEqualTo(sampleGuiDto.libraryPreparationKit)
        assertThat(resultSample.indexType).isEqualTo(sampleGuiDto.indexType)
        assertThat(resultSample.singleCellPlate).isEqualTo(sampleGuiDto.singleCellPlate)
        assertThat(resultSample.singleCellWellPosition).isEqualTo(sampleGuiDto.singleCellWellPosition)
        assertThat(resultSample.comment).isEqualTo(sampleGuiDto.comment)

        assertThat(resultSample.abstractSampleId).isEqualTo(sample.abstractSampleId)
        assertThat(resultSample.indexType).isEqualTo(sample.indexType)
        assertThat(resultSample.read1Length).isEqualTo(sample.read1Length)
        assertThat(resultSample.read2Length).isEqualTo(sample.read2Length)
        assertThat(resultSample.baseMaterial).isEqualTo(sample.baseMaterial)
        assertThat(resultSample.requestedLanes).isEqualTo(sample.requestedLanes)
        assertThat(resultSample.requestedSequencingInfo).isEqualTo(sample.requestedSequencingInfo)
    }

    @Test
    fun `convert sample gui dto to sample with new Sample`() {
        val sampleGuiDto = entityFactory.getSampleGuiDto()
        sampleGuiDto.id = 0

        val resultSample = sampleServiceMock.convertToEntity(sampleGuiDto)

        assertThat(resultSample.id).isEqualTo(sampleGuiDto.id)
        assertThat(resultSample.name).isEqualTo(sampleGuiDto.name)
        assertThat(resultSample.project).isEqualTo(sampleGuiDto.project)
        assertThat(resultSample.pid).isEqualTo(sampleGuiDto.pid)
        assertThat(resultSample.sampleType).isEqualTo(sampleGuiDto.sampleType.lowercase())
        assertThat(resultSample.xenograft).isEqualTo(sampleGuiDto.xenograft)
        assertThat(resultSample.sex.toString().lowercase()).isEqualTo(sampleGuiDto.sex.lowercase())
        assertThat(resultSample.phenotype).isEqualTo(sampleGuiDto.phenotype)
        assertThat(resultSample.libraryLayout.toString().lowercase()).isEqualTo(sampleGuiDto.libraryLayout.lowercase())
        assertThat(resultSample.singleCell).isEqualTo(sampleGuiDto.singleCell)
        assertThat(resultSample.seqType).isEqualTo(sampleGuiDto.seqType)
        assertThat(resultSample.tagmentationLibrary).isEqualTo(sampleGuiDto.tagmentationLibrary)
        assertThat(resultSample.antibody).isEqualTo(sampleGuiDto.antibody)
        assertThat(resultSample.antibodyTarget).isEqualTo(sampleGuiDto.antibodyTarget)
        assertThat(resultSample.libraryPreparationKit).isEqualTo(sampleGuiDto.libraryPreparationKit)
        assertThat(resultSample.singleCellPlate).isEqualTo(sampleGuiDto.singleCellPlate)
        assertThat(resultSample.singleCellWellPosition).isEqualTo(sampleGuiDto.singleCellWellPosition)
        assertThat(resultSample.comment).isEqualTo(sampleGuiDto.comment)
    }

    @Test
    fun `Check handleRequestedValues functionality`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        sample.antibodyTarget = "abt(ReqVal)"
        sample.libraryPreparationKit = "libPrepKit(ReqVal)"
        sample.speciesWithStrain = "Human (Homo sapiens)[No strain available]+NewSpecies(ReqVal)"
        val technicalSample = entityFactory.getTechnicalSample(sample)
        technicalSample.center = "center(ReqVal)"
        technicalSample.instrumentModel = "instrumentModel"
        val person = entityFactory.getPerson()
        val properties = setOf(sample::antibodyTarget, sample::libraryPreparationKit, sample::speciesWithStrain, technicalSample::center, technicalSample::instrumentModelWithSequencingKit)

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        sampleServiceMock.handleRequestedValues(properties, submission)

        verify(requestedValueService, times(1)).saveRequestedValue("antibodyTarget", "Sample", "abt", submission)
        verify(requestedValueService, times(1)).saveRequestedValue("libraryPreparationKit", "Sample", "libPrepKit", submission)
        verify(requestedValueService, times(1)).saveRequestedValue("speciesWithStrain", "Sample", "NewSpecies", submission)
        verify(requestedValueService, times(0)).saveRequestedValue("speciesWithStrain", "Sample", "Human (Homo sapiens)[No strain available]", submission)
        verify(requestedValueService, times(1)).saveRequestedValue("center", "TechnicalSample", "center", submission)
        verify(requestedValueService, times(0)).saveRequestedValue("instrumentModelWithSequencingKit", "TechnicalSample", "instrumentModelWithSequencingKit", submission)
    }

    @TestFactory
    fun `Check handleRequestedSeqTypes functionality`() = listOf(
        true to 1,
        false to 0
    ).map { (isRequested, expected) ->
        DynamicTest.dynamicTest("handleRequestedSeqTypes requested seqType $isRequested") {
            val submission = entityFactory.getUploadSubmission()
            val sample = entityFactory.getSample(submission)
            val seqType = entityFactory.getSeqType()
            seqType.isRequested = isRequested
            sample.seqType = seqType
            val person = entityFactory.getPerson()

            `when`(ldapService.getPerson()).thenReturn(person)
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

            sampleServiceMock.handleRequestedSeqTypes(sample.seqType, submission)

            verify(requestedValueService, times(expected)).saveSeqTypeRequestedValue(seqType, submission)
        }
    }

    @Test
    fun `check handleRequestedSeqTypes when sample has no seqType`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        sample.seqType = null
        val person = entityFactory.getPerson()

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        sampleServiceMock.handleRequestedSeqTypes(sample.seqType, submission)

        verifyNoInteractions(requestedValueService)
    }

    @Test
    fun `check deleteNotNeededSeqTypeRequests functionality`() {
        val seqType = entityFactory.getSeqType()
        seqType.isRequested = true
        val seqTypeRequestedValue = entityFactory.getRequestedSeqType()
        val requestedSeqType = seqTypeRequestedValue.requestedSeqType

        `when`(seqTypeRepository.findAllByIsRequestedIsTrue()).thenReturn(setOf(seqType, requestedSeqType!!))
        `when`(seqTypeRequestedValuesRepository.findAllByRequestedSeqType_IsRequestedIsTrue()).thenReturn(setOf(seqTypeRequestedValue))

        sampleServiceMock.deleteNotNeededSeqTypeRequests()

        verify(seqTypeRepository, times(1)).deleteAll(setOf(seqType))
    }

    @Test
    fun `check deletedFilesAndSamples functionality`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        sample.deletionFlag = false
        val file = entityFactory.getFile(sample)
        file.deletionFlag = false
        val fileToDelete = entityFactory.getFile(sample)
        val sampleToDelete = entityFactory.getSample(submission)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample, sampleToDelete))
        `when`(fileRepository.findAllBySampleIn(listOf(sample, sampleToDelete))).thenReturn(listOf(file, fileToDelete))

        sampleServiceMock.deletedFilesAndSamples(submission)

        verify(fileRepository, times(1)).deleteAll(listOf(fileToDelete))
        verify(sampleRepository, times(1)).deleteAll(listOf(sampleToDelete))
    }

    @Test
    fun `check cloneSample functionality`() {
        val sample = entityFactory.getSample()
        sample.unknownValues = mapOf("unknown" to "sample unknown")

        val newSample = sampleServiceMock.cloneSample(sample)

        assertThat(sample == newSample).isTrue
        assertThat(sample === newSample).isFalse
        assertThat(sample.libraryLayout).isEqualTo(newSample.libraryLayout).isEqualTo(Sample.LibraryLayout.PAIRED)
        assertThat(sample.unknownValues).isEqualTo(newSample.unknownValues)
        assertThat(sample.unknownValues === newSample.unknownValues).isFalse()
        verify(sampleRepository, times(1)).save(newSample)
    }

    @Test
    fun `check cloneSample functionality with technicalSample`() {
        val sample = entityFactory.getSample()
        val technicalSample = entityFactory.getTechnicalSample(sample)

        val newSample = sampleServiceMock.cloneSample(sample)

        assertThat(sample == newSample).isTrue
        assertThat(sample === newSample).isFalse
        assertThat(sample.libraryLayout).isEqualTo(newSample.libraryLayout).isEqualTo(Sample.LibraryLayout.PAIRED)
        assertThat(technicalSample == newSample.technicalSample).isTrue
        assertThat(technicalSample === newSample.technicalSample).isFalse
        verify(sampleRepository, times(1)).save(newSample)
        verify(technicalSampleRepository, times(1)).save(newSample.technicalSample!!)
    }

    @Test
    fun `test getSimilarPids`() {
        val pid = "pidtest"
        val project = "projecttest"
        val map = mapOf(
            "project" to project,
            "pid" to pid,
            "threshold" to "0.3",
            "limit" to "10",
        )

        `when`(externalMetadataSourceService.getValuesAsSetMap("similar-pids", map)).thenReturn(setOf(mapOf("a" to "1", "b" to "2")))

        val result = sampleServiceMock.getSimilarPids(pid, project)

        assertThat(result).isEqualTo(setOf(mapOf("a" to "1", "b" to "2")))
    }

    @Test
    fun `test getSimilarPids empty result`() {
        val pid = "pidtest"
        val project = "projecttest"
        val map = mapOf(
            "project" to project,
            "pid" to pid,
            "threshold" to "0.3",
            "limit" to "10",
        )

        `when`(externalMetadataSourceService.getValuesAsSetMap("similar-pids", map)).thenReturn(emptySet())

        val result = sampleServiceMock.getSimilarPids(pid, project)

        assertThat(result).hasSize(0)
    }

    @Test
    fun `test checkIfSamePidIsAvailable - similar PID is not available`() {
        val pid = "pid1"
        val project = "project1"
        val map = mapOf(
            "project" to project,
            "pid" to pid,
            "threshold" to "0.3",
            "limit" to "10",
        )

        `when`(externalMetadataSourceService.getValuesAsSetMap("similar-pids", map)).thenReturn(setOf(mapOf("similarity_num" to "0.7", "pid" to "%pid1%")))

        val result = sampleServiceMock.checkIfSamePidIsAvailable(pid, project)

        assertThat(result).isNull()
    }

    @Test
    fun `test checkIfSamePidIsAvailable - similar PID is available`() {
        val pid = "pid1"
        val project = "project1"
        val map = mapOf(
            "project" to project,
            "pid" to pid,
            "threshold" to "0.3",
            "limit" to "10",
        )

        `when`(externalMetadataSourceService.getValuesAsSetMap("similar-pids", map)).thenReturn(setOf(mapOf("similarity_num" to "1", "pid" to "PID12")))

        val result = sampleServiceMock.checkIfSamePidIsAvailable(pid, project)

        assertThat(result).isEqualTo(Pair("warning", "PID12"))
    }

    @Test
    fun `test checkIfSamePidIsAvailable - nearly same PID is available`() {
        val pid = "pid1"
        val project = "project1"
        val map = mapOf(
            "project" to project,
            "pid" to pid,
            "threshold" to "0.3",
            "limit" to "10",
        )

        `when`(externalMetadataSourceService.getValuesAsSetMap("similar-pids", map)).thenReturn(setOf(mapOf("similarity_num" to "1", "pid" to "PID1")))

        val result = sampleServiceMock.checkIfSamePidIsAvailable(pid, project)

        assertThat(result).isEqualTo(Pair("danger", "PID1"))
    }
}
