package de.dkfz.odcf.guide.service.importer

import de.dkfz.odcf.guide.FileRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.service.implementation.importer.ImportServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.kotlin.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.jdbc.Sql
import javax.management.relation.RelationException
import javax.persistence.EntityManager

@SpringBootTest
class ImportServiceTests @Autowired constructor(private val importService: ImportService) : AnyObject {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var importServiceMock: ImportServiceImpl

    @Mock
    lateinit var fileRepository: FileRepository

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var jsonApiService: JsonApiService

    @Mock
    lateinit var entityManager: EntityManager

    @Mock
    lateinit var env: Environment

    @Test
    @Sql("/db/migration/V1.5/V1.5.19__createSequence.sql")
    fun `When generateInternalIdentifier then return Identifier`() {
        val identifier = importService.generateInternalIdentifier()

        assertThat(identifier).startsWith("o")
    }

    @Test
    fun `When generateIlseIdentifier then return Identifier`() {
        val ilse = 123456

        val identifier = importServiceMock.generateIlseIdentifier(ilse)

        assertThat(identifier).startsWith("i")
        assertThat(identifier).isEqualTo("i0$ilse")
    }

    @Test
    fun `When extractTagmentationLibraryFromSampleIdentifierIfNecessary without tagmentationLibrary is string null`() {
        val sampleIdentifier = "sampleIdentifier_lib3"

        val tagmentationLibrary = importServiceMock.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleIdentifier, "null")

