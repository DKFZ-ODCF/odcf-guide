package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.Submission.TerminationState.NOTIFIED
import de.dkfz.odcf.guide.entity.submissionData.Submission.TerminationState.PREVENT_TERMINATION
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.ReminderServiceImpl
import de.dkfz.odcf.guide.service.interfaces.TerminationService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.ArgumentMatchers.anyList
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@SpringBootTest
class ReminderServiceTests : AnyObject {

    private val entityFactory = EntityFactory()

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @InjectMocks
    lateinit var reminderServiceMock: ReminderServiceImpl

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var environment: Environment

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var terminationService: TerminationService

    @Test
    fun `test sending terminate reminder mail for API submission`() {
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getApiSubmission()
        submission.submitter = submitter
        val activeStates = Submission.Status.values().filter { it.group == "active" }
        val date74Days = LocalDate.now().minusDays(74)

        `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn("true")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationReminderMailSubject")).thenReturn("Subject")
        `when`(mailContentGeneratorService.getTerminationReminderMailBody(submission)).thenReturn("Body")
        `when`(
            submissionRepository.findAllByStartTerminationPeriodIsBeforeAndStatusInAndTerminationStateNotIn(
                Date.from(date74Days.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                activeStates,
                listOf(PREVENT_TERMINATION, NOTIFIED)
            )
        ).thenReturn(setOf(submission))

        reminderServiceMock.sendTerminateReminderMails()

        verify(mailSenderService, times(1)).sendMailToSubmitter(
            subject = "Subject",
            body = "Body",
            submitterMail = submitter.mail
        )
        assertThat(submission.terminationState).isEqualTo(NOTIFIED)
    }

    @Test
    fun `test termination and sending mail`() {
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getApiSubmission()
        val uploadSubmission = entityFactory.getUploadSubmission()
        submission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(100).atStartOfDay(ZoneId.systemDefault()).toInstant())
        uploadSubmission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(100).atStartOfDay(ZoneId.systemDefault()).toInstant())
        submission.submitter = submitter
        uploadSubmission.submitter = submitter
        val activeStates = Submission.Status.values().filter { it.group == "active" }
        val date89Days = LocalDate.now().minusDays(89)

        `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn("true")
        `when`(
            submissionRepository.findAllByStartTerminationPeriodIsBeforeAndStatusInAndTerminationStateNotIn(
                Date.from(date89Days.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                activeStates,
                listOf(PREVENT_TERMINATION)
            )
        ).thenReturn(setOf(submission, uploadSubmission))

        reminderServiceMock.sendTerminateReminderMails()

        verify(terminationService, times(1)).terminateSubmissionAfterReminder(submission)
        verify(terminationService, times(1)).terminateSubmissionAfterReminder(uploadSubmission)
    }

    @Test
    fun `test sending terminate reminder mail for uploadSubmission`() {
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getUploadSubmission()
        submission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(80).atStartOfDay(ZoneId.systemDefault()).toInstant())
        submission.submitter = submitter
        val terminationDateSubmission = submission.startTerminationPeriod!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(90)
        val activeStates = Submission.Status.values().filter { it.group == "active" }
        val date74Days = LocalDate.now().minusDays(74)

        `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn("true")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationReminderMailSubject")).thenReturn("Subject")
        `when`(mailContentGeneratorService.getTerminationReminderMailBody(submission)).thenReturn("Body")
        `when`(
            submissionRepository.findAllByStartTerminationPeriodIsBeforeAndStatusInAndTerminationStateNotIn(
                Date.from(date74Days.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                activeStates,
                listOf(PREVENT_TERMINATION, NOTIFIED)
            )
        ).thenReturn(setOf(submission))

        reminderServiceMock.sendTerminateReminderMails()

        verify(mailSenderService, times(1)).sendMailToSubmitter(
            subject = "Subject",
            body = "Body",
            submitterMail = submitter.mail
        )
        assertThat(submission.terminationState).isEqualTo(NOTIFIED)
    }

    @Test
    fun `test not sending mail when send mail is off`() {
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getApiSubmission()
        submission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(100).atStartOfDay(ZoneId.systemDefault()).toInstant())
        submission.submitter = submitter

        `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn("false")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationReminderMailSubject")).thenReturn("Subject")
        `when`(mailContentGeneratorService.getTerminationReminderMailBody(submission)).thenReturn(mailBundle.getString("reminderService.terminationReminderMailBody"))

        reminderServiceMock.sendTerminateReminderMails()

        verify(mailSenderService, times(0)).sendMailToSubmitter(
            subject = "Subject",
            body = mailBundle.getString("reminderService.terminationMailBody")
                .replace("{0}", submitter.fullName)
                .replace("{1}", submission.identifier),
            submitterMail = submitter.mail
        )
    }

    @Test
    fun `test not sending mail when terminationPeriod isn't over yet`() {
        val submitter = entityFactory.getPerson()
        val submission = entityFactory.getApiSubmission()
        submission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(20).atStartOfDay(ZoneId.systemDefault()).toInstant())
        submission.submitter = submitter

