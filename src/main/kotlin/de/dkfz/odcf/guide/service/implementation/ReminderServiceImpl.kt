package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.Submission.Status
import de.dkfz.odcf.guide.entity.submissionData.Submission.Status.ON_HOLD
import de.dkfz.odcf.guide.entity.submissionData.Submission.TerminationState.NOTIFIED
import de.dkfz.odcf.guide.entity.submissionData.Submission.TerminationState.PREVENT_TERMINATION
import de.dkfz.odcf.guide.entity.submissionData.UploadSubmission
import de.dkfz.odcf.guide.service.interfaces.ReminderService
import de.dkfz.odcf.guide.service.interfaces.TerminationService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Service
open class ReminderServiceImpl(
    private val submissionRepository: SubmissionRepository,
    private val environment: Environment,
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val terminationService: TerminationService,
) : ReminderService {

    override fun sendDataReceivedReminderMail(submission: Submission) {
        if (environment.getRequiredProperty("application.mails.reminders.sendmail").toBoolean()) {
            val subject = mailContentGeneratorService.getOpenSubmissionReminderMailSubject(submission)
            val body = mailContentGeneratorService.getOpenSubmissionReminderMailBody(submission)
            mailSenderService.sendMailToAllSubmissionMembers(subject, body, submission)
        }
    }

    /**
     * Sends reminder mails scheduled with `application.mails.reminders.cronExpression` for all active
     * submissions that are not `ON_HOLD` and for which the technical data from ILSe is available for merging.
     */
    @Scheduled(cron = "\${application.mails.reminders.cronExpression}") // 0 0 9 * * mon: At 09:00 on Monday
    open fun sendReminderMails() {
        if (environment.getRequiredProperty("application.mails.reminders.sendmail").toBoolean()) {
            getSubmissionsToBeNotified().forEach { submission: Submission ->
                val subject = mailContentGeneratorService.getOpenSubmissionReminderMailSubject(submission)
                val body = mailContentGeneratorService.getOpenSubmissionReminderMailBody(submission)
                if (submission is ApiSubmission) {
                    mailSenderService.sendMailToAllSubmissionMembers(subject, body, submission)
                } else {
                    mailSenderService.sendMailToSubmitter(subject, body, submission.submitter.mail)
                }
            }
        }
    }

    /**
     * Returns all active submissions that are not set on hold for which the technical data from ILSe is available for merging.
     *
     * Only returns the submissions for which the technical data has already been available for more than 72 hours.
     *
     * @return Set of submission objects for which the criteria apply
     */
    fun getSubmissionsToBeNotified(): List<Submission> {
        val submissions = submissionRepository.findAllByStatusIn(Status.filterByGroup("active").minus(ON_HOLD))
        val hoursSinceMetadataAvailable = environment.getProperty("application.mails.reminders.hoursSinceMetadataAvailable", "48").toInt()
        return submissions.filter {
            (it is UploadSubmission || (Date().time - (it.startTerminationPeriod?.time ?: Date().time)) / (60 * 60 * 1000) >= hoursSinceMetadataAvailable) &&
                !it.isNotifiedForTermination
        }
    }

    /**
     * Checks if submissions are older than 74 days to notify users about the upcoming termination.
     * If they are older than 89 days they will be terminated automatically.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(rollbackFor = [Exception::class])
    open fun sendTerminateReminderMails() {
        if (environment.getRequiredProperty("application.mails.reminders.sendmail").toBoolean()) {
            val activeStates = Status.filterByGroup("active")
            val date74Days = LocalDate.now().minusDays(74)
            val date89Days = LocalDate.now().minusDays(89)

            var date = Date.from(date89Days.atStartOfDay(ZoneId.systemDefault()).toInstant())
            submissionRepository.findAllByStartTerminationPeriodIsBeforeAndStatusInAndTerminationStateNotIn(date, activeStates, listOf(PREVENT_TERMINATION)).forEach { submission: Submission ->
                terminationService.terminateSubmissionAfterReminder(submission)
            }

            date = Date.from(date74Days.atStartOfDay(ZoneId.systemDefault()).toInstant())
            submissionRepository.findAllByStartTerminationPeriodIsBeforeAndStatusInAndTerminationStateNotIn(date, activeStates, listOf(PREVENT_TERMINATION, NOTIFIED)).forEach { submission: Submission ->
                mailSenderService.sendMailToSubmitter(
                    subject = mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationReminderMailSubject"),
                    body = mailContentGeneratorService.getTerminationReminderMailBody(submission),
                    submitterMail = submission.submitter.mail
                )
                submission.terminationState = NOTIFIED
                submissionRepository.save(submission)
            }
        }
    }

    /**
     * Sends a reminder mail scheduled at 10:00 am on mondays for all submissions that have the state `OnHold`.
     */
    @Scheduled(cron = "0 0 10 * * mon") // 0 0 10 * * mon: At 10:00 on Monday
    open fun sendOnHoldReminderMail() {
        if (environment.getRequiredProperty("application.mails.reminders.sendmail").toBoolean()) {
            val onHoldSubmissions = submissionRepository.findAll().filter { it.isOnHold }.toSet()
            if (onHoldSubmissions.isNotEmpty()) {
                mailSenderService.sendOnHoldReminderMail(onHoldSubmissions)
            }
        }
    }
}
