package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.FileRepository
import de.dkfz.odcf.guide.MetaDataColumnRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.exceptions.MissingRuntimeOptionException
import de.dkfz.odcf.guide.exceptions.RowNotFoundException
import de.dkfz.odcf.guide.exceptions.SampleNamesDontMatchException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.FileServiceImpl
import de.dkfz.odcf.guide.service.implementation.SeqTypeMappingServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@SpringBootTest
class FileServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var fileServiceMock: FileServiceImpl

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var fileRepository: FileRepository

    @Mock
    lateinit var metaDataColumnRepository: MetaDataColumnRepository

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var seqTypeMappingService: SeqTypeMappingServiceImpl

    @Mock
    lateinit var env: Environment

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var remoteCommandsService: RemoteCommandsService

    @Test
    fun `test generated tsv header`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        val tsvContent = fileServiceMock.createTsvFile(submission)

        val headerRow = tsvContent.split("\n")[0]
        assertThat(headerRow).isEqualTo(
            "sample_name\t" +
                "parse_identifier\t" +
                "project\t" +
                "pid\t" +
                "sample_type\t" +
                "xenograft\t" +
                "sample_type_category\t" +
                "species_with_strain\t" +
                "sex\t" +
                "phenotype\t" +
                "sequencing_read_type\t" +
                "sequencing_type\t" +
                "low_coverage_requested\t" +
                "tagmentation_library\t" +
                "antibody_target\t" +
                "antibody\t" +
                "plate\t" +
                "well_position\t" +
                "library_preparation_kit\t" +
                "index_type\t" +
                "comment"
        )
    }

    @Test
    fun `test generated MetaDataTemplate`() {
        val column1 = entityFactory.getMetaDataColumn()
        val column2 = entityFactory.getMetaDataColumn()
        val column3 = entityFactory.getMetaDataColumn()
        column1.columnOrder = 3
        column2.columnOrder = 2
        column3.columnOrder = 1

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column1, column2, column3))

        val metaDataTemplate = fileServiceMock.createMetadataTemplate()

        assertThat(metaDataTemplate).isEqualTo("column 3\tcolumn 2\tcolumn 1")
    }

    @Test
    fun `test generated tsv`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        val tsvContent = fileServiceMock.createTsvFile(submission)

        assertThat(tsvContent.split("\n").size).isEqualTo(2)
        val secondRow = tsvContent.split("\n")[1].split("\t")
        var i = 0
        assertThat(secondRow[i++]).isEqualTo(sample.name)
        assertThat(secondRow[i++]).isEqualTo(sample.parseIdentifier)
        assertThat(secondRow[i++]).isEqualTo(sample.project)
        assertThat(secondRow[i++]).isEqualTo(sample.pid)
        assertThat(secondRow[i++]).isEqualTo(sample.sampleType)
        assertThat(secondRow[i++]).isEqualTo(sample.xenograft.toString())
        assertThat(secondRow[i++]).isEqualTo(sample.sampleTypeCategory?.toString().orEmpty())
        assertThat(secondRow[i++]).isEqualTo(sample.speciesWithStrain)
        assertThat(secondRow[i++]).isEqualTo(sample.sex.toString())
        assertThat(secondRow[i++]).isEqualTo(sample.phenotype)
        assertThat(secondRow[i++]).isEqualTo(sample.libraryLayout.toString())
        assertThat(secondRow[i++]).isEqualTo(sample.seqType?.name!!)
        assertThat(secondRow[i++]).isEqualTo(sample.lowCoverageRequested.toString())
        assertThat(secondRow[i++]).isEqualTo(sample.tagmentationLibrary)
        assertThat(secondRow[i++]).isEqualTo(sample.antibodyTarget)
        assertThat(secondRow[i++]).isEqualTo(sample.antibody)
        assertThat(secondRow[i++]).isEqualTo(sample.singleCellPlate)
        assertThat(secondRow[i++]).isEqualTo(sample.singleCellWellPosition)
        assertThat(secondRow[i++]).isEqualTo(sample.libraryPreparationKit)
        assertThat(secondRow[i++]).isEqualTo(sample.indexType)
        assertThat(secondRow[i]).isEqualTo(sample.comment)
    }

    @Test
    fun `test create long tsv`() {
        val file = entityFactory.getFile()
        val sample = file.sample
        val technicalSample = entityFactory.getTechnicalSample(sample)
        sample.technicalSample = entityFactory.getTechnicalSample(sample)
        val submission = sample.submission
        submission.identifier = "o0000001"
        submission.submitter = entityFactory.getPerson()

        val col1 = entityFactory.getMetaDataColumn()
        val col2 = entityFactory.getMetaDataColumn("col2", "TechnicalSample", "center")
        val col3 = entityFactory.getMetaDataColumn("col3", "File", "fileName")
        val col4 = entityFactory.getMetaDataColumn("col4", "Submission", "identifier")

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(col1, col2, col3, col4))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(fileRepository.findAllBySample(sample)).thenReturn(listOf(file))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("target/TSV_BASEPATH/SUBFOLDER/"))
        `when`(runtimeOptionsRepository.findByName("tsvExternalSubpath")).thenReturn(entityFactory.getRuntimeOption("TSV_EXTERNAL"))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption("TSV_INTERNAL"))

        val tsvContent = fileServiceMock.createLongTsvFile(submission)

        assertThat(tsvContent.trim().split("\n").size).isEqualTo(2)
        val dataRow = tsvContent.split("\n")[1].split("\t")
        assertThat(dataRow[0]).isEqualTo(sample.name)
        assertThat(dataRow[1]).isEqualTo(technicalSample.center)
        assertThat(dataRow[2]).isEqualTo(file.fileName)
        assertThat(dataRow[3]).isEqualTo(submission.identifier)
    }

    @Test
    fun `test create long tsv with additional metadata`() {
        val file = entityFactory.getFile()
        val sample = file.sample
        sample.unknownValues = mapOf("a1" to "b1", "a2" to "b2")
        val technicalSample = entityFactory.getTechnicalSample(sample)
        sample.technicalSample = entityFactory.getTechnicalSample(sample)
        val submission = sample.submission
        submission.identifier = "o0000001"
        submission.submitter = entityFactory.getPerson()

        val col1 = entityFactory.getMetaDataColumn()
        val col2 = entityFactory.getMetaDataColumn("col2", "TechnicalSample", "center")
        val col3 = entityFactory.getMetaDataColumn("col3", "File", "fileName")
        val col4 = entityFactory.getMetaDataColumn("col4", "Submission", "identifier")

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(col1, col2, col3, col4))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(fileRepository.findAllBySample(sample)).thenReturn(listOf(file))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("target/TSV_BASEPATH/SUBFOLDER/"))
        `when`(runtimeOptionsRepository.findByName("tsvExternalSubpath")).thenReturn(entityFactory.getRuntimeOption("TSV_EXTERNAL"))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption("TSV_INTERNAL"))
        `when`(runtimeOptionsRepository.findByName("unknownValuesAllowedOrganizationalUnits")).thenReturn(entityFactory.getRuntimeOption(submission.submitter.organizationalUnit))

        val tsvContent = fileServiceMock.createLongTsvFile(submission)

        assertThat(tsvContent.trim().split("\n").size).isEqualTo(2)
        val headerRow = tsvContent.split("\n")[0].split("\t")
        assertThat(headerRow).contains("a1", "a2")
        val dataRow = tsvContent.split("\n")[1].split("\t")
        assertThat(dataRow[0]).isEqualTo(sample.name)
        assertThat(dataRow[1]).isEqualTo(technicalSample.center)
        assertThat(dataRow[2]).isEqualTo(file.fileName)
        assertThat(dataRow[3]).isEqualTo(submission.identifier)
        assertThat(dataRow).contains("b1", "b2")
    }

    @Test
    fun `test create long tsv with additional metadata but wrong OE`() {
        val file = entityFactory.getFile()
        val sample = file.sample
        sample.unknownValues = mapOf("a1" to "b1", "a2" to "b2")
        val technicalSample = entityFactory.getTechnicalSample(sample)
        sample.technicalSample = entityFactory.getTechnicalSample(sample)
        val submission = sample.submission
        submission.identifier = "o0000001"
        submission.submitter = entityFactory.getPerson()

        val col1 = entityFactory.getMetaDataColumn()
        val col2 = entityFactory.getMetaDataColumn("col2", "TechnicalSample", "center")
        val col3 = entityFactory.getMetaDataColumn("col3", "File", "fileName")
        val col4 = entityFactory.getMetaDataColumn("col4", "Submission", "identifier")

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(col1, col2, col3, col4))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("target/TSV_BASEPATH/SUBFOLDER/"))
        `when`(runtimeOptionsRepository.findByName("tsvExternalSubpath")).thenReturn(entityFactory.getRuntimeOption("TSV_EXTERNAL"))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption("TSV_INTERNAL"))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(fileRepository.findAllBySample(sample)).thenReturn(listOf(file))
        `when`(runtimeOptionsRepository.findByName("unknownValuesAllowedOrganizationalUnits")).thenReturn(entityFactory.getRuntimeOption("OE_FAKE"))

        val tsvContent = fileServiceMock.createLongTsvFile(submission)

        assertThat(tsvContent.trim().split("\n").size).isEqualTo(2)
        val headerRow = tsvContent.split("\n")[0].split("\t")
        assertThat(headerRow).doesNotContain("a1", "a2")
        val dataRow = tsvContent.split("\n")[1].split("\t")
        assertThat(dataRow[0]).isEqualTo(sample.name)
        assertThat(dataRow[1]).isEqualTo(technicalSample.center)
        assertThat(dataRow[2]).isEqualTo(file.fileName)
        assertThat(dataRow[3]).isEqualTo(submission.identifier)
        assertThat(dataRow).doesNotContain("b1", "b2")
    }

    @Test
    fun `test create long tsv without export names`() {
        val file = entityFactory.getFile()
        val sample = file.sample
        val submission = sample.submission

        val col1 = entityFactory.getMetaDataColumn()
        val col2 = entityFactory.getMetaDataColumn()
        val col3 = entityFactory.getMetaDataColumn()
        col3.reflectionClassName = "File"
        col3.reflectionPropertyNameExport = "fileName"

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(col1, col2, col3))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(fileRepository.findAllBySample(sample)).thenReturn(listOf(file))

        val tsvContent = fileServiceMock.createLongTsvFile(submission, withExportNames = false)

        assertThat(tsvContent.trim().split("\n").size).isEqualTo(2)
        val headerRow = tsvContent.split("\n")[0].split("\t")
        assertThat(headerRow[0]).isEqualTo(col1.columnName)
        assertThat(headerRow[1]).isEqualTo(col2.columnName)
        assertThat(headerRow[2]).isNotEqualTo(col3.exportName)
    }

    @Test
    fun `test write long tsv`() {
        val file = entityFactory.getFile()
        val sample = file.sample
        val submission = sample.submission
        submission.submitter = entityFactory.getPerson()

        val col1 = entityFactory.getMetaDataColumn()
        val col2 = entityFactory.getMetaDataColumn()
        val col3 = entityFactory.getMetaDataColumn()
        col3.reflectionClassName = "File"
        col3.reflectionPropertyNameExport = "fileName"

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(col1, col2, col3))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("target/"))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption("sub/"))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        val path = fileServiceMock.writeLongTsvFile(submission)

        assertThat(File(path)).isFile
    }

    @Test
    fun `test upload tsv`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val seqType = entityFactory.getSeqType()
        val fis = FileInputStream("testData/simple.tsv")
        val multipartFile = MockMultipartFile("file", fis)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(seqTypeMappingService.getSeqType("RNA")).thenReturn(seqType)
        `when`(collectorService.getImportableProjects(submission)).thenReturn(setOf("3"))

        fileServiceMock.readTsvFile(submission, multipartFile)

        assertThat(sample.name).isEqualTo("sampleIdentifier")
        assertThat(sample.project).isEqualTo("3")
        assertThat(sample.pid).isEqualTo("4")
        assertThat(sample.sampleType).isEqualTo("5")
        assertThat(sample.xenograft).isEqualTo(false)
        assertThat(sample.sex).isEqualTo(Sample.Sex.MALE)
        assertThat(sample.phenotype).isEqualTo("8")
        assertThat(sample.libraryLayout).isEqualTo(Sample.LibraryLayout.PAIRED)
        assertThat(sample.seqType).isEqualTo(seqType)
        assertThat(sample.tagmentationLibrary).isEqualTo("12")
        assertThat(sample.antibodyTarget).isEqualTo("13")
        assertThat(sample.antibody).isEqualTo("14")
        assertThat(sample.singleCellPlate).isEqualTo("15")
        assertThat(sample.singleCellWellPosition).isEqualTo("16")
        assertThat(sample.comment).isEqualTo("17")
    }

    @Test
    fun `test upload tsv with wrong number of rows`() {
        val sample = entityFactory.getSample()
        val sample2 = entityFactory.getSample(sample.submission)
        val submission = sample.submission
        val fis = FileInputStream("testData/simple.tsv")
        val multipartFile = MockMultipartFile("file", fis)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample, sample2))

        assertThatExceptionOfType(RowNotFoundException::class.java).isThrownBy {
            fileServiceMock.readTsvFile(submission, multipartFile)
        }.withMessage("Number of samples in the uploaded file is 1 but expected are 2 samples")
    }

    @Test
    fun `test upload tsv with wrong sample identifier`() {
        val sample = entityFactory.getSample()
        sample.name = "123"
        val submission = sample.submission
        val fis = FileInputStream("testData/simple.tsv")
        val multipartFile = MockMultipartFile("file", fis)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        assertThatExceptionOfType(SampleNamesDontMatchException::class.java).isThrownBy {
            fileServiceMock.readTsvFile(submission, multipartFile)
        }.withMessage(
            "Expected samples with following names: [${sample.name}]\n" +
                "You provided samples with names that could not be matched to existing samples: [sampleIdentifier]"
        )
    }

    @TestFactory
    fun `test upload long file`() = listOf(
        "TSV" to "testData/long.tsv",
        "XLS" to "testData/long.xls",
        "XLSX" to "testData/long.xlsx",
    ).map { (fileType, filePath) ->
        DynamicTest.dynamicTest("test upload long $fileType file") {
            val file = entityFactory.getFile()
            file.md5 = "hdks0098df7fe7508cc6h1c384316f4h"
            val sample = file.sample
            val fis = FileInputStream(filePath)
            val multipartFile = MockMultipartFile("file", fis)

            `when`(fileRepository.findAllBySampleIn(listOf(sample))).thenReturn(listOf(file))

            val rows = when (fileType) {
                "TSV" -> fileServiceMock.readFromCsv(multipartFile.inputStream)
                "XLS" -> fileServiceMock.readFromXls(multipartFile.inputStream)
                "XLSX" -> fileServiceMock.readFromXlsx(multipartFile.inputStream)
                else -> listOf(mutableMapOf())
            }

            assertThat(rows.size).isEqualTo(1)
            val row = rows[0]
            assertThat(row["SAMPLE_NAME"]).isEqualTo("ABX_01_tumor03")
            assertThat(row["FASTQ_FILE"]).isEqualTo("xxxx_ATCACG_L008_R1.fastq.gz")
            assertThat(row["MD5"]).isEqualTo("hdks0098df7fe7508cc6h1c384316f4h")
            assertThat(row["READ"]).isEqualTo("1")
            assertThat(row["PROJECT"]).isEqualTo("")
            assertThat(row["SAMPLE_TYPE"]).isEqualTo("tumor03")
            assertThat(row["SEQUENCING_READ_TYPE"]).isEqualTo("PAIRED")
            assertThat(row["SEQUENCING_TYPE"]).isEqualTo("EXON")
        }
    }

    @Test
    fun `test upload long tsv`() {
        val file = entityFactory.getFile()
        file.md5 = "hdks0098df7fe7508cc6h1c384316f4h"
        val sample = file.sample
        val fis = FileInputStream("testData/long.tsv")
        val multipartFile = MockMultipartFile("file", fis)

        `when`(fileRepository.findAllBySampleIn(listOf(sample))).thenReturn(listOf(file))

        val rows = fileServiceMock.readFromCsv(multipartFile.inputStream)

        assertThat(rows.size).isEqualTo(1)
        val row = rows[0]
        assertThat(row["SAMPLE_NAME"]).isEqualTo("ABX_01_tumor03")
        assertThat(row["FASTQ_FILE"]).isEqualTo("xxxx_ATCACG_L008_R1.fastq.gz")
        assertThat(row["MD5"]).isEqualTo("hdks0098df7fe7508cc6h1c384316f4h")
        assertThat(row["READ"]).isEqualTo("1")
        assertThat(row["PROJECT"]).isEqualTo("")
        assertThat(row["SAMPLE_TYPE"]).isEqualTo("tumor03")
        assertThat(row["SEQUENCING_READ_TYPE"]).isEqualTo("PAIRED")
        assertThat(row["SEQUENCING_TYPE"]).isEqualTo("EXON")
    }

    @Test
    fun `test names for tsv files missing basepath`() {
        val submission = entityFactory.getUploadSubmission()
        val sample1 = entityFactory.getSample(submission)
        sample1.project = "project1"
        sample1.technicalSample = entityFactory.getTechnicalSample(sample1)
        val sample2 = entityFactory.getSample(submission)
        sample2.project = "project2"
        sample2.technicalSample = entityFactory.getTechnicalSample(sample2)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample1, sample2))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(null)

        assertThatExceptionOfType(MissingRuntimeOptionException::class.java)
            .isThrownBy { fileServiceMock.getNamesForTsvFiles(submission) }
            .withMessage("RuntimeOption 'tsvBasePath' is not set!")
    }

    @Test
    fun `test names for tsv files missing internal subpath`() {
        val sample1 = entityFactory.getSample()
        sample1.project = "project1"
        val submission = sample1.submission
        submission.identifier = "i0000001"
        val sample2 = entityFactory.getSample(submission)
        sample2.project = "project2"

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample1, sample2))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("TSV_BASEPATH/"))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(null)

        assertThatExceptionOfType(MissingRuntimeOptionException::class.java)
            .isThrownBy { fileServiceMock.getNamesForTsvFiles(submission) }
            .withMessage("RuntimeOption 'tsvInternalSubpath' is not set!")
    }

    @Test
    fun `test names for tsv files missing external subpath`() {
        val submission = entityFactory.getUploadSubmission()
        val sample1 = entityFactory.getSample(submission)
        sample1.project = "project1"
        sample1.technicalSample = entityFactory.getTechnicalSample(sample1)
        val sample2 = entityFactory.getSample(submission)
        sample2.project = "project2"
        sample2.technicalSample = entityFactory.getTechnicalSample(sample2)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample1, sample2))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("TSV_BASEPATH/"))
        `when`(runtimeOptionsRepository.findByName("tsvExternalSubpath")).thenReturn(null)

        assertThatExceptionOfType(MissingRuntimeOptionException::class.java)
            .isThrownBy { fileServiceMock.getNamesForTsvFiles(submission) }
            .withMessage("RuntimeOption 'tsvExternalSubpath' is not set!")
    }

    @Test
    fun `test names for tsv files internal`() {
        val sample1 = entityFactory.getSample()
        sample1.project = "project1"
        val submission = sample1.submission
        submission.identifier = "i0012345"
        val sample2 = entityFactory.getSample(submission)
        sample2.project = "project2"

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample1, sample2))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("TSV_BASEPATH/"))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption("internal/core/<ILSE_PREFIX>/<ILSE_ID>/<SUBMISSION_ID>.tsv"))

        val filePaths = fileServiceMock.getNamesForTsvFiles(submission)

        assertThat(filePaths.size).isEqualTo(1)
        assertThat(filePaths.first()).isEqualTo("TSV_BASEPATH/internal/core/012/012345/12345.tsv")
    }

    @Test
    fun `test names for tsv files external`() {
        val submission = entityFactory.getUploadSubmission()
        val sample1 = entityFactory.getSample(submission)
        sample1.project = "project1"
        sample1.technicalSample = entityFactory.getTechnicalSample(sample1)
        val sample2 = entityFactory.getSample(submission)
        sample2.project = "project2"
        sample2.technicalSample = entityFactory.getTechnicalSample(sample2)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample1, sample2))
        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("TSV_BASEPATH/"))
        `when`(runtimeOptionsRepository.findByName("tsvExternalSubpath")).thenReturn(entityFactory.getRuntimeOption("external/<PROJECT>/<DATE>-<SUBMISSION_ID>.tsv"))

        val filePaths = fileServiceMock.getNamesForTsvFiles(submission)

        assertThat(filePaths.size).isEqualTo(2)
        assertThat(filePaths.first()).startsWith("TSV_BASEPATH/external/")
        assertThat(filePaths.first()).isEqualTo("TSV_BASEPATH/external/${sample1.project}/${SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())}-${submission.identifier}.tsv")
    }

    @Test
    fun `convert file gui dto to file with existing file`() {
        val fileGuiDto = entityFactory.getFileGuiDto()
        val file = entityFactory.getFile()

        `when`(fileRepository.getOne(fileGuiDto.id)).thenReturn(file)

        val resultFile = fileServiceMock.convertToEntity(fileGuiDto, file.sample)

        assertThat(resultFile.id).isEqualTo(fileGuiDto.id)
        assertThat(resultFile.fileName).isEqualTo(fileGuiDto.fileName)
        assertThat(resultFile.readNumber).isEqualTo(fileGuiDto.readNumber)
        assertThat(resultFile.md5).isEqualTo(fileGuiDto.md5)
        assertThat(resultFile.baseCount).isEqualTo(fileGuiDto.baseCount)
        assertThat(resultFile.cycleCount).isEqualTo(fileGuiDto.cycleCount)
    }

    @Test
    fun `convert sample gui dto to sample with new file`() {
        val fileGuiDto = entityFactory.getFileGuiDto()
        fileGuiDto.id = 0

        val resultFile = fileServiceMock.convertToEntity(fileGuiDto, entityFactory.getSample())

        assertThat(resultFile.id).isEqualTo(fileGuiDto.id)
        assertThat(resultFile.fileName).isEqualTo(fileGuiDto.fileName)
        assertThat(resultFile.readNumber).isEqualTo(fileGuiDto.readNumber)
        assertThat(resultFile.md5).isEqualTo(fileGuiDto.md5)
        assertThat(resultFile.baseCount).isEqualTo(fileGuiDto.baseCount)
        assertThat(resultFile.cycleCount).isEqualTo(fileGuiDto.cycleCount)
    }

    @Test
    fun `test upload tsv with invalid project`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val seqType = entityFactory.getSeqType()
        val fis = FileInputStream("testData/simple.tsv")
        val multipartFile = MockMultipartFile("file", fis)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(seqTypeMappingService.getSeqType("RNA")).thenReturn(seqType)

        fileServiceMock.readTsvFile(submission, multipartFile)

        assertThat(sample.name).isEqualTo("sampleIdentifier")
        assertThat(sample.project).isEqualTo("")
        assertThat(sample.pid).isEqualTo("4")
        assertThat(sample.sampleType).isEqualTo("5")
        assertThat(sample.xenograft).isEqualTo(false)
        assertThat(sample.sex).isEqualTo(Sample.Sex.MALE)
        assertThat(sample.phenotype).isEqualTo("8")
        assertThat(sample.libraryLayout).isEqualTo(Sample.LibraryLayout.PAIRED)
        assertThat(sample.seqType).isEqualTo(seqType)
        assertThat(sample.tagmentationLibrary).isEqualTo("12")
        assertThat(sample.antibodyTarget).isEqualTo("13")
        assertThat(sample.antibody).isEqualTo("14")
        assertThat(sample.singleCellPlate).isEqualTo("15")
        assertThat(sample.singleCellWellPosition).isEqualTo("16")
        assertThat(sample.comment).isEqualTo("17")
    }

    @Test
    fun `check functionality convertStringToFile`() {
        val content = "Test content"
        val submissionIdentifier = "o0000001"
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val expectedFileName = "${submissionIdentifier}_${timestamp}\\.tsv".toRegex()

        val file = fileServiceMock.convertStringToTSVFile(submissionIdentifier, content)

        assertThat(file.exists()).isTrue
        assertThat(file.name.matches(expectedFileName)).isTrue

        val fileContent = file.readText()
        assertThat(content).isEqualTo(fileContent)
    }
}