        assertThat(tagmentationLibrary).isEqualTo("3")
    }

    @Test
    fun `When extractTagmentationLibraryFromSampleIdentifierIfNecessary without tagmentationLibrary is empty string`() {
        val sampleIdentifier = "sampleIdentifier_lib3"

        val tagmentationLibrary = importServiceMock.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleIdentifier, "")

        assertThat(tagmentationLibrary).isEqualTo("3")
    }

    @Test
    fun `When extractTagmentationLibraryFromSampleIdentifierIfNecessary with given tagmentationLibrary`() {
        val sampleIdentifier = "sampleIdentifier_lib3"

        val tagmentationLibrary = importServiceMock.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleIdentifier, "33")

        assertThat(tagmentationLibrary).isEqualTo("33")
    }

    @Test
    fun `When extractTagmentationLibraryFromSampleIdentifierIfNecessary with no parsable sampleIdentifier`() {
        val sampleIdentifier = "sampleIdentifier"

        val tagmentationLibrary = importServiceMock.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleIdentifier)

        assertThat(tagmentationLibrary).isEqualTo("")
    }

    @Test
    fun `When findByFileName don't differentiate between R1 R2 i1 i2 and return the same sample`() {
        val testFile = entityFactory.getFile()
        val filename = testFile.fileName.replace("_R1.fastq.gz", "")

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(fileRepository.findByFileNameLikeIgnoreCaseAndSample_Submission(anyString(), anySubmission())).thenReturn(listOf(testFile))

        val foundSample = importServiceMock.findSampleByFileName("${filename}_R2.fastq.gz", testFile.sample.submission)

        assertThat(testFile.sample.id).isEqualTo(foundSample!!.id)
    }

    @Test
    fun `When findByFileName with different filenames don't return the same sample`() {
        val testFile = entityFactory.getFile()

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption())

        val foundSample = importServiceMock.findSampleByFileName("ABC", testFile.sample.submission)

        assertThat(foundSample).isNull()
    }

    @Test
    fun `When findByFileName with different samples throw exception`() {
        val file = entityFactory.getFile()
        val fileDifferentSample = entityFactory.getFile()

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(fileRepository.findByFileNameLikeIgnoreCaseAndSample_Submission(anyString(), anySubmission())).thenReturn(listOf(file, fileDifferentSample))

        assertThatExceptionOfType(RelationException::class.java).isThrownBy {
            importServiceMock.findSampleByFileName(file.fileName, file.sample.submission)
        }.withMessage("Files have different samples.")
    }

    @Test
    fun `Check functionality findSampleByFile`() {
        val file = entityFactory.getFile()
        val submission = entityFactory.getApiSubmission()

        val foundSample = importServiceMock.findSampleByFile(file, submission)

        assertThat(foundSample).isEqualTo(file.sample)
    }

    @Test
    fun `Check functionality findSampleByFile with sample not initialized`() {
        val file = File()
        file.fileName = "fileName_R1.fastq"
        val submission = entityFactory.getApiSubmission()

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))

        val foundSample = importServiceMock.findSampleByFile(file, submission)

        assertThat(foundSample).isNull()
    }

    @Test
    fun `Test createTicket with no ticketSystemPath`() {
        val identifier = "identifier"
        val projects = listOf("p1", "p2")

        val result = importServiceMock.createTicket(identifier, projects)

        assertThat(result).isEmpty()
    }

    @Test
    fun `Test createTicket with no createTicketJson`() {
        val identifier = "identifier"
        val projects = listOf("p1", "p2")

        `when`(runtimeOptionsRepository.findByName("ticketSystemPath")).thenReturn(entityFactory.getRuntimeOption("PATH"))

        val result = importServiceMock.createTicket(identifier, projects)

        assertThat(result).isEmpty()
    }

    @Test
    fun `Test createTicket with valid response json`() {
        val identifier = "identifier"
        val projects = listOf("p1", "p2")
        val json = "{\"projects\":\"<PROJECTS>\",\"identifier\":\"<IDENTIFIER>\"}"
        val jsonReplaced = json.replace("<IDENTIFIER>", identifier).replace("<PROJECTS>", projects.sorted().joinToString())
        val ticketNumber = "123"

        `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
        `when`(runtimeOptionsRepository.findByName("ticketSystemPath")).thenReturn(entityFactory.getRuntimeOption("PATH"))
        `when`(runtimeOptionsRepository.findByName("createTicketJson")).thenReturn(entityFactory.getRuntimeOption(json))
        `when`(jsonApiService.postJsonToApi("PATH", emptyMap(), jsonReplaced, ApiType.OTRS)).thenReturn("{\"TicketNumber\":\"$ticketNumber\"}")

        val result = importServiceMock.createTicket(identifier, projects)

        assertThat(result).isEqualTo(ticketNumber)
    }

    @Test
    fun `Test createTicket with send mail false`() {
        val identifier = "identifier"
        val projects = listOf("p1", "p2")

        `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("false")

        val result = importServiceMock.createTicket(identifier, projects)

        assertThat(result).isEmpty()
    }

    @Test
    fun `Test createTicket with valid response json but empty project`() {
        val identifier = "identifier"
        val json = "{\"projects\":\"<PROJECTS>\",\"identifier\":\"<IDENTIFIER>\"}"
        val jsonReplaced = json.replace("<IDENTIFIER>", identifier).replace("<PROJECTS>", "No project")
        val ticketNumber = "123"

        `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
        `when`(runtimeOptionsRepository.findByName("ticketSystemPath")).thenReturn(entityFactory.getRuntimeOption("PATH"))
        `when`(runtimeOptionsRepository.findByName("createTicketJson")).thenReturn(entityFactory.getRuntimeOption(json))
        `when`(jsonApiService.postJsonToApi("PATH", emptyMap(), jsonReplaced, ApiType.OTRS)).thenReturn("{\"TicketNumber\":\"$ticketNumber\"}")

        val result = importServiceMock.createTicket(identifier, emptyList())

        assertThat(result).isEqualTo(ticketNumber)
        verify(jsonApiService, times(1)).postJsonToApi("PATH", emptyMap(), jsonReplaced, ApiType.OTRS)
    }

    @Test
    fun `Test createTicket with invalid response json`() {
        val identifier = "identifier"
        val projects = listOf("p1", "p2")
        val json = "{\"projects\":\"<PROJECTS>\",\"identifier\":\"<IDENTIFIER>\"}"
        val jsonReplaced = json.replace("<IDENTIFIER>", identifier)
            .replace("<PROJECTS>", projects.sorted().joinToString())

        `when`(runtimeOptionsRepository.findByName("ticketSystemPath")).thenReturn(entityFactory.getRuntimeOption("PATH"))
        `when`(runtimeOptionsRepository.findByName("createTicketJson")).thenReturn(entityFactory.getRuntimeOption(json))
        `when`(jsonApiService.postJsonToApi("PATH", emptyMap(), jsonReplaced, ApiType.OTRS)).thenReturn("{\"TicketNumber\":}")

        val result = importServiceMock.createTicket(identifier, projects)

        assertThat(result).isEmpty()
    }

    @Test
    fun `Check functionality findFastqFilePairs`() {
        val file = entityFactory.getFile()
        val file2 = entityFactory.getFile()
        file2.fileName = "fileName_R2.fastq.gz"
        val submission = entityFactory.getApiSubmission()

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(fileRepository.findByFileNameLikeIgnoreCaseAndSample_Submission(anyString(), anySubmission())).thenReturn(listOf(file, file2))

        val fastqFilePairs = importServiceMock.findFastqFilePairs(file.fileName, submission)

        assertThat(fastqFilePairs.size).isEqualTo(2)
        assertThat(fastqFilePairs).isEqualTo(listOf(file, file2))
    }

    @Test
    fun `Check functionality findFastqFilePairs find no pair`() {
        val file = entityFactory.getFile()
        val file2 = entityFactory.getFile()
        file2.fileName = "otherFileName_R2.fastq.gz"
        val submission = entityFactory.getApiSubmission()

        `when`(runtimeOptionsRepository.findByName("fastqFileSuffix")).thenReturn(entityFactory.getRuntimeOption("_[R|I][1|2]\\.fastq\\.gz"))
        `when`(fileRepository.findByFileNameLikeIgnoreCaseAndSample_Submission(anyString(), anySubmission())).thenReturn(listOf(file))

        val fastqFilePairs = importServiceMock.findFastqFilePairs(file.fileName, submission)

        assertThat(fastqFilePairs.size).isEqualTo(1)
        assertThat(fastqFilePairs).isEqualTo(listOf(file))
    }
}
