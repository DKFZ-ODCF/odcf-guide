package de.dkfz.odcf.guide.service.mail

import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.mail.MailContentGeneratorServiceImpl
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.UrlGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@SpringBootTest
class MailContentGeneratorServiceTests @Autowired constructor(private val mailContentGeneratorService: MailContentGeneratorService) : AnyObject {

    private val entityFactory = EntityFactory()

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @InjectMocks
    lateinit var mailContentGeneratorServiceMock: MailContentGeneratorServiceImpl

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var urlGeneratorService: UrlGeneratorService

    @Mock
    lateinit var fileService: FileService

    @Mock
    lateinit var env: Environment

    @Test
    fun `get valid finally submitted mail body for api submission`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")
        submission.identifier = "i1234569"
        submission.closedUser = "closedUser"

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        val result = mailContentGeneratorServiceMock.getFinallySubmittedMailBody(submission, listOf("/path"))

        assertThat(result).isEqualTo(
            """Dear ODCF service,
            |
            |validation of metadata for [${submission.identifier}] has been finished and finally submitted by ${submission.closedUser}.
            |
            |Kind regards,
            |ODCF Team""".trimMargin()
        )
    }

    @TestFactory
    fun `get valid mail body with existing metadata file for uploaded submission`() = listOf(
        listOf("/path") to "The metadata can be found here:",
        listOf("/path1", "/path2") to "This submission points to multiple projects: project1, project2\nThe metadata can be found here:"
    ).map { (filePaths, expected) ->
        DynamicTest.dynamicTest("test with path(s) $filePaths should contain '$expected'") {
            val submission = entityFactory.getUploadSubmission(Submission.Status.CLOSED)
            submission.identifier = "o1234569"
            val sample1 = entityFactory.getSample(submission)
            sample1.project = "project1"
            entityFactory.getTechnicalSample(sample1)
            val sample2 = entityFactory.getSample(submission)
            sample2.project = "project2"
            entityFactory.getTechnicalSample(sample2)
            submission.samples = listOf(sample1, sample2)
            submission.closedUser = "closedUser"

            `when`(fileService.fileExists(ArgumentMatchers.anyString())).thenReturn(true)
            `when`(runtimeOptionsRepository.findByName("otpImportLink")).thenReturn(entityFactory.getRuntimeOption("http://LINK?ticketNumber=TICKET_NUMBER&paths=FILE_PATH&dS=ABSOLUTE_PATH"))
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

            val result = mailContentGeneratorServiceMock.getFinallySubmittedMailBody(submission, filePaths)

            assertThat(result).isEqualTo(
                """Dear ODCF service,
                |
                |validation of metadata for [${submission.identifier}] has been finished and finally submitted by ${submission.closedUser}.
                |
                |$expected
                |${filePaths.joinToString("\n")}
                |http://LINK?ticketNumber=${submission.ticketNumber}&paths=${filePaths.first()}&dS=ABSOLUTE_PATH
                |
                |Kind regards,
                |ODCF Team""".trimMargin()
            )
        }
    }

    @Test
    fun `get error mail body for empty file path for uploaded submission`() {
        val submission = entityFactory.getUploadSubmission(Submission.Status.CLOSED)
        submission.identifier = "o1234569"
        submission.closedUser = "closedUser"

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        val result = mailContentGeneratorServiceMock.getFinallySubmittedMailBody(submission, emptyList())
        assertThat(result).isEqualTo(
            """Dear ODCF service,
            |
            |validation of metadata for [${submission.identifier}] has been finished and finally submitted by ${submission.closedUser}.
            |
            |Something went wrong when writing out the metadata tsv file! Please check the ValidationService log for detailed information!
            |
            |Kind regards,
            |ODCF Team""".trimMargin()
        )
    }

    @Test
    fun `check TicketSubjectPrefix internalSubmission withTicketId`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")
        submission.identifier = "i1234569"
        submission.ticketNumber = "7654321"

        `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("ODCF#")
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn("S#1234569")

        assertThat(mailContentGeneratorServiceMock.getTicketSubjectPrefix(submission)).isEqualTo("[ODCF#${submission.ticketNumber}][S#1234569]")
    }

    @Test
    fun `check TicketSubjectPrefix internalSubmission withoutTicketId`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")
        submission.identifier = "i1234569"
        submission.ticketNumber = ""

        `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("ODCF#")
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn("S#1234569")

        assertThat(mailContentGeneratorServiceMock.getTicketSubjectPrefix(submission)).isEqualTo("[S#1234569]")
    }

    @Test
    fun `check TicketSubjectPrefix externalSubmission withTicketId`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "o")
        submission.identifier = "o1234569"
        submission.ticketNumber = "7654321"

        `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("ODCF#")
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        assertThat(mailContentGeneratorServiceMock.getTicketSubjectPrefix(submission)).isEqualTo("[ODCF#${submission.ticketNumber}][o1234569]")
    }

    @Test
    fun `check TicketSubjectPrefix externalSubmission withoutTicketId`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "o")
        submission.identifier = "o1234569"
        submission.ticketNumber = ""

        `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("ODCF#")
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        assertThat(mailContentGeneratorServiceMock.getTicketSubjectPrefix(submission)).isEqualTo("[o1234569]")
    }

    @Test
    fun `validate subject for submission without OTRS ticket number`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")
        submission.identifier = "i0012345"
        submission.ticketNumber = ""

        `when`(mailContentGeneratorServiceMock.getTicketSubjectPrefix(submission)).thenReturn("S#12345")

        val subject: String = mailContentGeneratorServiceMock.getFinallySubmittedMailSubject(submission)

        assertThat(subject).isEqualTo("[S#12345] Submission has been validated")
    }

    @Test
    fun `validate subject for submission with OTRS ticket number`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")
        submission.identifier = "i0012345"
        submission.ticketNumber = "54321"

        `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("ODCF#")
        `when`(mailContentGeneratorServiceMock.getTicketSubjectPrefix(submission)).thenReturn("S#12345")

        val subject: String = mailContentGeneratorServiceMock.getFinallySubmittedMailSubject(submission)
        assertThat(subject).isEqualTo("[ODCF#54321][S#12345] Submission has been validated")
    }

    @Test
    fun `validate subject for closed submission`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.CLOSED, "i")

        val subject: String = mailContentGeneratorServiceMock.getFinallySubmittedMailSubject(submission)
        assertThat(subject).contains("Submission has been validated")
    }

    @Test
    fun `get auto-closed subject for auto-closed submission`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.AUTO_CLOSED, "i")

        val subject: String = mailContentGeneratorServiceMock.getFinallySubmittedMailSubject(submission)
        assertThat(subject).contains("Submission has been auto-closed")
    }

    @Test
    fun `get IllegalArgumentException for submission state unlike closed or auto_closed`() {
        val submission = entityFactory.getApiSubmission(Submission.Status.EXPORTED, "i")

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            mailContentGeneratorServiceMock.getFinallySubmittedMailSubject(submission)
        }.withMessage("Status must be CLOSED or AUTO_CLOSED in order to send FinallySubmitted mails!")
    }

    @Test
    fun `get valid uploaded submission mail body`() {
        val submission = entityFactory.getUploadSubmission()
        submission.submitter = entityFactory.getPerson()
        val url = "URL"
        val guideURLBasis = "SERVER_URL"

        `when`(env.getRequiredProperty("application.serverUrl")).thenReturn(guideURLBasis)
        `when`(urlGeneratorService.getURL(submission)).thenReturn(url)

        val mailBody = mailContentGeneratorServiceMock.mailBodyReceivedSubmission(submission)

        assertThat(mailBody).startsWith("Dear firstName lastName")
        assertThat(mailBody).contains("<a href='$url'>Your submission at ODCF</a>")
        assertThat(mailBody).contains("$guideURLBasis/project/overview/user")
    }

    @Test
    fun `get finished externally mail body`() {
        val submission = entityFactory.getApiSubmission()

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn("FORMATTED_IDENTIFIER")

        val result = mailContentGeneratorServiceMock.getFinishedExternallyMailBody(submission)

        assertThat(result).startsWith("Dear ODCF service")
        assertThat(result).contains("metadata for [FORMATTED_IDENTIFIER] has been reported to be finished externally.")
        assertThat(result).endsWith("ODCF Validation Service")
    }

    @Test
    fun `get finished externally mail subject`() {
        val submission = entityFactory.getApiSubmission()

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn("FORMATTED_IDENTIFIER")
        `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("PREFIX")

        val result = mailContentGeneratorServiceMock.getFinishedExternallyMailSubject(submission)

        assertThat(result).isEqualTo("[PREFIX${submission.ticketNumber}][FORMATTED_IDENTIFIER] Submission has been finished externally")
    }

    @Test
    fun `get mail body received submission`() {
        val submission = entityFactory.getApiSubmission()
        submission.submitter = entityFactory.getPerson()
        val url = "URL"
        val guideURLBasis = "SERVER_URL"

        `when`(env.getRequiredProperty("application.serverUrl")).thenReturn(guideURLBasis)
        `when`(urlGeneratorService.getURL(submission)).thenReturn(url)

        val result = mailContentGeneratorServiceMock.mailBodyReceivedSubmission(submission)

        assertThat(result).isEqualTo(
            mailBundle.getString("mailService.receivedSubmissionMailBody")
                .replace("{0}", submission.submitter.fullName)
                .replace("{1}", url)
                .replace("{2}", guideURLBasis)
        )
    }

    @Test
    fun `get mail body fasttrack imported`() {
        val submission = entityFactory.getApiSubmission()
        submission.originProjects = "project1;project2"

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        val result = mailContentGeneratorServiceMock.mailBodyFasttrackImported(submission)

        assertThat(result).isEqualTo(
            mailBundle.getString("mailService.fasttrackBody")
                .replace("{0}", submission.identifier)
                .replace("{1}", submission.originProjects)
        )
    }

    @Test
    fun `get open submission reminder mail subject`() {
        val submission = entityFactory.getApiSubmission()

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)
        `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("ODCF#")

        val result = mailContentGeneratorServiceMock.getOpenSubmissionReminderMailSubject(submission)

        assertThat(result).isEqualTo("[ODCF#${submission.ticketNumber}][${submission.identifier}] Please validate your open submission")
    }

    @Test
    fun `get open submission reminder mail body`() {
        val submission = entityFactory.getApiSubmission()
        submission.submitter = entityFactory.getPerson()
        val baseUrl = "BASE_URL"
        val url = "URL"

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)
        `when`(env.getRequiredProperty("application.serverUrl")).thenReturn(baseUrl)
        `when`(urlGeneratorService.getURL(submission)).thenReturn(url)

        val result = mailContentGeneratorServiceMock.getOpenSubmissionReminderMailBody(submission)

        assertThat(result).isEqualTo(
            mailBundle.getString("mailService.openSubmissionFirstReminderMailBody")
                .replace("{0}", submission.submitter.fullName)
                .replace("{1}", "project ${submission.projects.joinToString()} (${submission.identifier})")
                .replace("{2}", url)
                .replace("{3}", baseUrl)
        )
    }

    @Test
    fun `get open submission reminder mail body multiple projects`() {
        val submission = entityFactory.getApiSubmission()
        val sample1 = entityFactory.getSample(submission)
        sample1.project = "p1"
        val sample2 = entityFactory.getSample(submission)
        sample2.project = "p2"
        submission.samples = listOf(sample1, sample2)
        submission.submitter = entityFactory.getPerson()
        val baseUrl = "BASE_URL"
        val url = "URL"

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)
        `when`(env.getRequiredProperty("application.serverUrl")).thenReturn(baseUrl)
        `when`(urlGeneratorService.getURL(submission)).thenReturn(url)

        val result = mailContentGeneratorServiceMock.getOpenSubmissionReminderMailBody(submission)

        assertThat(result).isEqualTo(
            mailBundle.getString("mailService.openSubmissionFirstReminderMailBody")
                .replace("{0}", submission.submitter.fullName)
                .replace("{1}", "projects ${submission.projects.joinToString()} (${submission.identifier})")
                .replace("{2}", url)
                .replace("{3}", baseUrl)
        )
    }

    @TestFactory
    fun `get subject`() = listOf(
        "lsfService.processingStatusUpdateSubject" to "Data transfer update",
        "lsfService.finalProcessingStatusUpdateSubject" to "Data transfer completed",
        "lsfService.failedUpdateSubject" to "Data transfer failed",
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("get $expected subject") {
            val submission = entityFactory.getApiSubmission()

            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)
            `when`(env.getRequiredProperty("application.mails.ticketSystemPrefix")).thenReturn("ODCF#")

            val result = mailContentGeneratorServiceMock.getTicketSubject(submission, input)

            assertThat(result).isEqualTo("[ODCF#${submission.ticketNumber}][${submission.identifier}] $expected")
        }
    }

    @Test
    fun `get processing status update body single job`() {
        val job = entityFactory.getClusterJob()
        val submission = job.submission
        submission.samples = listOf(entityFactory.getSample(submission))

        `when`(urlGeneratorService.getAdminURL(submission)).thenReturn("URL")

        val result = mailContentGeneratorServiceMock.getProcessingStatusUpdateBody(listOf(job))

        assertThat(result).isEqualTo(
            "${job.printableName}: ${job.state.name}\n" +
                "\n" +
                "${submission.samples.size} sample(s) in this submission:\n" +
                submission.samples.joinToString("\n") { it.name } +
                "\n\n" +
                "For more details go to:\n" +
                "URL"
        )
    }

    @Test
    fun `get processing status update body multiple jobs`() {
        val job = entityFactory.getClusterJob()
        val submission = job.submission
        val job2 = entityFactory.getClusterJob(submission)
        submission.samples = listOf(entityFactory.getSample(submission))

        `when`(urlGeneratorService.getAdminURL(submission)).thenReturn("URL")

        val result = mailContentGeneratorServiceMock.getProcessingStatusUpdateBody(listOf(job, job2))

        assertThat(result).isEqualTo(
            "${job.printableName}: ${job.state.name}\n" +
                "${job2.printableName}: ${job2.state.name}\n" +
                "\n" +
                "${submission.samples.size} sample(s) in this submission:\n" +
                submission.samples.joinToString("\n") { it.name } +
                "\n\n" +
                "For more details go to:\n" +
                "URL"
        )
    }

    @TestFactory
    fun `check get termination reminder mail body for`() = listOf(
        entityFactory.getApiSubmission() to "reminderService.terminationReminderMailBody",
        entityFactory.getUploadSubmission() to "reminderService.extendedSubmissionTerminationReminderMailBody",
    ).map { (submission, expected) ->
        DynamicTest.dynamicTest(submission::class.java.simpleName) {
            val submitter = entityFactory.getPerson()
            submission.submitter = submitter
            submission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(80).atStartOfDay(ZoneId.systemDefault()).toInstant())
            val terminationDateSubmission = submission.startTerminationPeriod!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(90)

            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)
            `when`(urlGeneratorService.getURL(any())).thenReturn("URL")
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn("formattedSubmissionIdentifier")

            val result = mailContentGeneratorServiceMock.getTerminationReminderMailBody(submission)

            assertThat(result).isEqualTo(
                mailBundle.getString(expected)
                    .replace("{0}", submission.submitter.fullName)
                    .replace("{1}", "formattedSubmissionIdentifier")
                    .replace("{2}", "URL")
                    .replace("{3}", terminationDateSubmission.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            )
        }
    }

    @TestFactory
    fun `check get termination mail body for`() = listOf(
        entityFactory.getApiSubmission() to "reminderService.terminationMailBody",
        entityFactory.getUploadSubmission() to "reminderService.extendedSubmissionTerminationMailBody",
    ).map { (submission, expected) ->
        DynamicTest.dynamicTest(submission::class.java.simpleName) {
            val submitter = entityFactory.getPerson()
            submission.submitter = submitter
            submission.startTerminationPeriod = Date.from(LocalDate.now().minusDays(80).atStartOfDay(ZoneId.systemDefault()).toInstant())

            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)
            `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn("formattedSubmissionIdentifier")

            val result = mailContentGeneratorServiceMock.getTerminationMailBody(submission)

            assertThat(result).isEqualTo(
                mailBundle.getString(expected)
                    .replace("{0}", submission.submitter.fullName)
                    .replace("{1}", "formattedSubmissionIdentifier")
            )
        }
    }
}
