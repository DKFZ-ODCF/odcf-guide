package de.dkfz.odcf.guide.service.implementation.mail

import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helperObjects.mapDistinctAndNotNullOrBlank
import de.dkfz.odcf.guide.service.interfaces.UrlGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.hibernate.Hibernate
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class MailContentGeneratorServiceImpl(
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val sampleRepository: SampleRepository,
    private val collectorService: CollectorService,
    private val urlGeneratorService: UrlGeneratorService,
    private val env: Environment
) : MailContentGeneratorService {

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())
    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    override fun getTicketSubjectPrefix(submission: Submission): String {
        if (submission.ticketNumber.isNotEmpty()) {
            return "[" + env.getRequiredProperty("application.mails.ticketSystemPrefix") + submission.ticketNumber + "]" +
                "[" + collectorService.getFormattedIdentifier(submission.identifier) + "]"
        }
        return "[" + collectorService.getFormattedIdentifier(submission.identifier) + "]"
    }

    override fun getTicketSubject(submission: Submission, messageKey: String): String {
        return mailBundle.getString(messageKey).replace("{0}", getTicketSubjectPrefix(submission))
    }

    override fun getMailBody(key: String, values: Map<String, String>): String {
        var body = mailBundle.getString(key)
        values.forEach { body = body.replace(it.key, it.value) }
        return body
    }

    override fun mailBodyReceivedSubmission(submission: Submission): String {
        val textSelector = if (submission is ApiSubmission) { "receivedSubmissionMailBody" } else { "uploadedSubmissionMailBody" }
        return getMailBody(
            "mailService.$textSelector",
            mapOf(
                "{0}" to submission.submitter.fullName,
                "{1}" to urlGeneratorService.getURL(submission),
                "{2}" to env.getRequiredProperty("application.serverUrl"),
            )
        )
    }

    override fun mailBodyFasttrackImported(submission: Submission): String {
        return mailBundle.getString("mailService.fasttrackBody")
            .replace("{0}", collectorService.getFormattedIdentifier(submission.identifier))
            .replace("{1}", submission.originProjects)
    }

    override fun getOpenSubmissionReminderMailSubject(submission: Submission): String {
        return getTicketSubjectPrefix(submission) + " Please validate your open submission"
    }

    override fun getOpenSubmissionReminderMailBody(submission: Submission): String {
        val projects = sampleRepository.findAllBySubmission(submission).mapDistinctAndNotNullOrBlank { it.project }
        val projectPrefix = if (projects.size> 1) "projects" else "project"

        return mailBundle.getString("mailService.openSubmissionFirstReminderMailBody")
            .replace("{0}", submission.submitter.fullName)
            .replace("{1}", "$projectPrefix ${projects.joinToString()} (${collectorService.getFormattedIdentifier(submission.identifier)})")
            .replace("{2}", urlGeneratorService.getURL(submission))
            .replace("{3}", env.getRequiredProperty("application.serverUrl"))
    }

    override fun getFinallySubmittedMailBody(submission: Submission, filePaths: List<String>): String {
        val body = mailBundle.getString("mailService.finallySubmittedMailBody")
            .replace("{identifier}", collectorService.getFormattedIdentifier(submission.identifier))
            .replace("{closedUser}", submission.closedUser ?: "[automatic]")

        if (submission.isExtended) {
            if (filePaths.isEmpty()) {
                return body.replace("{fragment_extendedSubmission}", bundle.getString("export.errorWritingOutFile"))
            }

            val otpImportLink = runtimeOptionsRepository.findByName("otpImportLink")?.value.orEmpty()
                .replace("TICKET_NUMBER", submission.ticketNumber)
                .replace("FILE_PATH", filePaths.first())

            return body.replace("{fragment_extendedSubmission}", mailBundle.getString("mailService.finallySubmittedMailBody.extendedSubmission"))
                .replace("{fragment_multipleProjects}", if (filePaths.size > 1) "${mailBundle.getString("mailService.finallySubmittedMailBody.multipleProjects")}\n" else "")
                .replace("{projects}", sampleRepository.findAllBySubmission(submission).mapDistinctAndNotNullOrBlank { it.project }.sorted().joinToString())
                .replace("{filePath}", filePaths.joinToString("\n"))
                .replace("{otpImportLink}", otpImportLink)
        }
        return body.replace("\n{fragment_extendedSubmission}\n", "")
    }

    override fun getFinishedExternallyMailBody(submission: Submission): String {
        val sb = StringBuilder(
            "Dear ODCF service,\n" +
                "\n" +
                "metadata for [${collectorService.getFormattedIdentifier(submission.identifier)}] has been reported to be finished externally."
        )
        sb.append("\n\nBest regards,\nODCF Validation Service")
        return sb.toString()
    }

    @Throws(java.lang.IllegalArgumentException::class)
    override fun getFinallySubmittedMailSubject(submission: Submission): String {
        return when (submission.status) {
            Submission.Status.CLOSED -> {
                getTicketSubjectPrefix(submission) + " Submission has been validated"
            }
            Submission.Status.AUTO_CLOSED -> {
                getTicketSubjectPrefix(submission) + " Submission has been auto-closed"
            } else -> {
                throw IllegalArgumentException("Status must be CLOSED or AUTO_CLOSED in order to send FinallySubmitted mails!")
            }
        }
    }

    override fun getFinishedExternallyMailSubject(submission: Submission): String {
        return getTicketSubjectPrefix(submission) + " Submission has been finished externally"
    }

    override fun getProcessingStatusUpdateBody(jobs: List<ClusterJob>): String {
        val submission = jobs.first().submission
        val samples = sampleRepository.findAllBySubmissionAndProceedNot(submission, Sample.Proceed.NO)
        return mailBundle.getString("lsfService.processingStatusUpdateBody")
            .replace("{0}", jobs.joinToString("\n") { "${it.printableName}: ${it.state.name}" })
            .replace("{1}", "${samples.size}")
            .replace("{2}", samples.joinToString("\n") { it.name })
            .replace("{3}", urlGeneratorService.getAdminURL(submission))
            .trim()
    }

    override fun getFinalProcessingStatusUpdateBody(job: ClusterJob): String {
        val submission = Hibernate.unproxy(job.submission) as Submission
        val samplesWithPaths = collectorService.getPathsWithSampleList(sampleRepository.findAllBySubmissionAndProceedNot(submission, Sample.Proceed.NO).groupBy { it.abstractSampleId }, submission)
        return mailBundle.getString("lsfService.finalProcessingStatusUpdateBody")
            .replace("{0}", submission.identifier)
            .replace("{1}", samplesWithPaths.map { "${it.key} -> samples [${it.value.joinToString { it.name }}]" }.joinToString("\n"))
            .replace("{2}", urlGeneratorService.getURL(submission))
            .trim()
    }

    override fun getTerminationReminderMailBody(submission: Submission): String {
        val terminationDate = submission.startTerminationPeriod!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(90)
        val formattedSubmissionIdentifier = collectorService.getFormattedIdentifier(submission.identifier)

        val mailBodySelector = if (submission.isApiSubmission) {
            "reminderService.terminationReminderMailBody"
        } else {
            "reminderService.extendedSubmissionTerminationReminderMailBody"
        }

        return mailBundle.getString(mailBodySelector)
            .replace("{0}", submission.submitter.fullName)
            .replace("{1}", formattedSubmissionIdentifier)
            .replace("{2}", urlGeneratorService.getURL(submission))
            .replace("{3}", terminationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
    }

    override fun getTerminationMailBody(submission: Submission): String {
        val formattedSubmissionIdentifier = collectorService.getFormattedIdentifier(submission.identifier)
        val mailBodySelector = if (submission.isApiSubmission) { "reminderService.terminationMailBody" } else { "reminderService.extendedSubmissionTerminationMailBody" }

        return mailBundle.getString(mailBodySelector)
            .replace("{0}", submission.submitter.fullName)
            .replace("{1}", formattedSubmissionIdentifier)
    }
}
