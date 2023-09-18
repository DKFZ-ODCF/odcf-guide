package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.FeedbackRepository
import de.dkfz.odcf.guide.entity.Feedback
import de.dkfz.odcf.guide.service.interfaces.FeedbackService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/feedback")
class FeedbackController(
    private val ldapService: LdapService,
    private val collectorService: CollectorService,
    private val feedbackService: FeedbackService,
    private val feedbackRepository: FeedbackRepository,
    private val env: Environment
) {

    @GetMapping("")
    fun showFeedback(model: Model): String {
        val person = ldapService.getPerson()

        return if (person.isAdmin) {
            model["feedbacks"] = feedbackRepository.findAll()
            model["ticketSystemBase"] = env.getRequiredProperty("application.mails.ticketSystemBaseUrl")

            val projectMap: MutableMap<Feedback, String> = mutableMapOf()
            val seqTypesMap: MutableMap<Feedback, String> = mutableMapOf()
            feedbackRepository.findAll().forEach { feedback ->
                projectMap[feedback] =
                    feedback.submission.samples.map { it.project }.toSet().joinToString()

                seqTypesMap[feedback] =
                    feedback.submission.samples.mapNotNull { it.seqType?.name }.toSet().joinToString()
            }

            val averageRating = feedbackService.calculateAverage()
            model["averageRating"] = averageRating
            model["ratingStyle"] = getRatingStyle(averageRating)

            model["projectMap"] = projectMap
            model["seqTypesMap"] = seqTypesMap

            "feedback-overview"
        } else {
            model["errorMessage"] = "You are not allowed to access this page!"
            model["urls"] = collectorService.getUrlsByPerson(person)
            model["error"] = true
            "redirect:/error/403"
        }
    }

    @PostMapping("/change-feedback-ticket")
    fun changeFeedbackTicketNumber(@RequestParam feedback: Feedback, @RequestParam ticket: String): String {
        feedback.ticket = ticket
        feedbackRepository.save(feedback)

        return "redirect:/feedback"
    }

    private fun getRatingStyle(averageRating: String): String {
        return when (averageRating) {
            "sad" -> "badge badge-danger"
            "happy" -> "badge badge-success"
            "neutral" -> "badge badge-warning"
            else -> "badge badge-secondary"
        }
    }
}
