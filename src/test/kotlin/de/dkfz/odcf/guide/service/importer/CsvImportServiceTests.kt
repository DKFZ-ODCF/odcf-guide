package de.dkfz.odcf.guide.service.importer

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.submissionData.*
import de.dkfz.odcf.guide.exceptions.ColumnNotFoundException
import de.dkfz.odcf.guide.exceptions.FastQFileNameRejectedException
import de.dkfz.odcf.guide.exceptions.Md5SumFoundInDifferentSubmission
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.MetaDataColumnServiceImpl
import de.dkfz.odcf.guide.service.implementation.importer.CsvImportServiceImpl
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.SpeciesService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import de.dkfz.odcf.guide.service.interfaces.validator.SampleService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import java.io.FileInputStream
import java.util.*
import javax.persistence.EntityManager

@SpringBootTest
class CsvImportServiceTests @Autowired constructor(private val csvImportServiceImpl: CsvImportServiceImpl) : AnyObject {

    private val entityFactory = EntityFactory()

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @InjectMocks
    lateinit var csvImportServiceImplMock: CsvImportServiceImpl

    @Mock
    lateinit var importService: ImportService

    @Mock
    lateinit var seqTypeMappingService: SeqTypeMappingService

    @Mock
    lateinit var ldapService: LdapService

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var fileService: FileService

    @Mock
    lateinit var sampleService: SampleService

    @Mock
    lateinit var mailService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var metaDataColumnService: MetaDataColumnServiceImpl

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var speciesService: SpeciesService

    @Mock
    lateinit var metaDataColumnRepository: MetaDataColumnRepository

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var technicalSampleRepository: TechnicalSampleRepository

    @Mock
    lateinit var fileRepository: FileRepository

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var validationLevelRepository: ValidationLevelRepository

    @Mock
    lateinit var deletionService: DeletionService

    @Mock
    lateinit var submissionService: SubmissionService

    @Mock
    lateinit var entityManager: EntityManager

    @Test
    fun `When prepareFileData then return File`() {
        val fileCsvRow = entityFactory.getExampleRenamedCsvRows()[0].toMutableMap()

        `when`(metaDataColumnRepository.findAll()).thenReturn(entityFactory.getExampleMetadataColumns())
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { fileCsvRow.remove(it.getArgument(0)).orEmpty() }

        val file = csvImportServiceImplMock.prepareFileData(fileCsvRow, null)

        assertThat(file.fileName).isEqualTo("testfile_R1.fastq.gz")
        assertThat(file.baseCount).isEqualTo(1)
        assertThat(file.cycleCount).isEqualTo(2)
    }

    @Test
    fun `When saveTechnicalSample then return TechnicalSample`() {
        val tSampleCsvRow = entityFactory.getExampleRenamedCsvRows()[2].toMutableMap()
        var tSample = TechnicalSample()

        `when`(technicalSampleRepository.saveAndFlush(tSample)).thenReturn(tSample)
        `when`(metaDataColumnRepository.findAll()).thenReturn(entityFactory.getExampleMetadataColumns())
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { tSampleCsvRow.remove(it.getArgument(0)).orEmpty() }

        tSample = csvImportServiceImplMock.saveTechnicalSample(tSampleCsvRow, null)

        assertThat(tSample.barcode).isEqualTo("AGCT")
        assertThat(tSample.lane).isEqualTo(1)
        assertThat(tSample.pipelineVersion).isEqualTo("unknown")
        assertThat(tSample.readCount).isEqualTo(2)
    }

    @Test
    fun `Check removeTechnicalSampleFromRow functionality`() {
        val tSampleCsvRow = entityFactory.getExampleRenamedCsvRows()[2].toMutableMap()
        val exampleMetadataColumns = entityFactory.getExampleMetadataColumns()

        `when`(metaDataColumnRepository.findAll()).thenReturn(exampleMetadataColumns)

        csvImportServiceImplMock.removeTechnicalSampleFromRow(tSampleCsvRow)

        verify(metaDataColumnService, times(exampleMetadataColumns.filter { it.reflectionClassName == "TechnicalSample" }.size)).removeValueOrEmptyValue(anyString(), anyOrNull())
    }

