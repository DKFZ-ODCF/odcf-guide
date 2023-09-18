package de.dkfz.odcf.guide.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideMergerException
import de.dkfz.odcf.guide.exceptions.JsonExtractorException
import de.dkfz.odcf.guide.exceptions.SubmissionNotFinishedException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.MergingServiceImpl
import de.dkfz.odcf.guide.service.interfaces.MergingService
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Spy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.io.IOException

@SpringBootTest
class MergingServiceTests @Autowired constructor(private val mergingService: MergingService) {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var mergingServiceMock: MergingServiceImpl

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Spy
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var ldapService: LdapService

    @Mock
    lateinit var rcs: RemoteCommandsService

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var env: Environment

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(MergingServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `merging is turned off`() {
        val listAppender = initListAppender()
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "o")

        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("false")

        mergingServiceMock.doMerging(submission)
        val logsList = listAppender.list

        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.INFO)
        assertThat(logsList.first().message).isEqualTo("Did not trigger merging for submission [${submission.identifier}] since merging has been turned off.")
    }

    @Test
    fun `jsonExtraction is turned off`() {
        val listAppender = initListAppender()
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")

        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("false")
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")

        val jsonExtractorScript = mergingServiceMock.runJsonExtractorScript(submission)
        val logsList = listAppender.list

        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.INFO)
        assertThat(logsList.first().message).isEqualTo("Did not trigger json extraction for submission [${submission.identifier}] since merging has been turned off.")
        assertThat(jsonExtractorScript).startsWith("Did not trigger")
    }

    @Test
    fun `submission identifier does not start with i`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "o")

        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            mergingServiceMock.doMerging(submission)
        }.withMessage("Identifier does not start with 'i'")
    }

    @Test
    fun `submission not finished throws exception`() {
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(SubmissionNotFinishedException::class.java).isThrownBy {
            mergingServiceMock.doMerging(entityFactory.getApiSubmission(Submission.Status.EDITED, "i"))
        }.withMessage("Submission is not finished! Merging not possible!")
    }

    @Test
    fun `submission not finished but onlyRunJsonExtractor set to true does not throw exception`() {
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")
        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")

        assertThatCode {
            mergingServiceMock.doMerging(entityFactory.getApiSubmission(Submission.Status.EDITED, "i"), true)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `metadata json extractor script path not found throws exception`() {
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(JsonExtractorException::class.java).isThrownBy {
            mergingServiceMock.doMerging(entityFactory.getApiSubmission(Submission.Status.CLOSED, "i"))
        }.withMessage("Path to json extractor script not found.")
    }

    @Test
    fun `metadata file path not found throws exception`() {
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")

        assertThatExceptionOfType(JsonExtractorException::class.java).isThrownBy {
            mergingServiceMock.doMerging(entityFactory.getApiSubmission(Submission.Status.CLOSED, "i"))
        }.withMessage("No metadata file path found.")
    }

    @Test
    fun `json extractor run`() {
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("run")

        val response = mergingServiceMock.runJsonExtractorScript(entityFactory.getApiSubmission(Submission.Status.EDITED, "i"))

        assertThat(response).isEqualTo("run")
    }

    @Test
    fun `json extractor exception is handled correctly`() {
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("ERROR occured during execution of metadata json extractor script")
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(JsonExtractorException::class.java).isThrownBy {
            mergingServiceMock.doMerging(entityFactory.getApiSubmission(Submission.Status.CLOSED, "i"))
        }.withMessage("ERROR occured during execution of metadata json extractor script")
    }

    @Test
    fun `rcs IO exception in json extractor call is handled correctly`() {
        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenThrow(IOException::class.java)
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(JsonExtractorException::class.java).isThrownBy {
            mergingServiceMock.runJsonExtractorScript(entityFactory.getApiSubmission(Submission.Status.CLOSED, "i"))
        }.withMessage("")
    }

    @Test
    fun `rcs IO exception in guide joiner call is handled correctly`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")

        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(runtimeOptionsRepository.findByName("guideJoinerScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataGuideJoiner.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataGuideJoiner.sh"))).thenThrow(IOException::class.java)
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            mergingServiceMock.doMerging(submission)
        }.withMessage("")
    }

    @Test
    fun `guide merge exit code is handled correctly`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")

        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(runtimeOptionsRepository.findByName("guideJoinerScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataGuideJoiner.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataGuideJoiner.sh"))).thenReturn("RscriptExitCode:4")
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            mergingServiceMock.doMerging(submission)
        }.withMessageStartingWith("\nAutomated merging for submission with ILSe ID [${collectorService.getFormattedIdentifier(submission.identifier)}] has been tried,")
    }

    @Test
    fun `guide merge error message is handled correctly`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")

        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(runtimeOptionsRepository.findByName("guideJoinerScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataGuideJoiner.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataGuideJoiner.sh"))).thenReturn("ERROR occured while executing GuideJoiner script")
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            mergingServiceMock.doMerging(submission)
        }.withMessageStartingWith("\nAutomated merging for submission with ILSe ID [${collectorService.getFormattedIdentifier(submission.identifier)}] has been tried,")
    }

    @Test
    fun `guide merge throws exception when guide joiner script not found`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")

        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataGuideJoiner.sh"))).thenReturn("Output written to file: /path/to/guideMergedFile")
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatExceptionOfType(GuideMergerException::class.java).isThrownBy {
            mergingServiceMock.doMerging(submission)
        }.withMessageStartingWith("Path to guide joiner script not found.")
    }

    @Test
    fun `guide merge runs fine`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")

        `when`(runtimeOptionsRepository.findByName("metadataFilePath")).thenReturn(entityFactory.getRuntimeOption("/omics/gpcf/midterm/{1}/data/{2}_meta.tsv"))
        `when`(runtimeOptionsRepository.findByName("jsonExtractorScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataJsonExtractor.sh"))
        `when`(runtimeOptionsRepository.findByName("guideJoinerScript")).thenReturn(entityFactory.getRuntimeOption("/home/icgcdata/skripte/metadataGuideJoiner.sh"))
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataJsonExtractor.sh"))).thenReturn("/path/to/jsonExtractFile")
        `when`(rcs.getFromRemote(startsWith("/home/icgcdata/skripte/metadataGuideJoiner.sh"))).thenReturn("Output written to file: /path/to/guideMergedFile")
        `when`(env.getProperty("application.mergingService.doMerging", "false")).thenReturn("true")

        assertThatCode { mergingServiceMock.doMerging(submission) }.doesNotThrowAnyException()
    }
}
