package de.dkfz.odcf.guide.service.interfaces.mail

import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.submissionData.Submission
import kotlin.jvm.Throws

interface MailContentGeneratorService {

    /**
     * Returns the subject prefix for mails for a given submission.
     *
     * @param submission Submission for which to get the ticket prefix for the mail subject.
     * @return the mail subject prefix in the style of `"[ODCF #Ticketnumber][S#SubmissionIdentifier]"`.
     */
    fun getTicketSubjectPrefix(submission: Submission): String

    @Throws(Exception::class)
    fun getTicketSubject(submission: Submission, messageKey: String): String

    fun getMailBody(key: String, values: Map<String, String>): String

    fun mailBodyReceivedSubmission(submission: Submission): String

    fun mailBodyFasttrackImported(submission: Submission): String

    fun getOpenSubmissionReminderMailSubject(submission: Submission): String

    fun getOpenSubmissionReminderMailBody(submission: Submission): String

    fun getFinallySubmittedMailBody(submission: Submission, filePaths: List<String>): String

    fun getFinishedExternallyMailBody(submission: Submission): String

    fun getFinallySubmittedMailSubject(submission: Submission): String

    fun getFinishedExternallyMailSubject(submission: Submission): String

    fun getProcessingStatusUpdateBody(jobs: List<ClusterJob>): String

    fun getFinalProcessingStatusUpdateBody(job: ClusterJob): String

    fun getTerminationReminderMailBody(submission: Submission): String

    fun getTerminationMailBody(submission: Submission): String
}