    @Test
    fun `When saveSubmission then return Submission`() {
        val submission = entityFactory.getApiSubmission()
        val person = entityFactory.getPerson()

        `when`(submissionRepository.saveAndFlush(submission)).thenReturn(submission)
        `when`(ldapService.getPersonByMail("email")).thenReturn(person)
        `when`(importService.generateInternalIdentifier()).thenReturn(submission.identifier)
        `when`(validationLevelRepository.findByDefaultObjectIsTrue()).thenReturn(setOf(entityFactory.getValidationLevel()))

        val result = csvImportServiceImplMock.saveSubmission("ticket", "email")

        assertThat(result.ticketNumber).isEqualTo("ticket")
        assertThat(result.submitter).isEqualTo(person)
        assertThat(result.resettable).isEqualTo(false)
        assertThat(result.startTerminationPeriod).isNotNull
    }

    @TestFactory
    fun `test functionality of checkAuthorization for unauthorized users`() = listOf(
        "TSV" to "testData/long.tsv",
        "XLS" to "testData/long.xls",
        "XLSX" to "testData/long.xlsx",
    ).map { (fileType, filePath) ->
        DynamicTest.dynamicTest("When import $fileType return submission") {
            val submission = entityFactory.getUploadSubmission()
            val person = entityFactory.getPerson()
            val fis = FileInputStream(filePath)
            val multipartFile = MockMultipartFile("file", fis)

            `when`(submissionRepository.saveAndFlush(submission)).thenReturn(submission)
            `when`(ldapService.getPersonByMail("email")).thenReturn(person)
            `when`(importService.generateInternalIdentifier()).thenReturn(submission.identifier)
            `when`(validationLevelRepository.findByDefaultObjectIsTrue()).thenReturn(setOf(entityFactory.getValidationLevel()))

            val resultSubmission = csvImportServiceImplMock.import(multipartFile, "ticket", "email", ignoreMd5Check = false)

            assertThat(resultSubmission.ticketNumber).isEqualTo("ticket")
            assertThat(resultSubmission.submitter).isEqualTo(person)
            assertThat(resultSubmission.resettable).isEqualTo(false)
            assertThat(resultSubmission.startTerminationPeriod).isNotNull
        }
    }

    @Test
    fun `Test saveFilesAndSamples throws exception while import`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getUploadSubmission()
            val person = entityFactory.getPerson()
            val fis = FileInputStream("testData/long_invalid.tsv")
            val multipartFile = MockMultipartFile("file", fis)

            `when`(submissionRepository.saveAndFlush(submission)).thenReturn(submission)
            `when`(ldapService.getPersonByMail("email")).thenReturn(person)
            `when`(importService.generateInternalIdentifier()).thenReturn(submission.identifier)
            `when`(validationLevelRepository.findByDefaultObjectIsTrue()).thenReturn(setOf(entityFactory.getValidationLevel()))
            `when`(mailContentGeneratorService.getMailBody(anyString(), anyMap())).thenReturn("")

            csvImportServiceImplMock.import(multipartFile, "ticket", "email", ignoreMd5Check = true)
            delay(200)

