package de.dkfz.odcf.guide.service.validator

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.ApiSubmissionRepository
import de.dkfz.odcf.guide.ClusterJobRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.validator.SubmissionServiceImpl
import de.dkfz.odcf.guide.service.interfaces.MergingService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.LSFCommandService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(SpringExtension::class)
class SubmissionServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var submissionServiceMock: SubmissionServiceImpl

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var apiSubmissionRepository: ApiSubmissionRepository

    @Mock
    lateinit var clusterJobRepository: ClusterJobRepository

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var lsfCommandService: LSFCommandService

    @Mock
    lateinit var mergingService: MergingService

    @Mock
    lateinit var env: Environment

    private var i: Int = 0

    private fun getSubmission(): Submission {
        val submission = ApiSubmission()
        submission.identifier = "i000000${i++}"
        submission.status = Submission.Status.IMPORTED
        return submission
    }

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(SubmissionServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `check submission state change to IMPORTED`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.IMPORTED, "testUser")

        assertThat(submission.status).isEqualTo(Submission.Status.IMPORTED)
    }

    @Test
    fun `check submission state change to ON_HOLD`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.ON_HOLD, "testUser")

        assertThat(submission.status).isEqualTo(Submission.Status.ON_HOLD)
    }

    @Test
    fun `check submission state change to EDITED`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.EDITED, "testUser")

        assertThat(submission.status).isEqualTo(Submission.Status.EDITED)
    }

    @Test
    fun `check submission state change to CLOSED`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.CLOSED, "testUser")

        assertThat(submission.status).isEqualTo(Submission.Status.CLOSED)
        assertThat(submission.closedDate).isNotNull
        assertThat(submission.closedUser).isEqualTo("testUser")
    }

    @Test
    fun `check submission state change to AUTO_CLOSED`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.AUTO_CLOSED, "testUser")

        assertThat(submission.status).isEqualTo(Submission.Status.AUTO_CLOSED)
    }

    @Test
    fun `check submission state change to EXPORTED`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.EXPORTED, "testUser")

        assertThat(submission.status).isEqualTo(Submission.Status.EXPORTED)
        assertThat(submission.exportDate).isNotNull
    }

    @Test
    fun `check submission state change to LOCKED`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.LOCKED)

        assertThat(submission.status).isEqualTo(Submission.Status.LOCKED)
        assertThat(submission.lockDate).isNotNull
        assertThat(submission.lockUser).isEqualTo("automatic")
    }

    @Test
    fun `check submission state change to TERMINATED`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.TERMINATED, "testUser")

        assertThat(submission.status).isEqualTo(Submission.Status.TERMINATED)
        assertThat(submission.terminateDate).isNotNull
    }

    @Test
    fun `check submission state change to REMOVED_BY_ADMIN`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.REMOVED_BY_ADMIN)

        assertThat(submission.status).isEqualTo(Submission.Status.REMOVED_BY_ADMIN)
        assertThat(submission.removalUser).isEqualTo("automatic")
    }

    @Test
    fun `check submission state change to RESET`() {
        val submission = getSubmission()

        submissionServiceMock.changeSubmissionState(submission, Submission.Status.RESET, "testUser", "fakeComment")

        assertThat(submission.status).isEqualTo(Submission.Status.RESET)
        assertThat(submission.lockDate).isNull()
        assertThat(submission.lockUser).isNull()
        assertThat(submission.closedDate).isNull()
        assertThat(submission.closedUser).isNull()
        assertThat(submission.removalDate).isNull()
        assertThat(submission.removalDate).isNull()
    }

    @Test
    fun `check dataAvailable state`() {
        val submissionTrue = getSubmission()
        val submissionFalse = getSubmission()

        submissionServiceMock.setExternalDataAvailableForMerging(submissionTrue, true, null)
        submissionServiceMock.setExternalDataAvailableForMerging(submissionFalse, false, null)

        assertThat(submissionTrue.externalDataAvailableForMerging).isTrue
        assertThat(submissionTrue.externalDataAvailabilityDate).isNotNull
        assertThat(submissionTrue.startTerminationPeriod).isNotNull
        assertThat(submissionFalse.externalDataAvailableForMerging).isFalse
    }

    @Test
    fun `check finishSubmissionExternally`() {
        val submission = getSubmission()

        submissionServiceMock.finishSubmissionExternally(submission)

        assertThat(submission.status).isEqualTo(Submission.Status.FINISHED_EXTERNALLY)
    }

    @Test
    fun `check unlock of submission`() {
        val submission = entityFactory.getApiSubmission()
        submission.status = Submission.Status.LOCKED
        submission.lockDate = Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))

        `when`(env.getProperty("application.timeout", "${MetaValController.LOCKED_TIMEOUT_IN_MIN}")).thenReturn("10")
        `when`(submissionRepository.findAllByStatus(Submission.Status.LOCKED)).thenReturn(listOf(submission))

        submissionServiceMock.setUnlockState()

        assertThat(submission.status).isEqualTo(Submission.Status.UNLOCKED)
    }

    @Test
    fun `check unlock of submission if submission should be locked`() {
        val submission = entityFactory.getApiSubmission()
        submission.status = Submission.Status.LOCKED
        submission.lockDate = Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))

        `when`(env.getProperty("application.timeout", "${MetaValController.LOCKED_TIMEOUT_IN_MIN}")).thenReturn("90")
        `when`(submissionRepository.findAllByStatus(Submission.Status.LOCKED)).thenReturn(listOf(submission))

        submissionServiceMock.setUnlockState()

        assertThat(submission.status).isEqualTo(Submission.Status.LOCKED)
    }

    @Test
    fun `check if submission is imported external`() {
        val submission = entityFactory.getApiSubmission()

        `when`(apiSubmissionRepository.findAllByStatusInAndImportedExternalIsFalse(anyList())).thenReturn(setOf(submission))
        `when`(externalMetadataSourceService.getSingleValue(matches("checkIlseNumber"), anyMap())).thenReturn("t")

        assertThat(submission.importedExternal).isFalse

        submissionServiceMock.setCheckIfSubmissionIsImportedExternal()

        assertThat(submission.importedExternal).isTrue
    }
}
