package de.dkfz.odcf.guide.service.validator

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.UploadSubmission
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.validator.DeletionServiceImpl
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.core.env.Environment

@DataJpaTest
open class DeletionServiceDataJpaTests @Autowired constructor(
    private val entityManager: TestEntityManager
) : AnyObject {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var deletionServiceMock: DeletionServiceImpl

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var fileRepository: FileRepository

    @Mock
    lateinit var fieldRequestedValuesRepository: FieldRequestedValuesRepository

    @Mock
    lateinit var seqTypeRepository: SeqTypeRepository

    @Mock
    lateinit var seqTypeRequestedValuesRepository: SeqTypeRequestedValuesRepository

    @Mock
    lateinit var submissionService: SubmissionService

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var env: Environment

    @Test
    fun `check delete ApiSubmission`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val technicalSample = entityFactory.getTechnicalSample(sample)
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample)
        file2.fileName = "fileName2"

        val dummySubmission = entityFactory.getUploadSubmission()
        dummySubmission.identifier = "o0000000"

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        var resultSubmission = ApiSubmission()
        `when`(submissionRepository.delete(anySubmission())).then { i -> (i.arguments[0] as ApiSubmission).also { resultSubmission = it } }

        `when`(submissionRepository.findByIdentifier("o0000000")).thenReturn(dummySubmission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "deletionService.submission.subject")).thenReturn("subject")
        `when`(mailContentGeneratorService.getMailBody(anyString(), anyMap())).thenReturn("body")
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        val deleted = deletionServiceMock.deleteSubmission(submission)

        assertThat(deleted).isEqualTo(true)
        assertThat(submission.identifier).isEqualTo(resultSubmission.identifier)
        verify(submissionRepository, times(1)).delete(submission)
        verify(mailSenderService, times(1)).sendMailToTicketSystem("subject", "body")
    }

    @Test
    fun `check delete UploadSubmission`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        val technicalSample = entityFactory.getTechnicalSample(sample)
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample)
        file2.fileName = "fileName2"

        val dummySubmission = entityFactory.getUploadSubmission()
        dummySubmission.identifier = "o0000000"

        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        var resultSubmission = UploadSubmission()
        `when`(submissionRepository.delete(anySubmission())).then { i -> (i.arguments[0] as UploadSubmission).also { resultSubmission = it } }

        `when`(submissionRepository.findByIdentifier("o0000000")).thenReturn(dummySubmission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "deletionService.submission.subject")).thenReturn("subject")
        `when`(mailContentGeneratorService.getMailBody(anyString(), anyMap())).thenReturn("body")

        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        val deleted = deletionServiceMock.deleteSubmission(submission)

        assertThat(deleted).isEqualTo(true)
        assertThat(submission.identifier).isEqualTo(resultSubmission.identifier)
        verify(submissionRepository, times(1)).delete(submission)
        verify(mailSenderService, times(1)).sendMailToTicketSystem("subject", "body")
    }

    @Test
    fun `check delete sample`() {
        val sample = entityFactory.getSample()
        val technicalSample = entityFactory.getTechnicalSample(sample)
        val file1 = entityFactory.getFile(sample)
        val file2 = entityFactory.getFile(sample)
        file2.fileName = "fileName2"

        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(technicalSample)
        entityManager.persistAndFlush(sample.submission)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(file1)
        entityManager.persistAndFlush(file2)

        var resultSample = Sample()
        `when`(sampleRepository.delete(anySample())).then { i -> (i.arguments[0] as Sample).also { resultSample = it } }
        `when`(fileRepository.findAllBySample(sample)).thenReturn(listOf(file1, file2))

        val deleted = deletionServiceMock.deleteSample(sample)

        assertThat(deleted).isEqualTo(true)
        assertThat(sample.id).isEqualTo(resultSample.id)
        verify(sampleRepository, times(1)).delete(sample)
        verify(fileRepository, times(1)).deleteAll(listOf(file1, file2))
    }

    @Test
    fun `check changing references from requested values to submission and deleting unused requested values`() {
        val submission = entityFactory.getUploadSubmission()
        val submission2 = entityFactory.getUploadSubmission()
        val requestedValue1 = entityFactory.getRequestedValue(submission)
        requestedValue1.usedSubmissions.add(submission2)
        requestedValue1.requester.username = "otherUsername"
        val requestedValue2 = entityFactory.getRequestedValue(submission)

        val dummySubmission = entityFactory.getUploadSubmission()
        dummySubmission.identifier = "o0000000"

        `when`(submissionRepository.findByIdentifier("o0000000")).thenReturn(dummySubmission)
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedValue1, requestedValue2))

        deletionServiceMock.deleteSubmissionFromRequestedValues(submission)

        assertThat(requestedValue1.originSubmission).isEqualTo(dummySubmission)
        assertThat(requestedValue1.usedSubmissions).isEqualTo(setOf(submission2))
        verify(fieldRequestedValuesRepository, times(0)).delete(requestedValue1)
        verify(fieldRequestedValuesRepository, times(1)).delete(requestedValue2)
    }

    @Test
    fun `check that requested value is deleted after deleting all used submissions - first origin submission`() {
        val submission = entityFactory.getUploadSubmission()
        val submission2 = entityFactory.getUploadSubmission()
        val requestedValue1 = entityFactory.getRequestedValue(submission)
        requestedValue1.usedSubmissions.add(submission2)
        requestedValue1.requester.username = "otherUsername"

        val dummySubmission = entityFactory.getUploadSubmission()
        dummySubmission.identifier = "o0000000"

        `when`(submissionRepository.findByIdentifier("o0000000")).thenReturn(dummySubmission)
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedValue1))
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission2)).thenReturn(setOf(requestedValue1))

        deletionServiceMock.deleteSubmissionFromRequestedValues(submission)

        assertThat(requestedValue1.originSubmission).isEqualTo(dummySubmission)
        assertThat(requestedValue1.usedSubmissions).isEqualTo(setOf(submission2))
        verify(fieldRequestedValuesRepository, times(0)).delete(requestedValue1)

        deletionServiceMock.deleteSubmissionFromRequestedValues(submission2)

        verify(fieldRequestedValuesRepository, times(1)).delete(requestedValue1)
    }

    @Test
    fun `check that requested value is deleted after deleting all used submissions - first used submission`() {
        val submission = entityFactory.getUploadSubmission()
        val submission2 = entityFactory.getUploadSubmission()
        val requestedValue1 = entityFactory.getRequestedValue(submission)
        requestedValue1.usedSubmissions.add(submission2)
        requestedValue1.requester.username = "otherUsername"

        val dummySubmission = entityFactory.getUploadSubmission()
        dummySubmission.identifier = "o0000000"

        `when`(submissionRepository.findByIdentifier("o0000000")).thenReturn(dummySubmission)
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedValue1))
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission2)).thenReturn(setOf(requestedValue1))

        deletionServiceMock.deleteSubmissionFromRequestedValues(submission2)

        assertThat(requestedValue1.originSubmission).isNotEqualTo(dummySubmission)
        assertThat(requestedValue1.usedSubmissions).isEqualTo(setOf(submission))
        verify(fieldRequestedValuesRepository, times(0)).delete(requestedValue1)

        deletionServiceMock.deleteSubmissionFromRequestedValues(submission)

        verify(fieldRequestedValuesRepository, times(1)).delete(requestedValue1)
    }

    @Test
    fun `check deleteSeqType`() {
        val sample = entityFactory.getSample()
        val seqType = sample.seqType!!
        val requestedSeqType = entityFactory.getRequestedSeqType(sample.submission, seqType)
        val sample2 = entityFactory.getSample()

        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(sample.submission)
        entityManager.persistAndFlush(requestedSeqType.requester)
        entityManager.persistAndFlush(requestedSeqType)
        entityManager.persistAndFlush(sample)
        entityManager.persistAndFlush(sample2.seqType)
        entityManager.persistAndFlush(sample2.submission)
        entityManager.persistAndFlush(sample2)

        `when`(sampleRepository.findAllBySeqTypeAndSubmission_StatusIn(seqType, Submission.Status.filterBySampleIsCorrectable())).thenReturn(listOf(sample))
        `when`(seqTypeRequestedValuesRepository.findAllByRequestedSeqType(seqType)).thenReturn(setOf(requestedSeqType))

        val deleted = deletionServiceMock.deleteSeqType(seqType)

        assertThat(deleted).isEqualTo(true)
        assertThat(sample.seqType).isNull()
        assertThat(sample2.seqType).isNotNull
        assertThat(requestedSeqType.requestedSeqType).isNull()
        assertThat(requestedSeqType.state).isEqualTo(RequestedValue.State.REJECTED)
    }

    @Test
    fun `check deleteSeqType that is a requestedValue even if submission was validated`() {
        val sample = entityFactory.getSample()
        val submission = sample.submission
        submission.status = Submission.Status.VALIDATED
        val seqType = sample.seqType!!
        val requestedSeqType = entityFactory.getRequestedSeqType(submission, seqType)

        entityManager.persistAndFlush(sample.seqType)
        entityManager.persistAndFlush(submission)
        entityManager.persistAndFlush(requestedSeqType.requester)
        entityManager.persistAndFlush(requestedSeqType)

        `when`(sampleRepository.findAllBySeqTypeAndSubmission_StatusIn(seqType, listOf(Submission.Status.VALIDATED))).thenReturn(listOf(sample))
        `when`(seqTypeRequestedValuesRepository.findAllByRequestedSeqType(seqType)).thenReturn(setOf(requestedSeqType))
        `when`(sampleRepository.findAllBySeqTypeAndSubmission_StatusIn(seqType, Submission.Status.filterBySampleIsCorrectable())).thenReturn(listOf(sample))

        val deleted = deletionServiceMock.deleteSeqType(seqType)

        assertThat(deleted).isEqualTo(true)
        verify(submissionService, times(1)).changeSubmissionState(submission, Submission.Status.UNLOCKED)
        verify(submissionRepository, times(1)).saveAll(listOf(submission))
        assertThat(sample.seqType).isNull()
        assertThat(requestedSeqType.requestedSeqType).isNull()
        assertThat(requestedSeqType.state).isEqualTo(RequestedValue.State.REJECTED)
    }
}