            verify(mailService, times(1)).sendMailToSubmitter(anyString(), anyString(), anyString())
            verify(deletionService, times(1)).deleteSubmission(anySubmission(), anyBoolean())
            val identifier = submission.identifier.filter { it.isDigit() }.toInt()
            verify(entityManager, times(1)).createNativeQuery("select setval('internal_submission_id', $identifier, false)")
        }
    }

    @Test
    fun `When checkMd5Sums without column 'md5 sum' throw Exception`() {
        val csvRows: List<MutableMap<String, String>> = listOf(mutableMapOf("file name" to "fileName"))

        `when`(metaDataColumnService.getColumn(anyString())).thenReturn(entityFactory.getMetaDataColumn("md5 sum", "File", "md5"))
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()

        assertThatExceptionOfType(ColumnNotFoundException::class.java).isThrownBy {
            csvImportServiceImplMock.checkMd5Sums(csvRows)
        }.withMessage("Column 'md5 sum' not found.")
    }

    @Test
    fun `When checkMd5Sums with duplicated 'md5 sum' throw Exception`() {
        val file = entityFactory.getFile()
        val columnFileName = entityFactory.getMetaDataColumn("fastq file name", "File", "fileName")
        val columnMd5 = entityFactory.getMetaDataColumn("md5 sum", "File", "md5")
        val csvRows: List<MutableMap<String, String>> = listOf(mutableMapOf("fastq file name" to "fileName", "md5 sum" to "md5"))
        val columns = listOf(columnFileName, columnMd5)

        `when`(fileRepository.findAllByMd5In(listOf("md5"))).thenReturn(listOf(file))
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()

        assertThatExceptionOfType(Md5SumFoundInDifferentSubmission::class.java).isThrownBy {
            csvImportServiceImplMock.checkMd5Sums(csvRows)
        }.withMessage("Import paused, because [1 of 1] md5 sums were found in one or more different submissions.")
    }

    @Test
    fun `When saveSubmission then return submission`() {
        val person = entityFactory.getPerson()
        val submission = UploadSubmission("o000001", UUID.randomUUID(), "ticketNumber", person, "")

        `when`(importService.generateInternalIdentifier()).thenReturn("o000001")
        `when`(ldapService.getPersonByMail("e@mail")).thenReturn(person)
        `when`(submissionRepository.saveAndFlush(submission)).thenReturn(submission)
        `when`(validationLevelRepository.findByDefaultObjectIsTrue()).thenReturn(setOf(entityFactory.getValidationLevel()))

        val saveSubmission = csvImportServiceImplMock.saveSubmission("ticketnumber", "e@mail")

        assertThat(saveSubmission.identifier).isEqualTo(submission.identifier)
        assertThat(saveSubmission.submitter).isEqualTo(person)
        assertThat(saveSubmission.startTerminationPeriod).isNotNull
    }

    /*@Test
    fun `save file to sample`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val row = entityFactory.getExampleCsvRows().toMutableMap()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }
        `when`(externalMetadataSourceService.getSingleValue("sampleTypeCaseInsensitive", mapOf("sampleType" to "sample_type"))).thenReturn("sample_type")

        csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(sample.technicalSample).isNotNull
        assertThat(sample.files.size).isEqualTo(1)
    }

    @Test
    fun `save file to sample with empty sample`() {
        val submission = entityFactory.getApiSubmission()
        val sample = Sample(submission)
        val seqType = entityFactory.getSeqType()
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        val columns = entityFactory.getExampleMetadataColumns()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(speciesService.getSpeciesWithStrainForSpecies(anyString())).thenReturn("species")
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }
        `when`(externalMetadataSourceService.getSingleValue("sampleTypeCaseInsensitive", mapOf("sampleType" to "sample_type"))).thenReturn("sample_type")

        csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(sample.name).isEqualTo("testsample")
        assertThat(sample.antibodyTarget).isEqualTo("antibodyTarget")
        assertThat(sample.technicalSample).isNotNull
        assertThat(sample.files.size).isEqualTo(1)
        assertThat(sample.speciesWithStrain).isEqualTo("species")
    }

    @Test
    fun `save file to sample with same seq type`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val seqType = sample.seqType!!
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        row["sequencing type"] = seqType.name

        `when`(seqTypeMappingService.getSeqType(seqType.name)).thenReturn(seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val warnings = csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(warnings).isEmpty()
    }

    @Test
    fun `save file to sample with different seq type`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        val otherSeqType = entityFactory.getSeqType()
        row["sequencing type"] = otherSeqType.name
        val columns = entityFactory.getExampleMetadataColumns()

        `when`(seqTypeMappingService.getSeqType(sample.seqType!!.name)).thenReturn(sample.seqType)
        `when`(seqTypeMappingService.getSeqType(otherSeqType.name)).thenReturn(otherSeqType)
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val warnings = csvImportServiceImplMock.saveFileToSample(submission, row.toMutableMap())

        assertThat(warnings).isNotEmpty
        assertThat(warnings.first()).isEqualTo("Identified seqType mismatch! (GPCF: '${otherSeqType.name}' vs Guide: '${sample.seqType!!.name}')")
    }

    @Test
    fun `save file to sample with same antibody target`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val antibodyTarget = sample.antibodyTarget
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        row["antibody target"] = antibodyTarget

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val warnings = csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(warnings).isEmpty()
    }

    @Test
    fun `save file to sample with different antibody target`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val antibodyTarget = sample.antibodyTarget
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        row["antibody target"] = "other antibody target"
        val columns = entityFactory.getExampleMetadataColumns()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val warnings = csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(warnings).isNotEmpty
        assertThat(warnings.first()).isEqualTo("Identified antibodyTarget mismatch! (GPCF: 'other antibody target' vs Guide: '$antibodyTarget')")
    }

    @Test
    fun `save file to sample with single cell inconsistency`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        row["base material"] = "Single-cell RNA"

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            csvImportServiceImplMock.saveFileToSample(submission, row)
        }.withMessageStartingWith("Single cell inconsistency found in ${submission.identifier} while merging sample with name '${sample.name}")
    }

    @Test
    fun `save file to sample and check absolute path`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        val columns = entityFactory.getExampleMetadataColumns()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(runtimeOptionsRepository.findByName("fastqPathMidterm")).thenReturn(entityFactory.getRuntimeOption("/path/{1}/{2}/{3}/{4}"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(sample.technicalSample).isNotNull
        assertThat(sample.files.size).isEqualTo(1)
        assertThat(sample.files[0].fileName).isEqualTo("/path/${String.format("%06d", submission.identifier.filter { it.isDigit() }.toInt())}/some_run_id/testfile/testfile_R1.fastq.gz")
    }

    @Test
    fun `check saveFileToSample returns empty Set when sample is null`() {
        val submission = entityFactory.getApiSubmission()
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        row["uuid (do not change)"] = "ff8df912-8391-4588-8d4b-d3996bb9b8f9"
        val file = entityFactory.getFile()
        val columns = entityFactory.getExampleMetadataColumns()

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenCallRealMethod()
        `when`(speciesService.getSpeciesWithStrainForSpecies("some_species")).thenReturn("some_species")
        `when`(externalMetadataSourceService.getSingleValue("sampleTypeCaseInsensitive", mapOf("sampleType" to "sample_type"))).thenReturn("sample_type")
        `when`(fileRepository.findByUuid(UUID.fromString("ff8df912-8391-4588-8d4b-d3996bb9b8f9"))).thenReturn(file)

        val warnings = csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(warnings).isEqualTo(emptySet<String>())
    }

    @Test
    fun `import additional with not empty sample`() {
        val ilse = 123456
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val row = entityFactory.getExampleCsvRows().toMutableMap()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(env.getRequiredProperty("application.midterm")).thenReturn("/path/<ILSE>")
        `when`(fileService.readTsvFile("/path/$ilse")).thenReturn(listOf(row))
        `when`(importService.generateIlseIdentifier(ilse)).thenReturn("")
        `when`(submissionRepository.findByIdentifier(anyString())).thenReturn(submission)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> entityFactory.getExampleMetadataColumns().find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val (resultSubmission, warnings) = csvImportServiceImplMock.importAdditional(ilse)

        assertThat(warnings).isEmpty()
        assertThat(resultSubmission).isNotNull
    }

    @Test
    fun `import additional with no sample`() {
        val ilse = 123456
        val submission = entityFactory.getApiSubmission()
        val columns = entityFactory.getExampleMetadataColumns()
        val row = entityFactory.getExampleCsvRows().toMutableMap()

        `when`(env.getRequiredProperty("application.midterm")).thenReturn("/path/<ILSE>")
        `when`(fileService.readTsvFile("/path/$ilse")).thenReturn(listOf(row))
        `when`(importService.generateIlseIdentifier(ilse)).thenReturn("")
        `when`(submissionRepository.findByIdentifier(anyString())).thenReturn(submission)
        `when`(speciesService.getSpeciesWithStrainForSpecies(anyString())).thenReturn("SPECIES")
        `when`(externalMetadataSourceService.getSingleValue("sampleTypeCaseInsensitive", mapOf("sampleType" to "sample_type"))).thenReturn("sample_type")
        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val (_, warnings) = csvImportServiceImplMock.importAdditional(ilse)

        assertThat(warnings).isEmpty()
    }

    @Test
    fun `import additional with antibodyTarget mismatch`() {
        val ilse = 123456
        val sample = entityFactory.getSample()
        sample.antibodyTarget = "TargetAntibody"
        val submission = sample.submission
        val columns = entityFactory.getExampleMetadataColumns()
        val row = entityFactory.getExampleCsvRows().toMutableMap()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(env.getRequiredProperty("application.midterm")).thenReturn("/path/<ILSE>")
        `when`(fileService.readTsvFile("/path/$ilse")).thenReturn(listOf(row))
        `when`(importService.generateIlseIdentifier(ilse)).thenReturn("")
        `when`(submissionRepository.findByIdentifier(anyString())).thenReturn(submission)
        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val (_, warnings) = csvImportServiceImplMock.importAdditional(ilse)

        assertThat(warnings).isNotEmpty
        assertThat(warnings).contains(
            bundle.getString("merging.warning.antibodyTarget")
                .replace("{0}", "antibodyTarget")
                .replace("{1}", sample.antibodyTarget)
        )
    }

    @Test
    fun `import additional with singleCell mismatch`() {
        val ilse = 123456
        val sample = entityFactory.getSample()
        sample.seqType!!.singleCell = true
        val submission = sample.submission
        val columns = entityFactory.getExampleMetadataColumns()
        val row = entityFactory.getExampleCsvRows().toMutableMap()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(env.getRequiredProperty("application.midterm")).thenReturn("/path/<ILSE>")
        `when`(fileService.readTsvFile("/path/$ilse")).thenReturn(listOf(row))
        `when`(importService.generateIlseIdentifier(ilse)).thenReturn("")
        `when`(submissionRepository.findByIdentifier(anyString())).thenReturn(submission)
        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            csvImportServiceImplMock.importAdditional(ilse)
        }.withMessageStartingWith(
            "Single cell inconsistency found in ${submission.identifier} while merging sample with name '${sample.name}'"
        )
    }

    @Test
    fun `import additional with seqType mismatch`() {
        val ilse = 123456
        val sample = entityFactory.getSample()
        val submission = sample.submission
        val columns = entityFactory.getExampleMetadataColumns()
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        val newSeqType = entityFactory.getSeqType()

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(newSeqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(env.getRequiredProperty("application.midterm")).thenReturn("/path/<ILSE>")
        `when`(fileService.readTsvFile("/path/$ilse")).thenReturn(listOf(row))
        `when`(importService.generateIlseIdentifier(ilse)).thenReturn("")
        `when`(submissionRepository.findByIdentifier(anyString())).thenReturn(submission)
        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenAnswer { row.remove(it.getArgument(0)).orEmpty() }

        val (_, warnings) = csvImportServiceImplMock.importAdditional(ilse)

        assertThat(warnings).isNotEmpty
        assertThat(warnings).contains(
            bundle.getString("merging.warning.seqType")
                .replace("{0}", newSeqType.name)
                .replace("{1}", sample.seqType!!.name)
        )
    }

    @Test
    fun `import additional empty row`() {
        val ilse = 123456
        val sample = entityFactory.getSample()
        val submission = sample.submission

        `when`(seqTypeMappingService.getSeqType(anyString())).thenReturn(sample.seqType)
        `when`(importService.findSampleByFileName(anyString(), anySubmission())).thenReturn(sample)
        `when`(env.getRequiredProperty("application.midterm")).thenReturn("/path/<ILSE>")
        `when`(fileService.readTsvFile("/path/$ilse")).thenReturn(emptyList())
        `when`(importService.generateIlseIdentifier(ilse)).thenReturn("")
        `when`(submissionRepository.findByIdentifier(anyString())).thenReturn(submission)

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            csvImportServiceImplMock.importAdditional(ilse)
        }.withMessageStartingWith("No rows given in the tsv file from midterm")
    }*/

    @Test
    fun `check functionality of saveFileAndSample for initial upload`() {
        val submission = entityFactory.getApiSubmission()
        val columns = entityFactory.getExampleMetadataColumns()
        val csvRow = entityFactory.getExampleCsvRows().toMutableMap()
        val fileCaptor = ArgumentCaptor.forClass(File::class.java)

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrEmptyValue(any(), any())).thenCallRealMethod()
        `when`(speciesService.getSpeciesWithStrainForSpecies("some_species")).thenReturn("some_species")
        `when`(externalMetadataSourceService.getSingleValue("sampleTypeCaseInsensitive", mapOf("sampleType" to "sample_type"))).thenReturn("sample_type")

        val sample = csvImportServiceImplMock.saveFileAndSample(submission, csvRow, false, true)

        assertThat(sample).isNotNull
        assertThat(sample!!.submission).isEqualTo(submission)
        assertThat(sample.name).isEqualTo("testsample")
        assertThat(sample.sex).isEqualTo(Sample.Sex.OTHER)
        verify(fileRepository, times(1)).saveAndFlush(fileCaptor.capture())
        assertThat(fileCaptor.value.fileName).isEqualTo("testfile_R1.fastq.gz")
        assertThat(sample.technicalSample).isNotNull
        assertThat(sample.technicalSample!!.runId).isEqualTo("some_run_id")
    }

    @Test
    fun `check saveFileAndSample throws error when wrong file ending`() {
        val submission = entityFactory.getApiSubmission()
        val columns = entityFactory.getExampleMetadataColumns()
        val csvRow = entityFactory.getExampleCsvRows().toMutableMap()
        csvRow["fastq file name"] = "testfile"

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()

        assertThatExceptionOfType(FastQFileNameRejectedException::class.java).isThrownBy {
            csvImportServiceImplMock.saveFileAndSample(submission, csvRow, false, true)
        }.withMessageStartingWith("The fastq filename does not end with '_[R|I][1|2]\\.fastq\\.gz'")
    }

    @Test
    fun `check saveFileAndSample returns null when filename to ignore`() {
        val submission = entityFactory.getApiSubmission()
        val columns = entityFactory.getExampleMetadataColumns()
        val csvRow = entityFactory.getExampleCsvRows().toMutableMap()
        csvRow["fastq file name"] = "Undetermined"

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()

        val sample = csvImportServiceImplMock.saveFileAndSample(submission, csvRow, false, true)

        assertThat(sample).isNull()
    }

    @Test
    fun `check saveFileAndSample throws error when uuid is not given and not initialUpload`() {
        val submission = entityFactory.getApiSubmission()
        val columns = entityFactory.getExampleMetadataColumns()
        val csvRow = entityFactory.getExampleCsvRows().toMutableMap()

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()
        `when`(metaDataColumnService.removeValueOrThrowException(anyString(), anyMap())).thenCallRealMethod()

        assertThatExceptionOfType(ColumnNotFoundException::class.java).isThrownBy {
            csvImportServiceImplMock.saveFileAndSample(submission, csvRow, false)
        }.withMessageStartingWith("Column 'uuid (do not change)' not found.")
    }

    /*@Test
    fun `save file to sample with empty filename`() {
        val submission = entityFactory.getApiSubmission()
        val columns = entityFactory.getExampleMetadataColumns()
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        row["fastq file name"] = ""

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            csvImportServiceImplMock.saveFileToSample(submission, row)
        }.withMessageStartingWith("The column file name is not given or empty. Please provide a filename for each row.")
    }

    @Test
    fun `save file to sample with filename to ignore`() {
        val submission = entityFactory.getApiSubmission()
        val columns = entityFactory.getExampleMetadataColumns()
        val row = entityFactory.getExampleCsvRows().toMutableMap()
        row["fastq file name"] = "Undetermined"

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(metaDataColumnRepository.findAll()).thenReturn(columns)
        `when`(metaDataColumnService.getColumn(anyString())).then { mock -> columns.find { it.importNames.contains(mock.getArgument(0)) } }
        `when`(metaDataColumnService.getValue(anyString(), anyMap())).thenCallRealMethod()

        val result = csvImportServiceImplMock.saveFileToSample(submission, row)

        assertThat(result).isEmpty()
    }*/
}
