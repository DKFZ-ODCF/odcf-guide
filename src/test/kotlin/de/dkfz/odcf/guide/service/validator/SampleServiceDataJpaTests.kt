package de.dkfz.odcf.guide.service.validator

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.TechnicalSample
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.helperObjects.SampleForm
import de.dkfz.odcf.guide.service.implementation.validator.SampleServiceImpl
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.RequestedValueService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.ModificationService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
open class SampleServiceDataJpaTests @Autowired constructor(
    private val entityManager: TestEntityManager
) : AnyObject {

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
    fun `check update samples`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(sample)

        val newSample = entityFactory.getSample(sample.submission)
        newSample.id = sample.id
        newSample.name = "newName"

        var resultSample = Sample()
        `when`(sampleRepository.save(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }

        sampleServiceMock.updateSamples(submission, listOf(newSample))

        assertThat(resultSample.name).isEqualTo(newSample.name)
    }

    @Test
    fun `check update samples with dto`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val form = SampleForm()

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(sample)

        val sampleGuiDto = entityFactory.getSampleGuiDto()
        sampleGuiDto.id = sample.id
        form.sampleList = arrayListOf(sampleGuiDto)

        `when`(sampleRepository.getOne(sampleGuiDto.id)).thenReturn(entityManager.find(Sample::class.java, sample.id))
        var resultSample = Sample()
        `when`(sampleRepository.save(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())

        sampleServiceMock.updateSamples(submission, form)

        assertThat(resultSample.id).isEqualTo(sampleGuiDto.id)
        assertThat(resultSample.name).isEqualTo(sampleGuiDto.name)
        assertThat(resultSample.project).isEqualTo(sampleGuiDto.project)
        assertThat(resultSample.pid).isEqualTo(sampleGuiDto.pid)
        assertThat(resultSample.sampleType).isEqualTo(sampleGuiDto.sampleType.lowercase())
        assertThat(resultSample.xenograft).isEqualTo(sampleGuiDto.xenograft)
        assertThat(resultSample.sampleTypeCategory).isEqualTo(sampleGuiDto.sampleTypeCategory)
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
    fun `check update files and samples`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        val file = entityFactory.getFile(sample)
        sample.files = listOf(file)
        val technicalSample = entityFactory.getTechnicalSample(sample)

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file)

        file.fileName = "newFileName"
        technicalSample.center = "newCenter"

        var resultSample = Sample()
        `when`(sampleRepository.save(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }
        var resultTechnicalSample = TechnicalSample()
        `when`(technicalSampleRepository.save(anyTechnicalSample())).then { i -> (i.arguments[0] as TechnicalSample).also { resultTechnicalSample = it } }
        var resultFiles = emptyList<File>()
        `when`(fileRepository.saveAll(anyList())).then { i -> (i.arguments[0] as List<File>).also { resultFiles = it } }

        assertThat(submission.startTerminationPeriod).isNull()

        sampleServiceMock.updateFilesAndSamples(submission, listOf(sample))

        assertThat(resultSample.name).isEqualTo("${sample.pid}_${sample.sampleType}")
        assertThat(resultTechnicalSample.center).isEqualTo("newCenter")
        assertThat(resultFiles.first().fileName).isEqualTo("newFileName")
        assertThat(submission.startTerminationPeriod).isNotNull
    }

    @Test
    fun `check update files and samples with form`() {
        val sample = entityFactory.getSample()
        val file = entityFactory.getFile(sample)
        sample.files = listOf(file)
        val technicalSample = entityFactory.getTechnicalSample(sample)
        val submission = sample.submission

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file)

        val sampleGuiDto = entityFactory.getSampleGuiDto()
        sampleGuiDto.id = sample.id
        sampleGuiDto.technicalSample = technicalSample
        val fileGuiDto = entityFactory.getFileGuiDto()
        fileGuiDto.id = file.id
        sampleGuiDto.files = listOf(fileGuiDto)
        val form = SampleForm()
        form.sampleList = arrayListOf(sampleGuiDto)

        `when`(sampleRepository.getOne(sampleGuiDto.id)).thenReturn(entityManager.find(Sample::class.java, sample.id))
        `when`(fileService.convertToEntity(fileGuiDto, sample)).thenReturn(entityManager.find(File::class.java, file.id))
        var resultSample = Sample()
        `when`(sampleRepository.save(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }
        var resultTechnicalSample = TechnicalSample()
        `when`(technicalSampleRepository.save(anyTechnicalSample())).then { i -> (i.arguments[0] as TechnicalSample).also { resultTechnicalSample = it } }
        var resultFiles = emptyList<File>()
        `when`(fileRepository.saveAll(anyList())).then { i -> (i.arguments[0] as List<File>).also { resultFiles = it } }
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())

        sampleServiceMock.updateSamples(submission, form)

        assertThat(resultSample.name).isEqualTo("${sample.pid}_${sample.sampleType}")
        assertThat(resultFiles.first().fileName).isEqualTo(file.fileName)
        assertThat(resultFiles.first().readNumber).isEqualTo(file.readNumber)
        assertThat(resultFiles.first().md5).isEqualTo(file.md5)
        assertThat(resultFiles.first().baseCount).isEqualTo(file.baseCount)
        assertThat(resultFiles.first().cycleCount).isEqualTo(file.cycleCount)
        assertThat(resultTechnicalSample.center).isEqualTo("center")
        assertThat(resultTechnicalSample.externalSubmissionId).isEqualTo("ilseNo")
        assertThat(resultTechnicalSample.barcode).isEqualTo("barcode")
        assertThat(resultTechnicalSample.instrumentModel).isEqualTo("instrModel")
        assertThat(resultTechnicalSample.instrumentPlatform).isEqualTo("instrPlatform")
        assertThat(resultTechnicalSample.lane).isEqualTo(1)
        assertThat(resultTechnicalSample.pipelineVersion).isEqualTo("pipeline")
        assertThat(resultTechnicalSample.readCount).isEqualTo(1)
        assertThat(resultTechnicalSample.runDate).isEqualTo("2000-01-01")
        assertThat(resultTechnicalSample.runId).isEqualTo("runId")
        assertThat(resultTechnicalSample.sequencingKit).isEqualTo("sequencingKit")
    }
}
