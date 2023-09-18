package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.UploadSubmission
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.TerminationService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*

@Service
class TerminationServiceImpl(
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val fileService: FileService,
    private val submissionService: SubmissionService,
    private val submissionRepository: SubmissionRepository,
    private val collectorService: CollectorService,
    private val deletionService: DeletionService,
) : TerminationService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun terminateSubmissionAfterReminder(submission: Submission) {
        if (submission is ApiSubmission) {
            mailSenderService.sendMailToSubmitter(
                subject = mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationMailSubject"),
                body = mailContentGeneratorService.getTerminationMailBody(submission),
                submitterMail = submission.submitter.mail
            )
            submissionService.changeSubmissionState(submission, Submission.Status.TERMINATED)
        } else if (submission is UploadSubmission) {
            try {
                val tsvContent = fileService.createLongTsvFile(
                    submission,
                    withImportIdentifier = false,
                    withExportNames = false
                )
                val attachment = fileService.convertStringToTSVFile(submission.identifier, tsvContent)

                mailSenderService.sendMailToSubmitterWithAttachment(
                    subject = mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationMailSubject"),
                    body = mailContentGeneratorService.getTerminationMailBody(submission),
                    submitterMail = submission.submitter.mail,
                    attachment = attachment
                )
                deletionService.deleteSubmission(submission, false)
            } catch (e: IOException) {
                logger.error(
                    "The submission ${submission.identifier} could not be terminated because there was an error when writing out the TSV File\n" +
                        e.stackTraceToString()
                )
            }
        }
    }

    override fun terminateSubmission(submission: Submission, subjectKey: String) {
        submissionService.changeSubmissionState(submission, Submission.Status.TERMINATED, null)
        val subject = try {
            mailContentGeneratorService.getTicketSubject(submission, "mailService.$subjectKey")
        } catch (e: Exception) {
            logger.warn(e.localizedMessage)
            mailContentGeneratorService.getTicketSubject(submission, "mailService.terminatedMailSubject")
        }
        mailSenderService.sendMailToTicketSystem(
            subject,
            mailContentGeneratorService.getMailBody(
                "mailService.terminatedMailBody",
                mapOf("{0}" to collectorService.getFormattedIdentifier(submission.identifier))
            )
        )
    }

    override fun resetSubmissionTerminationPeriod(submission: Submission) {
        submission.startTerminationPeriod = Date()
        submissionRepository.saveAndFlush(submission)
        mailSenderService.sendMailToTicketSystem(
            mailContentGeneratorService.getTicketSubject(submission, "reminderService.terminationResetMailSubject"),
            mailContentGeneratorService.getMailBody(
                "mailService.terminationResetMailBody",
                mapOf("{0}" to collectorService.getFormattedIdentifier(submission.identifier))
            )
        )
    }
}
