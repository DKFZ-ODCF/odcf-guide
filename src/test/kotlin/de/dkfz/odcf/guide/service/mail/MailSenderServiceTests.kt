package de.dkfz.odcf.guide.service.mail

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.helperObjects.mapDistinctAndNotNullOrBlank
import de.dkfz.odcf.guide.service.implementation.mail.MailContentGeneratorServiceImpl
import de.dkfz.odcf.guide.service.implementation.mail.MailSenderServiceImpl
import de.dkfz.odcf.guide.service.interfaces.UrlGeneratorService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.mail.util.MimeMessageParser
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.kotlin.times
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.util.*
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.SendFailedException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@ExtendWith(SpringExtension::class)
class MailSenderServiceTests : AnyObject {

    private val entityFactory = EntityFactory()

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @InjectMocks
    @Spy
    lateinit var mailSenderServiceMock: MailSenderServiceImpl

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorServiceImpl

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var urlGeneratorService: UrlGeneratorService

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var env: Environment

    @Mock
    lateinit var sender: JavaMailSender

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(MailSenderServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    private fun initMailAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger("mail-log") as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `test sending a mail all valid`(): Unit = runBlocking {
        launch {
            val mimeMessage = MimeMessage(null as Session?)
            val listAppender = initListAppender()
            val mailAppender = initMailAppender()

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")

            mailSenderServiceMock.sendMail(
                from = "a.b@c.de",
                to = "e.d@c.ba",
                cc = "e.d@cb.a",
                replyTo = "a.b@c.de",
                subject = "testmail",
                messageText = "mail test"
            )
            delay(200)

            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )
            verify(sender, times(1)).send(emailCaptor.capture())

            val mailLogsList = mailAppender.list
            assertThat(mailLogsList).hasSize(2)
            assertThat(mailLogsList.first().level).isEqualTo(Level.INFO)
            assertThat(mailLogsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "FROM: 'ODCF Guide <a.b@c.de>'\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "CC: '[e.d@cb.a]'\n" +
                    "REPLY_TO: 'a.b@c.de'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "MESSAGE: 'mail test'\n"
            )
            assertThat(mailLogsList.last().message).isEqualTo("### Mail sent successfully. ###")
            val logsList = listAppender.list
            assertThat(logsList).hasSize(1)
            assertThat(logsList.first().level).isEqualTo(Level.INFO)
            assertThat(logsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "SUBJECT: 'testmail'\n"
            )
        }
    }

