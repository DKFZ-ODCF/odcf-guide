package de.dkfz.odcf.guide.service.implementation.mail

import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.service.interfaces.UrlGeneratorService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.Message
import javax.mail.SendFailedException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Service
class MailSenderServiceImpl(
    private var sender: JavaMailSender,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val collectorService: CollectorService,
    private val urlGeneratorService: UrlGeneratorService,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val env: Environment
) : MailSenderService {

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val mailLogger = LoggerFactory.getLogger("mail-log")

    override fun sendMail(from: String, to: String, cc: String, subject: String, messageText: String) {
        sendMail(from, to, cc, "ODCF Service <${env.getRequiredProperty("application.mails.ticketSystemAddress")}>", subject, messageText)
    }

    override fun sendMail(
        from: String,
        to: String,
        cc: String,
        replyTo: String,
        subject: String,
        messageText: String,
        attachment: File?,
        deleteAttachmentAfterSending: Boolean
    ) {
        GlobalScope.launch {
            if (env.getRequiredProperty("application.mails.sendmail").toBoolean()) {
                val mimeMessage: MimeMessage = sender.createMimeMessage()
                val message = MimeMessageHelper(mimeMessage, true, "UTF-8")

                message.setFrom(InternetAddress("ODCF Guide <$from>"))
                message.setTo(to)
                if (cc.isNotEmpty()) {
                    message.setCc(cc.split(";").toTypedArray())
                }
                message.setReplyTo(InternetAddress(replyTo))
                message.setSubject(subject)

                val htmlText = ClassPathResource("static/mail_template.html").inputStream
                    .bufferedReader().use { it.readText() }
                    .replace("[MESSAGE]", messageText.replace("\n", "<br>"))

                message.setText(htmlText, true)
                val currentDate = SimpleDateFormat("Mdd").format(Date()).toInt()
                val file = if (currentDate in 1206..1230) {
                    ClassPathResource("static/images/logo-guide-mail-christmas.png")
                } else {
                    ClassPathResource("static/images/logo-guide-mail.png")
                }
                message.addInline("guideLogo", file)

                attachment?.let { message.addAttachment(it.name, it) }

                logger.info(
                    "### Mail sending triggered ###\n" +
                        "TO: '[$to]'\n" +
                        "SUBJECT: '$subject'\n" +
                        ("WITH 1 ATTACHMENT\n".takeIf { attachment != null } ?: "")
                )
                mailLogger.info(
                    "### Mail sending triggered ###\n" +
                        "FROM: 'ODCF Guide <$from>'\n" +
                        "TO: '[$to]'\n" +
                        "CC: '[$cc]'\n" +
                        "REPLY_TO: '$replyTo'\n" +
                        "SUBJECT: '$subject'\n" +
                        "MESSAGE: '$messageText'\n" +
                        (attachment?.let { "ATTACHMENT: '${it.name}'\n" } ?: "")
                )
                try {
                    sender.send(mimeMessage)
                    mailLogger.info("### Mail sent successfully. ###")

                    if (deleteAttachmentAfterSending && attachment != null) {
                        logger.info("### Attempting to delete attachment ${attachment.name} ###")
                        if (attachment.delete()) {
                            logger.info("### Attachment deleted successfully. ###")
                        } else {
                            logger.error("### Error while attempting to delete attachment. ###")
                        }
                    }
                } catch (me: MailSendException) {
                    filterInvalidAddress(me)
                } catch (io: IOException) {
                    logger.error("### Exception while attempting to delete attachment. ###")
                } catch (e: Exception) {
                    logger.error("### Error while sending mail. ###\n ${e.stackTraceToString()}")
                    mailLogger.error("### Error while sending mail. ###\n ${e.stackTraceToString()}")
                }
            }
        }
    }

    fun filterInvalidAddress(me: MailSendException) {
        me.failedMessages.forEach {
            val sendFailedException = (it.value as SendFailedException)
            if (sendFailedException.message?.contains("553") == true) { // 553 -> Mailbox name invalid
                throw GuideRuntimeException("Error while sending mail. Sender address is invalid.")
            }
            val invalidAddresses = sendFailedException.invalidAddresses.toSet()
            logger.error("invalid addresses: $invalidAddresses")
            val message = it.key as MimeMessage
            val to = message.getRecipients(Message.RecipientType.TO)
            if (to != null) message.setRecipients(Message.RecipientType.TO, to.toMutableSet().minus(invalidAddresses).toTypedArray())
            val cc = message.getRecipients(Message.RecipientType.CC)
            if (cc != null) message.setRecipients(Message.RecipientType.CC, cc.toMutableSet().minus(invalidAddresses).toTypedArray())
            if (message.allRecipients.isNullOrEmpty()) {
                logger.error("Error while sending mail. All addresses are invalid.")
            } else {
                sender.send(message)
                logger.info("Send mail with removed invalid addresses.")
            }
        }
    }

    override fun sendMailToTicketSystem(subject: String, body: String) {
        sendMail(
            env.getRequiredProperty("application.mails.senderAddress"),
            env.getRequiredProperty("application.mails.ticketSystemAddress"),
            "",
            subject,
            body
        )
    }

    override fun sendMailToSubmitter(subject: String, body: String, submitterMail: String) {
        if (env.getRequiredProperty("application.mails.submitterMails").toBoolean()) {
            sendMail(
                env.getRequiredProperty("application.mails.senderAddress"),
                submitterMail,
                env.getRequiredProperty("application.mails.ticketSystemAddress"),
                subject,
                body
            )
        }
    }

    override fun sendMailToSubmitterWithAttachment(subject: String, body: String, submitterMail: String, attachment: File) {
        if (env.getRequiredProperty("application.mails.submitterMails").toBoolean()) {
            sendMail(
                from = env.getRequiredProperty("application.mails.senderAddress"),
                to = submitterMail,
                cc = env.getRequiredProperty("application.mails.ticketSystemAddress"),
                replyTo = "ODCF Service <${env.getRequiredProperty("application.mails.ticketSystemAddress")}>",
                subject = subject,
                messageText = body,
                attachment = attachment
            )
        }
    }

    override fun sendMailToAllSubmissionMembers(subject: String, body: String, submission: Submission) {
        val mailAddresses = submission.projects.flatMap { projectName ->
            externalMetadataSourceService.getSetOfValues("usersToBeNotifiedByProject", mapOf("project" to projectName))
        }.toSet().minus(submission.submitter.mail).plus(env.getRequiredProperty("application.mails.ticketSystemAddress"))

        if (env.getRequiredProperty("application.mails.submitterMails").toBoolean()) {
            sendMail(
                env.getRequiredProperty("application.mails.senderAddress"),
                submission.submitter.mail,
                mailAddresses.joinToString(";"),
                subject,
                body
            )
        }
    }

    override fun sendReceivedSubmissionMail(submission: Submission, sendToUser: Boolean) {
        val projects = submission.samples.map { it.project }.distinct().sorted().joinToString()
        val subject = mailContentGeneratorService.getTicketSubjectPrefix(submission) + " Transferred metadata table to ODCF validation service - $projects"
        if (sendToUser) {
            sendMailToSubmitter(subject, mailContentGeneratorService.mailBodyReceivedSubmission(submission), submission.submitter.mail)
        } else {
            sendMailToTicketSystem(subject, mailContentGeneratorService.mailBodyReceivedSubmission(submission))
        }
    }

    override fun sendMailFasttrackImported(submission: Submission) {
        val subject = mailContentGeneratorService.getTicketSubjectPrefix(submission) + mailBundle.getString("mailService.fasttrackSubject")
        sendMailToTicketSystem(subject, mailContentGeneratorService.mailBodyFasttrackImported(submission))
    }

    override fun sendFinishedExternallyMail(submission: Submission) {
        val mailBody = mailContentGeneratorService.getFinishedExternallyMailBody(submission)
        val subject = mailContentGeneratorService.getFinishedExternallyMailSubject(submission)
        sendMailToTicketSystem(subject, mailBody)
    }

    override fun sendFinallySubmittedMail(submission: Submission, filePaths: List<String>, includeSubmissionReceived: Boolean) {
        var body = mailContentGeneratorService.getFinallySubmittedMailBody(submission, filePaths)
        if (includeSubmissionReceived) {
            body += "\n\n${"#".repeat(60)}\n\n${mailContentGeneratorService.mailBodyReceivedSubmission(submission)}"
        }

        val subject: String
        try {
            subject = mailContentGeneratorService.getFinallySubmittedMailSubject(submission)
        } catch (e: IllegalArgumentException) {
            logger.warn(e.message)
            return
        }
        sendMailToTicketSystem(subject, body)
    }

    override fun sendReopenSubmissionMail(submission: Submission) {
        val subject = mailBundle.getString("mailService.reopenSubmissionMailSubject")
            .replace("{0}", mailContentGeneratorService.getTicketSubjectPrefix(submission))
        val mailBody = mailBundle.getString("mailService.reopenSubmissionMailBody")
            .replace("{0}", collectorService.getFormattedIdentifier(submission.identifier))
        sendMailToTicketSystem(subject, mailBody)
    }

    override fun sendOnHoldReminderMail(submissions: Set<Submission>) {
        val subject = mailBundle.getString("mailService.onHoldReminderSubject")
        val content = submissions.joinToString("\n") { submission ->
            mailBundle.getString("mailService.onHoldReminderContent")
                .replace("{0}", urlGeneratorService.getAdminURL(submission))
                .replace("{1}", submission.identifier)
                .replace("{2}", submission.onHoldComment.ifEmpty { "No on-hold reason was given" })
        }
        val body = mailBundle.getString("mailService.onHoldReminderBody").replace("{0}", content)
        sendMailToTicketSystem(subject, body)
    }
}
