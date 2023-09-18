package de.dkfz.odcf.guide.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.TerminationServiceImpl
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.TerminationService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.io.IOException
import java.util.*

@SpringBootTest
class TerminationServiceTests @Autowired constructor(private val terminationService: TerminationService) {

    private val entityFactory = EntityFactory()

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @InjectMocks
    lateinit var terminationServiceMock: TerminationServiceImpl

    @Mock
    lateinit var env: Environment

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var fileService: FileService

    @Mock
    lateinit var submissionService: SubmissionService

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var deletionService: DeletionService

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(TerminationServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `check functionality terminateSubmissionAfterReminder for ApiSubmission`() {
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getApiSubmission()
        submission.submitter = submitter

        `when`(env.getRequiredProperty("application.mails.submitterMails")).thenReturn("true")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationMailSubject")).thenReturn("terminationMailSubject")
        `when`(mailContentGeneratorService.getTerminationMailBody(submission)).thenReturn("terminationMailBody")

        terminationServiceMock.terminateSubmissionAfterReminder(submission)

        verify(mailSenderService, times(1)).sendMailToSubmitter(
            subject = "terminationMailSubject",
            body = "terminationMailBody",
            submitterMail = submitter.mail
        )
        verify(submissionService, times(1)).changeSubmissionState(submission, Submission.Status.TERMINATED)
    }

    @Test
    fun `check functionality terminateSubmissionAfterReminder for UploadSubmission`() {
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getUploadSubmission()
        submission.submitter = submitter

        `when`(env.getRequiredProperty("application.mails.submitterMails")).thenReturn("true")
        `when`(fileService.createLongTsvFile(submission, false, false)).thenReturn("TSV Content")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationMailSubject")).thenReturn("terminationMailSubject")
        `when`(mailContentGeneratorService.getTerminationMailBody(submission)).thenReturn("terminationMailBody")

        terminationServiceMock.terminateSubmissionAfterReminder(submission)

        verify(mailSenderService, times(1)).sendMailToSubmitterWithAttachment(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        verify(deletionService, times(1)).deleteSubmission(submission, false)
    }

    @Test
    fun `check functionality terminateExternalSubmission throws Exception`() {
        val listAppender = initListAppender()
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getUploadSubmission()
        submission.submitter = submitter

        `when`(env.getRequiredProperty("application.mails.submitterMails")).thenReturn("true")
        `when`(fileService.createLongTsvFile(submission, false, false)).thenReturn("TSV Content")
        `when`(fileService.convertStringToTSVFile(submission.identifier, "TSV Content")).thenThrow(IOException::class.java)

        terminationServiceMock.terminateSubmissionAfterReminder(submission)
        val logsList = listAppender.list

        verify(mailSenderService, times(0)).sendMailToSubmitterWithAttachment(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        verify(submissionService, times(0)).changeSubmissionState(submission, Submission.Status.REMOVED_BY_ADMIN)
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.ERROR)
        assertThat(logsList.first().message).startsWith(
            "The submission ${submission.identifier} could not be terminated because there was an error when writing out the TSV File"
        )
    }

    @Test
    fun `check submission termination`() {
        val submission = entityFactory.getApiSubmission()

        `when`(submissionService.changeSubmissionState(submission, Submission.Status.TERMINATED, null)).doAnswer {
            submission.status = Submission.Status.TERMINATED
        }

        terminationServiceMock.terminateSubmission(submission, "terminatedMailSubjectGUI")

        assertThat(submission.status).isEqualTo(Submission.Status.TERMINATED)
    }

    @Test
    fun `check submission termination with a wrong subject key should not fail`() {
        val submission = entityFactory.getApiSubmission()
        val listAppender = initListAppender()

        `when`(mailContentGeneratorService.getTicketSubject(submission, "mailService.wrongKey")).thenThrow(Exception("Can't find resource for bundle java.util.PropertyResourceBundle, key mailService.wrongKey"))

        terminationServiceMock.terminateSubmission(submission, "wrongKey")
        val logsList = listAppender.list

        verify(submissionService, times(1)).changeSubmissionState(submission, Submission.Status.TERMINATED, null)
        assertThat(logsList).hasSize(1)
        assertThat(logsList.last().level).isEqualTo(Level.WARN)
        assertThat(logsList.last().message).isEqualTo("Can't find resource for bundle java.util.PropertyResourceBundle, key mailService.wrongKey")
    }

    @Test
    fun `check resetSubmissionTerminationPeriod functionality`() {
        val submission = entityFactory.getApiSubmission()

        terminationServiceMock.resetSubmissionTerminationPeriod(submission)

        assertThat(submission.startTerminationPeriod).isNotNull
        Mockito.verify(mailSenderService, Mockito.times(0)).sendMail(
            from = anyString(),
            to = anyString(),
            cc = anyString(),
            replyTo = anyString(),
            subject = anyString(),
            messageText = anyString(),
            attachment = any(),
            deleteAttachmentAfterSending = anyBoolean()
        )
    }
}
