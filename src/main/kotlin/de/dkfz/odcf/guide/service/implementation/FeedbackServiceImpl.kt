package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.FeedbackRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.Feedback
import de.dkfz.odcf.guide.service.interfaces.FeedbackService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.util.*

@Service
class FeedbackServiceImpl(
    private val feedbackRepository: FeedbackRepository,
    private val mailService: MailSenderService,
    private val ldapService: LdapService,
    private val submissionRepository: SubmissionRepository,
    private val collectorService: CollectorService,
    private val env: Environment
) : FeedbackService {

    override fun saveFeedback(feedbackSmiley: String, message: String, submissionId: String): Feedback {
        val feedback = Feedback()
        feedback.user = ldapService.getPerson()
        feedback.date = Date()
        feedback.setRating(feedbackSmiley)
        feedback.message = message
        feedback.submission = submissionRepository.findByIdentifier(submissionId)!!
        feedbackRepository.save(feedback)

        return feedback
    }

    override fun sendFeedbackMail(name: String, mail: String, rating: Feedback.Rating, message: String, submissionId: String?) {
        if (message == "") {
            return
        }
        val replyTo = "$name<$mail>"
        var subject = "Feedback from GUIDE: $rating"

        if (submissionId != null) {
            subject += " (to submission ${collectorService.getFormattedIdentifier(submissionId)})"
        }

        mailService.sendMail(
            env.getRequiredProperty("application.mails.senderAddress"),
            env.getRequiredProperty("application.mails.ticketSystemAddress"),
            "",
            replyTo,
            subject,
            message
        )
    }

    override fun calculateAverage(): String {
        var lastFeedbackCount = 25
        val averageRating: Double
        if (feedbackRepository.findAll().size > 0) {
            if (feedbackRepository.findAll().size < lastFeedbackCount) {
                lastFeedbackCount = feedbackRepository.findAll().size
            }

            val lastFeedbacks = feedbackRepository.findAllByOrderByDateDesc().take(lastFeedbackCount)
            averageRating = lastFeedbacks.sumOf { it.rating.ordinal }.toDouble() / lastFeedbackCount

            return when {
                averageRating < 0.5 -> "sad"
                averageRating > 1.5 -> "happy"
                else -> "neutral"
            }
        } else {
            return "noRating"
        }
    }
}
