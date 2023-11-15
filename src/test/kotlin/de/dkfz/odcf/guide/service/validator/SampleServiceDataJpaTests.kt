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
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyList
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
        assertThat(resultSample.sampleTypeCategory.toString()).isEqualTo(sampleGuiDto.sampleTypeCategory)
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
        sample.deletionFlag = false
        val file = entityFactory.getFile(sample)
        file.deletionFlag = false
        val technicalSample = entityFactory.getTechnicalSample(sample)

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file)

        file.fileName = "newFileName"
        technicalSample.center = "newCenter"

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        var resultSample = Sample()
        `when`(sampleRepository.save(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }
        var resultTechnicalSample = TechnicalSample()
        `when`(technicalSampleRepository.save(anyTechnicalSample())).then { i -> (i.arguments[0] as TechnicalSample).also { resultTechnicalSample = it } }
        var resultFiles = emptyList<File>()
        `when`(fileRepository.saveAll(anyList())).then { i -> (i.arguments[0] as List<File>).also { resultFiles = it } }

        assertThat(submission.startTerminationPeriod).isNull()

        sampleServiceMock.updateFilesAndSamples(submission, listOf(sample), listOf(file))

        assertThat(resultSample.name).isEqualTo("${sample.pid}_${sample.sampleType}")
        assertThat(resultTechnicalSample.center).isEqualTo("newCenter")
        assertThat(resultFiles.first().fileName).isEqualTo("newFileName")
        assertThat(submission.startTerminationPeriod).isNotNull
    }

    @Test
    fun `check update files and samples with form`() {
        val sample = entityFactory.getSample()
        val file = entityFactory.getFile(sample)
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

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
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

    @Test
    fun `check mergeFastqFilePairs functionality`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val sample2 = entityFactory.getSample(submission)
        sample2.seqType = sample.seqType
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample2)

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(sample2)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        val captor = ArgumentCaptor.forClass(Sample::class.java)
        `when`(sampleRepository.delete(captor.capture())).then { entityManager.remove(sample2) }

        sampleServiceMock.mergeFastqFilePairs(listOf(file1, file2), submission)

        // Verify that sample2 is deleted
        val deletedSample2 = entityManager.find(Sample::class.java, sample2.id)
        assertThat(deletedSample2).isNull()

        // Verify that sample2 was the one to be removed
        val capturedSample = captor.value
        assertThat(capturedSample).isEqualTo(sample2)
    }

    @Test
    fun `check mergeFastqFilePairs functionality do not merge samples that are not the same`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val sample2 = entityFactory.getSample(submission)
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample2)

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(sample2.seqType)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(sample2)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))

        sampleServiceMock.mergeFastqFilePairs(listOf(file1, file2), submission)

        verify(sampleRepository, times(0)).delete(anySample())
    }

    @Test
    fun `check mergeFastqFilePairs functionality do not merge samples if not fastq file pairs`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val sample2 = entityFactory.getSample(submission)
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample2)

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(sample2.seqType)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(sample2)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))

        sampleServiceMock.mergeFastqFilePairs(listOf(file1, file2), submission)

        verify(sampleRepository, times(0)).delete(anySample())
    }

    @Test
    fun `check mergeFastqFilePairs functionality split up samples if files are not fastq file pairs`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val technicalSample = entityFactory.getTechnicalSample(sample)
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample)
        file2.fileName = "someOtherFilename.fastq.gz"
        sample.technicalSample = technicalSample

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(fileRepository.findAllBySample(sample)).thenReturn(listOf(file1, file2))
        var resultSample = Sample()
        `when`(sampleRepository.save(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }
        var resultTechnicalSample = TechnicalSample()
        `when`(technicalSampleRepository.save(anyTechnicalSample())).then { i -> (i.arguments[0] as TechnicalSample).also { resultTechnicalSample = it } }

        sampleServiceMock.mergeFastqFilePairs(listOf(file1, file2), submission)

        assertThat(file1.sample === file2.sample).isFalse
        assertThat(resultSample).isNotNull
        assertThat(sample == resultSample).isTrue
        assertThat(resultSample.technicalSample).isNotNull
        assertThat(file2.sample.technicalSample == resultTechnicalSample).isTrue
        assertThat(technicalSample == resultTechnicalSample).isTrue
    }

    @Test
    fun `check mergeFastqFilePairs functionality split up samples if files are not fastq file pairs version 2`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val technicalSample = entityFactory.getTechnicalSample(sample)
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample)
        file2.fileName = "someOtherFilename.fastq.gz"
        sample.technicalSample = technicalSample

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))

        // needed because the bidirectionality of samples.files got changed to fileRepository.findAllBySample()
        var findAllBySampleCallCount = 0
        `when`(fileRepository.findAllBySample(anySample())).thenAnswer { invocation ->
            val sampleArg = invocation.getArgument<Sample>(0)
            if (sampleArg == sample && findAllBySampleCallCount++ == 0) {
                // On the first call, return both file1 and file2
                listOf(file1, file2)
            } else {
                // Afterward, the sample of file2 has been changed and therefore fileRepository.findAllBySample(file2.sample) should only return file2
                listOf(file2)
            }
        }

        var resultSample = Sample()
        `when`(sampleRepository.save(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }
        var resultTechnicalSample = TechnicalSample()
        `when`(technicalSampleRepository.save(anyTechnicalSample())).then { i -> (i.arguments[0] as TechnicalSample).also { resultTechnicalSample = it } }

        sampleServiceMock.mergeFastqFilePairs(listOf(file1, file2), submission)

        assertThat(file1.sample === sample).isTrue
        assertThat(file2.sample === resultSample).isTrue
        assertThat(file1.sample === file2.sample).isFalse
        assertThat(sample == resultSample).isTrue
        assertThat(file1.sample.technicalSample?.id).isEqualTo(technicalSample.id)
        assertThat(resultSample.technicalSample).isNotNull
        assertThat(file2.sample.technicalSample == resultTechnicalSample).isTrue
        assertThat(technicalSample == resultTechnicalSample).isTrue
    }
}
