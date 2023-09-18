package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.ClusterJobRepository
import de.dkfz.odcf.guide.PersonRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.UploadSubmission
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.*
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping("/metadata-validator/overview")
class OverviewController(
    private val collectorService: CollectorService,
    private val submissionRepository: SubmissionRepository,
    private val ldapService: LdapService,
    private val personRepository: PersonRepository,
    private val clusterJobRepository: ClusterJobRepository,
    private val env: Environment
) {

    @GetMapping("")
    fun showSimpleMetadataValidator(redirectAttributes: RedirectAttributes, model: Model): String {
        model.asMap().forEach { redirectAttributes.addFlashAttribute(it.key, it.value) }
        if (ldapService.getPerson().isAdmin) {
            return "redirect:/metadata-validator/overview/admin"
        }
        return "redirect:/metadata-validator/overview/user"
    }

    @GetMapping("/admin")
    fun showAdminOverview(redirectAttributes: RedirectAttributes, model: Model): String {
        if (!ldapService.getPerson().isAdmin) {
            return "redirect:/error/403"
        }
        model["types"] = listOf("active", "closed", "terminated", "auto-closed", "finished-externally", "exported")
        model["ticketSystemBase"] = env.getRequiredProperty("application.mails.ticketSystemBaseUrl")
        model["date"] = Date()
        model["user"] = ldapService.getPerson()
        model["timeout"] = env.getRequiredProperty("application.timeout").toInt() * 60000
        return "metadataValidator/overview"
    }

    @GetMapping("/user")
    fun showUserOverview(redirectAttributes: RedirectAttributes, model: Model): String {
        model["types"] = listOf("active", "closed", "exported")
        model["date"] = Date()
        model["user"] = ldapService.getPerson()
        model["timeout"] = env.getRequiredProperty("application.timeout").toInt() * 60000
        return "metadataValidator/user-overview"
    }

    @GetMapping("/uploaded")
    fun showUploadedOverview(redirectAttributes: RedirectAttributes, model: Model): String {
        model.asMap().forEach { redirectAttributes.addFlashAttribute(it.key, it.value) }
        if (ldapService.getPerson().isAdmin) {
            return "redirect:/metadata-validator/overview/uploaded/admin"
        }
        return "redirect:/metadata-validator/overview/uploaded/user"
    }

    @GetMapping("/uploaded/user")
    fun showUploadedUserOverview(model: Model): String {
        model["types"] = listOf("active", "closed", "exported")
        model["date"] = Date()
        model["admin"] = false
        model["user"] = ldapService.getPerson()
        model["timeout"] = env.getRequiredProperty("application.timeout").toInt() * 60000
        return "metadataInput/upload/user-overview"
    }

    @GetMapping("/uploaded/admin")
    fun showUploadedAdminOverview(model: Model): String {
        val person = ldapService.getPerson()
        return if (person.isAdmin) {
            model["types"] = listOf("active", "closed", "terminated", "auto-closed", "finished-externally", "exported")
            model["ticketSystemBase"] = env.getRequiredProperty("application.mails.ticketSystemBaseUrl")
            model["date"] = Date()
            model["admin"] = true
            model["user"] = person
            model["timeout"] = env.getRequiredProperty("application.timeout").toInt() * 60000
            "metadataInput/upload/admin-overview"
        } else {
            "redirect:/metadata-validator/overview/uploaded/user"
        }
    }

    @PostMapping("/change-submission-ticket")
    fun changeFeedbackTicketNumber(@RequestParam submission: Submission, @RequestParam ticket: String): String {
        submission.ticketNumber = ticket
        submissionRepository.save(submission)
        return if (submission.isApiSubmission) "redirect:/metadata-validator/overview/"
        else "redirect:/metadata-validator/overview/uploaded/"
    }

    @PostMapping("/change-submission-custom-name-and-comment")
    fun changeSubmissionCustomNameAndComment(
        redirectAttributes: RedirectAttributes,
        @RequestParam submission: Submission,
        @RequestParam submissionCustomName: String,
        @RequestParam submissionCustomComment: String,
    ): String {
        if (submission.isApiSubmission) {
            redirectAttributes.addFlashAttribute("errorMessage", "A submission from ILSe doesn't have a custom name or comment.")
            redirectAttributes.addFlashAttribute("error", true)
            return "redirect:/metadata-validator/overview/user"
        }
        submission as UploadSubmission
        submission.customName = submissionCustomName
        submission.comment = submissionCustomComment
        submissionRepository.save(submission)
        return "redirect:/metadata-validator/overview/uploaded/"
    }

    @GetMapping("/get-submissions/{state}")
    fun getApiSubmissions(@PathVariable state: String, request: HttpServletRequest): ResponseEntity<*> {
        return getSubmissions<ApiSubmission>(state, request)
    }

    @GetMapping("/uploaded/get-submissions/{state}")
    fun getUploadedSubmissions(@PathVariable state: String, request: HttpServletRequest): ResponseEntity<*> {
        return getSubmissions<UploadSubmission>(state, request)
    }

    private inline fun <reified clazz> getSubmissions(state: String, request: HttpServletRequest): ResponseEntity<*> {
        val token = request.getHeader("User-Token")
        if (token.isNullOrBlank()) return ResponseEntity("No tokens were found", HttpHeaders(), HttpStatus.UNAUTHORIZED)
        val user = personRepository.findByApiToken(token) ?: return ResponseEntity("Invalid token", HttpHeaders(), HttpStatus.FORBIDDEN)

        var limitResults = false
        val states = when (state) {
            "active" -> Submission.Status.values().filter { it.group == "active" || it.group == "paused" }
            "closed" -> listOf(Submission.Status.CLOSED)
            "exported" -> { limitResults = true; listOf(Submission.Status.EXPORTED) }
            "terminated" -> { limitResults = true; listOf(Submission.Status.TERMINATED) }
            "auto-closed" -> { limitResults = true; listOf(Submission.Status.AUTO_CLOSED) }
            "finished-externally" -> { limitResults = true; listOf(Submission.Status.FINISHED_EXTERNALLY) }
            else -> emptyList()
        }
        val submissions = if (user.isAdmin) {
            if (limitResults) submissionRepository.findTop100ByStatusInOrderByImportDateDesc(states) else submissionRepository.findAllByStatusIn(states)
        } else {
            collectorService.getAllSubmissionsPerUser().filter { states.contains(it.status) }
        }.filter { it is clazz }
        val submissionMap = submissions.sortedBy { it.identifier }.map {
            val map = emptyMap<String, String>().toMutableMap()
            map["submission"] = "${it.identifier} [${it.samples.size} samples] (${it.submitter.fullName})"
            map["identifier"] = it.identifier
            map["customName"] = if (it is UploadSubmission) it.customName else ""
            map["stateNotActive"] = (it.isDiscontinued || it.isFinished).toString()
            map["uuid"] = it.uuid.toString()
            map["projectNames"] = it.projects.sorted().joinToString()
            map["importDate"] = it.formattedImportDate
            map["received"] = it.getFormattedDate(it.externalDataAvailabilityDate)
            map["state"] = it.status.name
            map["submissionComment"] = if (it is UploadSubmission) it.comment else ""
            if (user.isAdmin) {
                map["ticketNumber"] = it.ticketNumber
                map["editor"] = it.lockUser.orEmpty()
                map["externalDataAvailableForMerging"] = it.externalDataAvailableForMerging.toString()
                map["finally"] = it.closedUser.orEmpty()
                map["clusterJob"] = clusterJobRepository.findAllBySubmission(it).maxByOrNull { it.dateCreated }?.state.toString()
                map["onHoldComment"] = it.onHoldComment
            }
            return@map map
        }
        return ResponseEntity(submissionMap, HttpHeaders(), HttpStatus.OK)
    }
}