    @Test
    fun `test sending a mail all valid with attachment`(): Unit = runBlocking {
        launch {
            val mimeMessage = MimeMessage(null as Session?)
            val listAppender = initListAppender()
            val mailAppender = initMailAppender()

            val attachment = File("testData/file.tsv")
            val attachmentContent = "File content"
            attachment.deleteOnExit()
            attachment.writeText(attachmentContent)

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")

            mailSenderServiceMock.sendMail(
                from = "a.b@c.de",
                to = "e.d@c.ba",
                cc = "e.d@cb.a",
                replyTo = "a.b@c.de",
                subject = "testmail",
                messageText = "mail test",
                attachment = attachment,
                deleteAttachmentAfterSending = false
            )
            delay(200)

            val emailCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)
            verify(sender, times(1)).send(emailCaptor.capture())
            val sentEmail = emailCaptor.value

            // Test that file is attached
            val multipart = sentEmail.content as Multipart
            val attachmentPart = multipart.getBodyPart(1)
            assertThat(attachmentPart.disposition).isEqualTo(Part.ATTACHMENT)
            assertThat(attachmentPart.fileName).isEqualTo("file.tsv")

            // Test that file content is correct
            val attachmentBytes = attachmentPart.inputStream.readAllBytes()
            val attachmentContentActual = String(attachmentBytes)
            assertThat(attachmentContentActual).isEqualTo(attachmentContent)

            val mailLogsList = mailAppender.list
            assertThat(mailLogsList).hasSize(2)
            assertThat(mailLogsList.first().level).isEqualTo(Level.INFO)
            assertThat(mailLogsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "FROM: 'ODCF Guide <a.b@c.de>'\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "CC: '[e.d@cb.a]'\n" +
                    "REPLY_TO: 'a.b@c.de'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "MESSAGE: 'mail test'\n" +
                    "ATTACHMENT: 'file.tsv'\n"
            )
            assertThat(mailLogsList.last().message).isEqualTo("### Mail sent successfully. ###")
            val logsList = listAppender.list
            assertThat(logsList).hasSize(1)
            assertThat(logsList.first().level).isEqualTo(Level.INFO)
            assertThat(logsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "WITH 1 ATTACHMENT\n"
            )
        }
    }

    @Test
    fun `test sending a mail with attachment and deleting the file afterwards`(): Unit = runBlocking {
        launch {
            val mimeMessage = MimeMessage(null as Session?)
            val listAppender = initListAppender()
            val mailAppender = initMailAppender()

            val attachment = File("testData/file.tsv")
            val attachmentContent = "File content"
            attachment.deleteOnExit()
            attachment.writeText(attachmentContent)
            val spyAttachment = spy(attachment)

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")

            mailSenderServiceMock.sendMail(
                from = "a.b@c.de",
                to = "e.d@c.ba",
                cc = "e.d@cb.a",
                replyTo = "a.b@c.de",
                subject = "testmail",
                messageText = "mail test",
                attachment = spyAttachment,
                deleteAttachmentAfterSending = true
            )
            delay(200)

            val emailCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)
            verify(sender, times(1)).send(emailCaptor.capture())
            val sentEmail = emailCaptor.value

            // Test that file is attached
            val multipart = sentEmail.content as Multipart
            val attachmentPart = multipart.getBodyPart(1)
            assertThat(attachmentPart.disposition).isEqualTo(Part.ATTACHMENT)
            assertThat(attachmentPart.fileName).isEqualTo("file.tsv")

            val mailLogsList = mailAppender.list
            assertThat(mailLogsList).hasSize(2)
            assertThat(mailLogsList.first().level).isEqualTo(Level.INFO)
            assertThat(mailLogsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "FROM: 'ODCF Guide <a.b@c.de>'\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "CC: '[e.d@cb.a]'\n" +
                    "REPLY_TO: 'a.b@c.de'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "MESSAGE: 'mail test'\n" +
                    "ATTACHMENT: 'file.tsv'\n"
            )
            assertThat(mailLogsList.last().message).isEqualTo("### Mail sent successfully. ###")
            val logsList = listAppender.list
            assertThat(logsList).hasSize(3)
            assertThat(logsList.first().level).isEqualTo(logsList.last().level).isEqualTo(Level.INFO)
            assertThat(logsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "WITH 1 ATTACHMENT\n"
            )
            assertThat(logsList[1].message).isEqualTo("### Attempting to delete attachment ${attachment.name} ###")
            verify(spyAttachment, times(1)).delete()
            assertThat(logsList.last().message).isEqualTo("### Attachment deleted successfully. ###")
        }
    }

    @Test
    fun `test sending a mail with attachment but deleting the file encounters error`(): Unit = runBlocking {
        launch {
            val mimeMessage = MimeMessage(null as Session?)
            val listAppender = initListAppender()
            val mailAppender = initMailAppender()

            val attachment = File("testData/file.tsv")
            val attachmentContent = "File content"
            attachment.deleteOnExit()
            attachment.writeText(attachmentContent)
            val spyAttachment = spy(attachment)

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(spyAttachment.delete()).thenReturn(false)

            mailSenderServiceMock.sendMail(
                from = "a.b@c.de",
                to = "e.d@c.ba",
                cc = "e.d@cb.a",
                replyTo = "a.b@c.de",
                subject = "testmail",
                messageText = "mail test",
                attachment = spyAttachment,
                deleteAttachmentAfterSending = true
            )
            delay(200)

            val emailCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)
            verify(sender, times(1)).send(emailCaptor.capture())
            val sentEmail = emailCaptor.value

            // Test that file is attached
            val multipart = sentEmail.content as Multipart
            val attachmentPart = multipart.getBodyPart(1)
            assertThat(attachmentPart.disposition).isEqualTo(Part.ATTACHMENT)
            assertThat(attachmentPart.fileName).isEqualTo("file.tsv")

            val mailLogsList = mailAppender.list
            assertThat(mailLogsList).hasSize(2)
            assertThat(mailLogsList.first().level).isEqualTo(Level.INFO)
            assertThat(mailLogsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "FROM: 'ODCF Guide <a.b@c.de>'\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "CC: '[e.d@cb.a]'\n" +
                    "REPLY_TO: 'a.b@c.de'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "MESSAGE: 'mail test'\n" +
                    "ATTACHMENT: 'file.tsv'\n"
            )
            assertThat(mailLogsList.last().message).isEqualTo("### Mail sent successfully. ###")
            val logsList = listAppender.list
            assertThat(logsList).hasSize(3)
            assertThat(logsList.first().level).isEqualTo(Level.INFO)
            assertThat(logsList.last().level).isEqualTo(Level.ERROR)
            assertThat(logsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "WITH 1 ATTACHMENT\n"
            )
            assertThat(logsList[1].message).isEqualTo("### Attempting to delete attachment ${attachment.name} ###")
            assertThat(logsList.last().message).isEqualTo("### Error while attempting to delete attachment. ###")
        }
    }

    @Test
    fun `test sending a mail with attachment but attachment file no longer exists for deletion`(): Unit = runBlocking {
        launch {
            val mimeMessage = MimeMessage(null as Session?)
            val listAppender = initListAppender()
            val mailAppender = initMailAppender()

            val attachment = File("testData/file.tsv")
            val attachmentContent = "File content"
            attachment.deleteOnExit()
            attachment.writeText(attachmentContent)
            val spyAttachment = spy(attachment)

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")

            mailSenderServiceMock.sendMail(
                from = "a.b@c.de",
                to = "e.d@c.ba",
                cc = "e.d@cb.a",
                replyTo = "a.b@c.de",
                subject = "testmail",
                messageText = "mail test",
                attachment = spyAttachment,
                deleteAttachmentAfterSending = true
            )
            // deleting the attachment to cause an error during the attempted deletion from the function
            attachment.delete()
            delay(200)

            val emailCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)
            verify(sender, times(1)).send(emailCaptor.capture())
            val sentEmail = emailCaptor.value

            // Test that file is attached
            val multipart = sentEmail.content as Multipart
            val attachmentPart = multipart.getBodyPart(1)
            assertThat(attachmentPart.disposition).isEqualTo(Part.ATTACHMENT)
            assertThat(attachmentPart.fileName).isEqualTo("file.tsv")

            val mailLogsList = mailAppender.list
            assertThat(mailLogsList).hasSize(2)
            assertThat(mailLogsList.first().level).isEqualTo(Level.INFO)
            assertThat(mailLogsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "FROM: 'ODCF Guide <a.b@c.de>'\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "CC: '[e.d@cb.a]'\n" +
                    "REPLY_TO: 'a.b@c.de'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "MESSAGE: 'mail test'\n" +
                    "ATTACHMENT: 'file.tsv'\n"
            )
            assertThat(mailLogsList.last().message).isEqualTo("### Mail sent successfully. ###")
            val logsList = listAppender.list
            assertThat(logsList).hasSize(3)
            assertThat(logsList.first().level).isEqualTo(Level.INFO)
            assertThat(logsList.last().level).isEqualTo(Level.ERROR)
            assertThat(logsList.first().message).isEqualTo(
                "### Mail sending triggered ###\n" +
                    "TO: '[e.d@c.ba]'\n" +
                    "SUBJECT: 'testmail'\n" +
                    "WITH 1 ATTACHMENT\n"
            )
            assertThat(logsList[1].message).isEqualTo("### Attempting to delete attachment ${attachment.name} ###")
            assertThat(logsList.last().message).isEqualTo("### Error while attempting to delete attachment. ###")
        }
    }

    @Test
    fun `test sending a mail all valid empty cc`(): Unit = runBlocking {
        launch {
            val mimeMessage = MimeMessage(null as Session?)

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")

            mailSenderServiceMock.sendMail(
                from = "a.b@c.de",
                to = "e.d@c.ba",
                cc = "",
                replyTo = "a.b@c.de",
                subject = "testmail",
                messageText = "mail test"
            )

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )
            verify(sender, times(1)).send(emailCaptor.capture())
        }
    }

    @Test
    fun `test sending a mail all valid without reply to`(): Unit = runBlocking {
        launch {
            val mimeMessage = MimeMessage(null as Session?)

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("h.h@h.hh")

            mailSenderServiceMock.sendMail(
                from = "a.b@c.de",
                to = "e.d@c.ba",
                cc = "",
                subject = "testmail",
                messageText = "mail test"
            )

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <h.h@h.hh>")
        }
    }

    @Test
    fun `test filter invalid address with invalid sender address`() {
        val mimeMessage = MimeMessage(null as Session?)
        val mailSendException = MailSendException(mapOf(mimeMessage to SendFailedException("553")))

        Assertions.assertThatExceptionOfType(GuideRuntimeException::class.java).isThrownBy {
            mailSenderServiceMock.filterInvalidAddress(mailSendException)
        }.withMessage("Error while sending mail. Sender address is invalid.")
    }

    @Test
    fun `test filter invalid address with invalid to address`() {
        val mimeMessage = MimeMessage(null as Session?)
        val message = MimeMessageHelper(mimeMessage, "utf-8")
        message.setFrom("a.b@c.de")
        message.setTo("e.d@c.ba")
        val sendFailedException =
            SendFailedException("", Exception(), emptyArray(), emptyArray(), arrayOf(InternetAddress()))
        val exi = MailSendException(mapOf(mimeMessage to sendFailedException))

        mailSenderServiceMock.filterInvalidAddress(exi)

        verify(sender, times(1)).send(any(MimeMessage::class.java))
    }

    @Test
    fun `test filter invalid address with invalid sender and receiver address`() {
        val listAppender = initListAppender()
        val mimeMessage = MimeMessage(null as Session?)
        val message = MimeMessageHelper(mimeMessage, "utf-8")
        message.setFrom("a.b@c.de")
        message.setTo("e.d@c.ba")
        message.setCc("f.g@h.i")
        val sendFailedException = SendFailedException("", Exception(), emptyArray(), emptyArray(), arrayOf(InternetAddress("e.d@c.ba"), InternetAddress("f.g@h.i")))
        val exi = MailSendException(mapOf(mimeMessage to sendFailedException))

        mailSenderServiceMock.filterInvalidAddress(exi)

        verify(sender, times(0)).send(any(MimeMessage::class.java))
        val logsList = listAppender.list
        assertThat(logsList).hasSize(2)
        assertThat(logsList.first().level).isEqualTo(Level.ERROR)
        assertThat(logsList.first().message).isEqualTo("invalid addresses: [e.d@c.ba, f.g@h.i]")
        assertThat(logsList.last().message).isEqualTo("Error while sending mail. All addresses are invalid.")
    }

    @Test
    fun `Check functionality sendMailToSubmitter`() {
        val senderAddress = "ticketsystem@h.hh"
        val submitterAddress = "submitter@s.ss"

        `when`(env.getRequiredProperty("application.mails.submitterMails")).thenReturn("true")
        `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn(senderAddress)
        `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn(senderAddress)

        mailSenderServiceMock.sendMailToSubmitter("subject", "body", submitterAddress)

        verify(mailSenderServiceMock, times(1)).sendMail(senderAddress, submitterAddress, senderAddress, "subject", "body")
    }

    @Test
    fun `Check functionality sendMailToSubmitterWithAttachment`() {
        val senderAddress = "ticketsystem@h.hh"
        val submitterAddress = "submitter@s.ss"

        val attachment = File("testData/long.tsv")

        `when`(env.getRequiredProperty("application.mails.submitterMails")).thenReturn("true")
        `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn(senderAddress)
        `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn(senderAddress)

        mailSenderServiceMock.sendMailToSubmitterWithAttachment("subject", "body", submitterAddress, attachment)

        verify(mailSenderServiceMock, times(1)).sendMail(
            senderAddress,
            submitterAddress,
            senderAddress,
            "ODCF Service <$senderAddress>",
            "subject",
            "body",
            attachment
        )
    }

    @Test
    fun `test sending on hold reminder mail`(): Unit = runBlocking {
        launch {
            val submissions = setOf(entityFactory.getApiSubmission(Submission.Status.ON_HOLD, ""))
            val mimeMessage = MimeMessage(null as Session?)
            val subject = mailBundle.getString("mailService.onHoldReminderSubject")
            val content = mailBundle.getString("mailService.onHoldReminderContent")
                .replace("{0}", "https://url")
                .replace("{1}", submissions.first().identifier)
                .replace("{2}", "No on-hold reason was given")
            val body = mailBundle.getString("mailService.onHoldReminderBody").replace("{0}", content).replace("\n", "<br>")

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(urlGeneratorService.getAdminURL(submissions.first())).thenReturn("https://url")
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")

            mailSenderServiceMock.sendOnHoldReminderMail(submissions)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo("ticketsystem@h.hh")
            assertThat(MimeMessageParser(mimeMessage).parse().htmlContent).contains(body)
        }
    }

    @Test
    fun `Check functionality sendReopenSubmissionMail`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission()
            val identifier = submission.identifier.replace("i", "S#")
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix] Submission has been reopened"
            val body = "Dear ODCF service,<br><br>Submission [$identifier] has been reopened in GUIDE.<br><br>Kind regards,<br>ODCF Team"

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(identifier)

            mailSenderServiceMock.sendReopenSubmissionMail(submission)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo("ticketsystem@h.hh")
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <ticketsystem@h.hh>")
            assertThat(mimeMessage.from[0].toString()).isEqualTo("ODCF Guide <ticketsystem@h.hh>")
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
            assertThat(MimeMessageParser(mimeMessage).parse().htmlContent).contains(body)
        }
    }

    @Test
    fun `Check functionality sendFinishedExternallyMail`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission()
            val identifier = submission.identifier.replace("i", "S#")
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix] Submission has been finished externally"
            val body = "Dear ODCF service,<br><br>metadata for [$identifier] has been reported to be finished externally.<br><br>Best regards,<br>ODCF Validation Service"

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(identifier)
            `when`(mailContentGeneratorService.getFinishedExternallyMailBody(submission)).thenReturn(body)
            `when`(mailContentGeneratorService.getFinishedExternallyMailSubject(submission)).thenCallRealMethod()

            mailSenderServiceMock.sendFinishedExternallyMail(submission)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo("ticketsystem@h.hh")
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <ticketsystem@h.hh>")
            assertThat(mimeMessage.from[0].toString()).isEqualTo("ODCF Guide <ticketsystem@h.hh>")
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
            assertThat(MimeMessageParser(mimeMessage).parse().htmlContent).contains(body)
        }
    }

    @Test
    fun `Check functionality sendReceivedSubmissionMail`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission()
            submission.submitter = entityFactory.getPerson()
            val sample1 = entityFactory.getSample(submission)
            val sample2 = entityFactory.getSample(submission)
            sample2.project = "otherProject"
            val projects = listOf(sample2, sample1).mapDistinctAndNotNullOrBlank { it.project }.sorted().joinToString()
            val identifier = submission.identifier.replace("i", "S#")
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix] Transferred metadata table to ODCF validation service - $projects"
            val body = "Submitter {0}, URL {1}, Base URL {3}"

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(env.getRequiredProperty("application.mails.submitterMails")).thenReturn("true")
            `when`(env.getRequiredProperty("application.serverUrl")).thenReturn("url/")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(identifier)
            `when`(mailContentGeneratorService.mailBodyReceivedSubmission(submission)).thenReturn(body)
            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample1, sample2))

            mailSenderServiceMock.sendReceivedSubmissionMail(submission)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            verify(mailSenderServiceMock, times(0)).sendMailToTicketSystem(subject, body)
            verify(mailSenderServiceMock, times(1)).sendMailToAllSubmissionMembers(subject, body, submission)
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo(submission.submitter.mail)
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <ticketsystem@h.hh>")
            assertThat(mimeMessage.from[0].toString()).isEqualTo("ODCF Guide <ticketsystem@h.hh>")
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
            assertThat(MimeMessageParser(mimeMessage).parse().htmlContent).contains(body)
        }
    }

    @Test
    fun `Check functionality sendReceivedSubmissionMail with sendToUser false`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission()
            submission.submitter = entityFactory.getPerson()
            val sample = entityFactory.getSample(submission)
            val identifier = submission.identifier.replace("i", "S#")
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix] Transferred metadata table to ODCF validation service - ${sample.project}"
            val body = "Submitter {0}, URL {1}, Base URL {3}"

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(env.getRequiredProperty("application.mails.submitterMails")).thenReturn("true")
            `when`(env.getRequiredProperty("application.serverUrl")).thenReturn("url/")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(identifier)
            `when`(mailContentGeneratorService.mailBodyReceivedSubmission(submission)).thenReturn(body)
            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

            mailSenderServiceMock.sendReceivedSubmissionMail(submission, false)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)

            verify(mailSenderServiceMock, times(1)).sendMailToTicketSystem(subject, body)
            verify(mailSenderServiceMock, times(0)).sendMailToAllSubmissionMembers(subject, body, submission)
            verify(sender, times(1)).send(emailCaptor.capture())
        }
    }

    @Test
    fun `Check functionality sendMailFasttrackImported`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission()
            val identifier = submission.identifier.replace("i", "S#")
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix]Fasttrack submission found"
            val body = "Submission {0} belonging to the project(s) {1} is marked as fasttrack. The notifications in OTP should be turned off."

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(identifier)
            `when`(mailContentGeneratorService.mailBodyFasttrackImported(submission)).thenReturn(body)

            mailSenderServiceMock.sendMailFasttrackImported(submission)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo("ticketsystem@h.hh")
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <ticketsystem@h.hh>")
            assertThat(mimeMessage.from[0].toString()).isEqualTo("ODCF Guide <ticketsystem@h.hh>")
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
            assertThat(MimeMessageParser(mimeMessage).parse().htmlContent).contains(body)
        }
    }

    @Test
    fun `Check functionality sendMailsUploadedSubmission`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getUploadSubmission()
            val sample = entityFactory.getSample(submission)
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix] Transferred metadata table to ODCF validation service - ${sample.project}"
            val body = "Submitter {0}, URL {1}, Base URL {3}"

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(urlGeneratorService.getURL(submission)).thenReturn("url/submission")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(mailContentGeneratorService.mailBodyReceivedSubmission(submission)).thenReturn(body)
            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

            mailSenderServiceMock.sendReceivedSubmissionMail(submission, false)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo("ticketsystem@h.hh")
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <ticketsystem@h.hh>")
            assertThat(mimeMessage.from[0].toString()).isEqualTo("ODCF Guide <ticketsystem@h.hh>")
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
        }
    }

    @Test
    fun `Check functionality sendMailsUploadedSubmission with isAdmin false`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getUploadSubmission()
            submission.submitter = entityFactory.getPerson()
            val sample = entityFactory.getSample(submission)
            val subject = "[prefix] Transferred metadata table to ODCF validation service - ${sample.project}"
            val body = "Submitter {0}, URL {1}, Base URL {3}"

            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(urlGeneratorService.getURL(submission)).thenReturn("url/submission")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(mailContentGeneratorService.mailBodyReceivedSubmission(submission)).thenReturn(body)
            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

            mailSenderServiceMock.sendReceivedSubmissionMail(submission, true)

            verify(mailSenderServiceMock, times(1)).sendMailToAllSubmissionMembers(subject, body, submission)
        }
    }

    @Test
    fun `Check functionality sendFinallySubmittedMail`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "")
            val identifier = submission.identifier.replace("i", "S#")
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix] Submission has been validated"

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(urlGeneratorService.getURL(submission)).thenReturn("url/submission")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(identifier)
            `when`(mailContentGeneratorService.getFinallySubmittedMailSubject(submission)).thenCallRealMethod()
            `when`(mailContentGeneratorService.getFinallySubmittedMailBody(submission, emptyList())).thenReturn("body")

            mailSenderServiceMock.sendFinallySubmittedMail(submission)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(
                MimeMessage::class.java
            )

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo("ticketsystem@h.hh")
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <ticketsystem@h.hh>")
            assertThat(mimeMessage.from[0].toString()).isEqualTo("ODCF Guide <ticketsystem@h.hh>")
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
        }
    }

    @Test
    fun `Check functionality sendFinallySubmittedMail with includeSubmissionReceived true`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "")
            val identifier = submission.identifier.replace("i", "S#")
            val mimeMessage = MimeMessage(null as Session?)
            val subject = "[prefix] Submission has been validated"

            `when`(sender.createMimeMessage()).thenReturn(mimeMessage)
            `when`(env.getRequiredProperty("application.mails.sendmail")).thenReturn("true")
            `when`(urlGeneratorService.getURL(submission)).thenReturn("url/submission")
            `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("ticketsystem@h.hh")
            `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("ticketsystem@h.hh")
            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(identifier)
            `when`(mailContentGeneratorService.getFinallySubmittedMailSubject(submission)).thenCallRealMethod()

            mailSenderServiceMock.sendFinallySubmittedMail(submission, listOf("filepath"), true)

            delay(200)
            val emailCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)

            verify(sender, times(1)).send(emailCaptor.capture())
            assertThat(mimeMessage.allRecipients[0].toString()).isEqualTo("ticketsystem@h.hh")
            assertThat(mimeMessage.replyTo[0].toString()).isEqualTo("ODCF Service <ticketsystem@h.hh>")
            assertThat(mimeMessage.from[0].toString()).isEqualTo("ODCF Guide <ticketsystem@h.hh>")
            assertThat(mimeMessage.subject.toString()).isEqualTo(subject)
            verify(mailContentGeneratorService, times(1)).getFinallySubmittedMailBody(submission, listOf("filepath"))
            verify(mailContentGeneratorService, times(1)).mailBodyReceivedSubmission(submission)
            assertThat(MimeMessageParser(mimeMessage).parse().htmlContent).contains("null<br><br>${"#".repeat(60)}<br><br>null")
        }
    }
}
