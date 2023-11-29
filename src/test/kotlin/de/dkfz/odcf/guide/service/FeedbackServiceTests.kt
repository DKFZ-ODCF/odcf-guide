package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.FeedbackRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.Feedback
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.FeedbackServiceImpl
import de.dkfz.odcf.guide.service.implementation.security.LdapServiceImpl
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.core.env.Environment
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class FeedbackServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var feedbackServiceMock: FeedbackServiceImpl

    @Mock
    lateinit var feedbackRepository: FeedbackRepository

    @Mock
    lateinit var mailService: MailSenderService

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var ldapServiceImpl: LdapServiceImpl

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var env: Environment

    @Test
    fun `save feedback with message`() {
        val person = entityFactory.getPerson()
        val rating = "happy"
        val message = "message"
        val submission = entityFactory.getApiSubmission()

        `when`(ldapServiceImpl.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)

        val feedback = feedbackServiceMock.saveFeedback(rating, message, submission.identifier)

        assertThat(feedback.rating).isEqualTo(Feedback.Rating.HAPPY)
        assertThat(feedback.message).isEqualTo("message")
        assertThat(feedback.submission).isEqualTo(submission)
        assertThat(feedback.user).isEqualTo(person)
    }

    @Test
    fun `save feedback without message`() {
        val person = entityFactory.getPerson()
        val rating = "sad"
        val message = ""
        val submission = entityFactory.getApiSubmission()

        `when`(ldapServiceImpl.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)

        val feedback = feedbackServiceMock.saveFeedback(rating, message, submission.identifier)

        assertThat(feedback.rating).isEqualTo(Feedback.Rating.SAD)
        assertThat(feedback.message).isEqualTo("")
        assertThat(feedback.submission).isEqualTo(submission)
        assertThat(feedback.user).isEqualTo(person)
    }

    @Test
    fun `average feedback rating`() {
        val feedback1 = entityFactory.getFeedback("happy")
        val feedback2 = entityFactory.getFeedback("happy")
        val feedback3 = entityFactory.getFeedback("neutral")
        val feedback4 = entityFactory.getFeedback("sad")
        val feedbackList = listOf(feedback1, feedback2, feedback3, feedback4)

        `when`(feedbackRepository.findAll()).thenReturn(feedbackList)
        `when`(feedbackRepository.findAllByOrderByDateDesc()).thenReturn(feedbackList)

        val result = feedbackServiceMock.calculateAverage()

        assertThat(result).isEqualTo("neutral")
    }

    @Test
    fun `average feedback of 0 feedbacks`() {
        `when`(feedbackRepository.findAll()).thenReturn(listOf())
        `when`(feedbackRepository.findAllByOrderByDateDesc()).thenReturn(listOf())

        val result = feedbackServiceMock.calculateAverage()

        assertThat(result).isEqualTo("noRating")
    }

    @Test
    fun `average feedback of 1 feedback`() {
        val feedback1 = entityFactory.getFeedback("happy")

        `when`(feedbackRepository.findAll()).thenReturn(listOf(feedback1))
        `when`(feedbackRepository.findAllByOrderByDateDesc()).thenReturn(listOf(feedback1))

        val result = feedbackServiceMock.calculateAverage()

        assertThat(result).isEqualTo("happy")
    }

    @Test
    fun `test send feedback mail success`() {
        val name = "name"
        val mail = "mail"
        val rating = Feedback.Rating.HAPPY
        val message = "message"
        val submissionId = "submissionId"

        `when`(env.getRequiredProperty("application.mails.senderAddress")).thenReturn("testText")
        `when`(env.getRequiredProperty("application.mails.ticketSystemAddress")).thenReturn("testText")
        `when`(collectorService.getFormattedIdentifier(submissionId)).thenReturn(submissionId)

        feedbackServiceMock.sendFeedbackMail(name, mail, rating, message, submissionId)

        verify(mailService, times(1)).sendMail(
            "testText",
            "testText",
            "",
            "$name<$mail>",
            "Feedback from GUIDE: $rating (to submission $submissionId)",
            message
        )
    }

    @Test
    fun `test send feedback mail message empty`() {
        feedbackServiceMock.sendFeedbackMail("name", "mail", Feedback.Rating.HAPPY, "", "submissionId")

        verify(mailService, times(0)).sendMail(
            from = anyString(),
            to = anyString(),
            cc = anyString(),
            replyTo = anyString(),
            subject = anyString(),
            messageText = anyString(),
            attachment = any(),
            deleteAttachmentAfterSending = anyBoolean()
        )
    }
}