        `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn("true")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationReminderMailSubject")).thenReturn("Subject")
        `when`(mailContentGeneratorService.getTerminationReminderMailBody(submission)).thenReturn(mailBundle.getString("reminderService.terminationReminderMailBody"))

        reminderServiceMock.sendTerminateReminderMails()

        verify(mailSenderService, times(0)).sendMailToSubmitter(
            subject = "Subject",
            body = mailBundle.getString("reminderService.terminationMailBody")
                .replace("{0}", submitter.fullName)
                .replace("{1}", submission.identifier),
            submitterMail = submitter.mail
        )
    }

    @Test
    fun `get correct submissions to be notified`() {
        val activeSubmission = entityFactory.getApiSubmission(Submission.Status.IMPORTED, "")
        activeSubmission.externalDataAvailableForMerging = true
        activeSubmission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(75).atStartOfDay(ZoneId.systemDefault()).toInstant())
        val onHoldSubmission = entityFactory.getApiSubmission(Submission.Status.ON_HOLD, "")
        val submissions = listOf(activeSubmission, onHoldSubmission)

        `when`(submissionRepository.findAllByStatusIn(anyList())).thenReturn(submissions)
        `when`(environment.getProperty("application.mails.reminders.hoursSinceMetadataAvailable", "48")).thenReturn("72")

        val submissionsToBeNotified = reminderServiceMock.getSubmissionsToBeNotified()

        assertThat(submissionsToBeNotified).isEqualTo(listOf(activeSubmission))
    }

    @TestFactory
    fun `test send on-hold reminder mail`() = listOf(
        "true" to 1,
        "false" to 0
    ).map { (sendmail, expected) ->
        DynamicTest.dynamicTest("When sendmail '$sendmail' then send $expected OnHoldReminderMail") {
            val onHoldSubmission = entityFactory.getApiSubmission(Submission.Status.ON_HOLD, "")
            val activeSubmission = entityFactory.getApiSubmission(Submission.Status.IMPORTED, "")
            val submissions = listOf(onHoldSubmission, activeSubmission)

            `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn(sendmail)
            `when`(submissionRepository.findAll()).thenReturn(submissions)

            reminderServiceMock.sendOnHoldReminderMail()

            verify(mailSenderService, times(expected)).sendOnHoldReminderMail(setOf(onHoldSubmission))
            verify(mailSenderService, times(0)).sendOnHoldReminderMail(submissions.toSet())
        }
    }

    @TestFactory
    fun `check functionality sendDataReceivedReminderMail`() = listOf(
        "true" to 1,
        "false" to 0
    ).map { (sendmail, expected) ->
        DynamicTest.dynamicTest("When sendmail '$sendmail' then send $expected DataReceivedReminderMails") {
            val submission = entityFactory.getApiSubmission()

            `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn(sendmail)
            `when`(mailContentGeneratorService.getOpenSubmissionReminderMailSubject(submission)).thenReturn("subject")
            `when`(mailContentGeneratorService.getOpenSubmissionReminderMailBody(submission)).thenReturn("body")

            reminderServiceMock.sendDataReceivedReminderMail(submission)

            verify(mailSenderService, times(expected)).sendMailToAllSubmissionMembers("subject", "body", submission)
        }
    }

    @TestFactory
    fun `check functionality sendReminderMails`() = listOf(
        "true" to 1,
        "false" to 0
    ).map { (sendmail, expected) ->
        DynamicTest.dynamicTest("When sendmail '$sendmail' then send $expected sendReminderMail") {
            val activeSubmission = entityFactory.getApiSubmission(Submission.Status.IMPORTED, "")
            activeSubmission.externalDataAvailableForMerging = true
            activeSubmission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(75).atStartOfDay(ZoneId.systemDefault()).toInstant())
            val onHoldSubmission = entityFactory.getApiSubmission(Submission.Status.ON_HOLD, "")
            val submissions = listOf(activeSubmission, onHoldSubmission)

            `when`(submissionRepository.findAllByStatusIn(anyList())).thenReturn(submissions)
            `when`(environment.getProperty("application.mails.reminders.hoursSinceMetadataAvailable", "48")).thenReturn("72")
            `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn(sendmail)
            `when`(mailContentGeneratorService.getOpenSubmissionReminderMailSubject(anySubmission())).thenReturn("subject")
            `when`(mailContentGeneratorService.getOpenSubmissionReminderMailBody(anySubmission())).thenReturn("body")

            reminderServiceMock.sendReminderMails()

            verify(mailSenderService, times(expected)).sendMailToAllSubmissionMembers("subject", "body", activeSubmission)
            verify(mailSenderService, times(0)).sendMailToAllSubmissionMembers("subject", "body", onHoldSubmission)
        }
    }

    @Test
    fun `check sendReminderMails with UploadSubmission`() {
        val submission = entityFactory.getUploadSubmission()
        submission.submitter = entityFactory.getPerson()

        `when`(submissionRepository.findAllByStatusIn(anyList())).thenReturn(listOf(submission))
        `when`(environment.getProperty("application.mails.reminders.hoursSinceMetadataAvailable", "48")).thenReturn("72")
        `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn("true")
        `when`(mailContentGeneratorService.getOpenSubmissionReminderMailSubject(anySubmission())).thenReturn("subject")
        `when`(mailContentGeneratorService.getOpenSubmissionReminderMailBody(anySubmission())).thenReturn("body")

        reminderServiceMock.sendReminderMails()

        verify(mailSenderService, times(1)).sendMailToSubmitter("subject", "body", submission.submitter.mail)
    }

    @Test
    fun `test do not send empty on-hold reminder mail`() {
        val submissions = listOf(entityFactory.getApiSubmission(Submission.Status.IMPORTED, ""))

        `when`(environment.getRequiredProperty("application.mails.reminders.sendmail")).thenReturn("true")
        `when`(submissionRepository.findAll()).thenReturn(submissions)

        reminderServiceMock.sendOnHoldReminderMail()

        verify(mailSenderService, times(0)).sendOnHoldReminderMail(submissions.toSet())
    }
}
