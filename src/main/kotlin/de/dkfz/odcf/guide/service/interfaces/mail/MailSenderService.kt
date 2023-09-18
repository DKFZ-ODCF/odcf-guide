package de.dkfz.odcf.guide.service.interfaces.mail

import de.dkfz.odcf.guide.entity.submissionData.Submission
import java.io.File

interface MailSenderService {

    /**
     * Sends a mail to the submitter or ticket system when the GUIDE receives the metadata for a submission.
     * Contains the URLs necessary to complete the submission in the GUIDE.
     *
     * @param submission Submission object for which the metadata has been received
     * @param sendToUser send mail to user if true or to ticket system if false
     */
    fun sendReceivedSubmissionMail(submission: Submission, sendToUser: Boolean = true)

    /**
     * Sends a mail if the property for the sending of mails (`application.mails.sendmail`) is set to `true`
     * in the YML file for the run configurations.
     *
     * Also records the sent mail in the log file.
     *
     * @param from Sender of the mail
     * @param to Recipient of the mail
     * @param cc Secondary recipient(s) of the mail
     * @param subject Subject of the mail
     * @param messageText Content of the mail
     */
    fun sendMail(from: String, to: String, cc: String, subject: String, messageText: String)

    /**
     * Sends a mail with a ReplyTo mail address if the property for the sending of mails (`application.mails.sendmail`)
     * is set to `true` in the YML file for the run configurations.
     *
     * Also records the sent mail in the log file.
     *
     * @param from Sender of the mail
     * @param to Recipient of the mail
     * @param cc Secondary recipient(s) of the mail
     * @param replyTo Mail address that should be used for the Reply
     * @param subject Subject of the mail
     * @param messageText Content of the mail
     * @param attachment An optional file attachment
     * @param deleteAttachmentAfterSending Whether to delete the attachment after sending the mail (necessary for testing)
     */
    fun sendMail(from: String, to: String, cc: String, replyTo: String, subject: String, messageText: String, attachment: File? = null, deleteAttachmentAfterSending: Boolean = true)

    /**
     * Sends a mail to the ticket system with no secondary recipient(s) of the mail.
     *
     * @param subject Subject of the mail
     * @param body Content of the mail
     */
    fun sendMailToTicketSystem(subject: String, body: String)

    /**
     * Sends a mail to the submitter as well as the ticket system
     * if the property for the sending of mails to the submitter (`application.mails.submitterMails`)
     * is set to `true` in the YML file for the run configurations.
     *
     * @param subject Subject of the mail
     * @param body Content of the mail
     * @param submitterMail Mail address of the recipient of the mail
     */
    fun sendMailToSubmitter(subject: String, body: String, submitterMail: String)

    /**
     * Sends a mail with an attachment to the submitter as well as the ticket system
     * if the property for the sending of mails to the submitter (`application.mails.submitterMails`)
     * is set to `true` in the YML file for the run configurations.
     *
     * @param subject Subject of the mail
     * @param body Content of the mail
     * @param submitterMail Mail address of the recipient of the mail
     * @param attachment attachment for the mail
     */
    fun sendMailToSubmitterWithAttachment(subject: String, body: String, submitterMail: String, attachment: File)

    /**
     * Sends a mail to the submitter as well as all other members of all containing projects
     *
     * @param subject Subject of the mail
     * @param body Content of the mail
     * @param submission Submission belongs to the mail
     */
    fun sendMailToAllSubmissionMembers(subject: String, body: String, submission: Submission)

    /**
     * Sends a mail to the ticket system when a submission is finished and has been finally submitted.
     *
     * If the submission is extended, checks whether the filePath exists
     * and puts the filePath (or filePaths in the case of multiple projects) in the body of the mail.
     *
     * Otherwise, if the submission is not auto-closed, writes the command for merging with the GPCF metadata file in the body of the mail,
     * with the notice to choose the correct jsonExtracted file in the case of multiple files existing.
     *
     * @param submission Submission that has been finally submitted.
     * @param filePaths Paths to the metadata (tsv) files of the submission.
     * @param includeSubmissionReceived Whether or not to append text of submissionReceived mail to this mail
     *
     */
    fun sendFinallySubmittedMail(submission: Submission, filePaths: List<String> = emptyList(), includeSubmissionReceived: Boolean = false)

    /**
     * Sends a mail to the ticket system when a submission has been finished externally.
     *
     * @param submission Submission that has been finished externally.
     */
    fun sendFinishedExternallyMail(submission: Submission)

    /**
     * Sends a mail to the ticket system when a submission has been reopened.
     *
     * @param submission Submission that has been reopened.
     */
    fun sendReopenSubmissionMail(submission: Submission)

    fun sendMailFasttrackImported(submission: Submission)

    /**
     * Sends a mail to the ticket system to remind the operators that there are still submissions that were put on hold.
     *
     * @param submissions Set of all the submissions with the state On-Hold
     */
    fun sendOnHoldReminderMail(submissions: Set<Submission>)
}
