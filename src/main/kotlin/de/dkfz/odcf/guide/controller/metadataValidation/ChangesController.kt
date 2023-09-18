package de.dkfz.odcf.guide.controller.metadataValidation

import de.dkfz.odcf.guide.ApiSubmissionRepository
import de.dkfz.odcf.guide.ClusterJobRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.EXTENDED_READ_ONLY
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.EXTENDED_TABLE_PAGE_ADMIN
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_READ_ONLY
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_TABLE_PAGE_ADMIN
import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.UploadSubmission
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.exceptions.OutputFileNotWritableException
import de.dkfz.odcf.guide.helperObjects.ControllerHelper
import de.dkfz.odcf.guide.service.deprecated.JsonImportService
import de.dkfz.odcf.guide.service.interfaces.*
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.MergingService
import de.dkfz.odcf.guide.service.interfaces.external.LSFCommandService
import de.dkfz.odcf.guide.service.interfaces.importer.IlseImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.view.RedirectView
import java.util.*

@Controller
@RequestMapping("/metadata-validator/submission-actions")
class ChangesController(
    private val submissionService: SubmissionService,
    private val deletionService: DeletionService,
    private val terminationService: TerminationService,
    private val ldapService: LdapService,
    private val env: Environment,
    private val mergingService: MergingService,
    private val collectorService: CollectorService,
    private val ilseImportService: IlseImportService,
    private val jsonImportService: JsonImportService,
    private val fileService: FileService,
    private val mailService: MailSenderService,
    private val lsfCommandService: LSFCommandService,
    private val submissionRepository: SubmissionRepository,
    private val apiSubmissionRepository: ApiSubmissionRepository,
    private val sequencingTechnologyService: SequencingTechnologyService,
    private val clusterJobRepository: ClusterJobRepository,
    private val authorizationService: AuthorizationService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())
    private val controllerHelper = ControllerHelper(ldapService)

    @PostMapping("/reopenSubmission")
    fun reopenSubmission(
        @RequestParam("identifier") identifier: String,
        redirectAttributes: RedirectAttributes
    ): RedirectView {
        val submission = submissionRepository.findByIdentifier(identifier)!!
        submissionService.changeSubmissionState(submission, Submission.Status.LOCKED, null)
        submission.closedUser = null
        submission.closedDate = null
        submission.importedExternal = false
        submissionRepository.save(submission)
        mailService.sendReopenSubmissionMail(submission)
        submissionService.changeSubmissionState(submission, Submission.Status.UNLOCKED, null)
        val page = if (submission.identifier.startsWith("i")) {
            SIMPLE_TABLE_PAGE_ADMIN
        } else {
            EXTENDED_TABLE_PAGE_ADMIN
        }
        val redirectView = RedirectView("$page?identifier=${submission.identifier}", true)
        redirectAttributes.addFlashAttribute("successMessage", "Reopened submission ${submission.identifier}")
        redirectAttributes.addFlashAttribute("success", true)
        return redirectView
    }

    @PostMapping("/retrigger-merging")
    fun retriggerMerging(
        @RequestParam("identifier") identifier: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val submission = submissionRepository.findByIdentifier(identifier)!!
        if (!submission.isFinished) {
            redirectAttributes.addFlashAttribute("warning", true)
            redirectAttributes.addFlashAttribute("warningMessage", "Submission ${collectorService.getFormattedIdentifier(submission.identifier)} is not finished, yet!")
        } else {
            if (env.getProperty("application.mergingService.doMerging", "false").toBoolean()) {
                try {
                    mergingService.doMerging(submission)
                } catch (e: GuideRuntimeException) {
                    val ticketUrl = env.getRequiredProperty("application.mails.ticketSystemBaseUrl") + submission.ticketNumber
                    val link = "<a href=\"${ticketUrl}\" target=\"_blank\">ticket system</a>"
                    redirectAttributes.addFlashAttribute("warning", true)
                    redirectAttributes.addFlashAttribute("warningMessage", "Could not merge table for submission ${collectorService.getFormattedIdentifier(submission.identifier)}. Please refer to $link for detailed information!")
                    return "redirect:$SIMPLE_READ_ONLY?uuid=${submission.uuid}"
                }
                redirectAttributes.addFlashAttribute("info", true)
                redirectAttributes.addFlashAttribute("infoMessage", "Retriggered merging for ${collectorService.getFormattedIdentifier(submission.identifier)}")
            } else {
                redirectAttributes.addFlashAttribute("warning", true)
                redirectAttributes.addFlashAttribute("warningMessage", "Merging has been turned off! Could not trigger merging procedure.")
            }
        }
        return "redirect:$SIMPLE_READ_ONLY?uuid=${submission.uuid}"
    }

    @PostMapping("/finally")
    fun showFinalSubmissionPage(@RequestParam("uuid") uuid: String, redirectAttributes: RedirectAttributes): String {
        val submission = submissionRepository.findByUuid(UUID.fromString(uuid))
        if (submission!!.isValidated) {
            submissionService.changeSubmissionState(submission, Submission.Status.CLOSED, ldapService.getPerson().username)
            val filePath = try {
                fileService.writeLongTsvFile(submission)
            } catch (e: OutputFileNotWritableException) {
                logger.error("Could not write out tsv file for submission ${collectorService.getFormattedIdentifier(submission.identifier)}:\n${e.message}")
                ""
            }
            try {
                mailService.sendFinallySubmittedMail(submission, filePath.split("\n"))
            } catch (e: Exception) {
                logger.info("Could not send finally submitted mail:\n${e.message}")
            }
            if (submission.externalDataAvailableForMerging) {
                submissionService.postProceedWithSubmission(submission)
            }
            redirectAttributes.addFlashAttribute("header", "Finally submitted.")
            redirectAttributes.addFlashAttribute("showFeedback", true)
        }
        val page = if (submission.identifier.startsWith("i")) {
            SIMPLE_READ_ONLY
        } else {
            EXTENDED_READ_ONLY
        }
        return "redirect:$page?uuid=${submission.uuid}"
    }

    @GetMapping("/deleteSubmission")
    fun delete(@RequestParam identifier: String, redirectAttributes: RedirectAttributes): String {
        val submission = submissionRepository.findByIdentifier(identifier)
        val isUploadedSubmission = submission is UploadSubmission
        val (type, message) = if (ldapService.isCurrentUserAdmin() || isUploadedSubmission) {
            if (submission != null) {
                if (deletionService.deleteSubmission(submission)) {
                    "success" to "Submission with identifier " + collectorService.getFormattedIdentifier(submission.identifier) + " has been deleted."
                } else {
                    "error" to "Could not delete submission with identifier " + collectorService.getFormattedIdentifier(submission.identifier) + ". Please contact ODCF Validation Service Developers!"
                }
            } else {
                "error" to "Could not find a submission with identifier $identifier"
            }
        } else {
            "error" to "You are not allowed to delete submission $identifier! Did not delete submission."
        }
        redirectAttributes.addFlashAttribute("${type}Message", message)
        redirectAttributes.addFlashAttribute(type, true)
        return "redirect:/metadata-validator/overview/${"uploaded/".takeIf { isUploadedSubmission }.orEmpty()}admin"
    }

    private fun checkForAdminActions(identifier: String): Pair<String, String> {
        val submission = submissionRepository.findByIdentifier(identifier)
        return if (ldapService.isCurrentUserAdmin()) {
            if (submission == null) {
                "" to ""
            } else {
                "error" to "Could not find a submission with identifier $identifier"
            }
        } else {
            "error" to "You do not have admin privileges!"
        }
    }

    @GetMapping("/terminate-submission")
    fun terminateSubmission(@RequestParam identifier: String, redirectAttributes: RedirectAttributes): String {
        val submission = submissionRepository.findByIdentifier(identifier)
        var messagePair = checkForAdminActions(identifier)
        if (messagePair.first.isNotBlank()) {
            terminationService.terminateSubmission(submission!!, "terminatedMailSubjectGUI")
            messagePair = "success" to "Submission with identifier '${collectorService.getFormattedIdentifier(submission.identifier)}' has been terminated."
        }
        val (type, message) = messagePair
        redirectAttributes.addFlashAttribute("${type}Message", message)
        redirectAttributes.addFlashAttribute(type, true)
        return "redirect:/metadata-validator/overview/admin"
    }

    @GetMapping("/reset-termination-timer")
    fun resetTerminationTimer(@RequestParam identifier: String, redirectAttributes: RedirectAttributes): String {
        val submission = submissionRepository.findByIdentifier(identifier)
        var messagePair = checkForAdminActions(identifier)
        if (messagePair.first.isNotBlank()) {
            terminationService.resetSubmissionTerminationPeriod(submission!!)
            messagePair = "success" to "The termination period of submission $identifier has been reset."
        }
        val (type, message) = messagePair
        redirectAttributes.addFlashAttribute("${type}Message", message)
        redirectAttributes.addFlashAttribute(type, true)
        return "redirect:/metadata-validator/overview/admin"
    }

    @GetMapping("/finish-submission-externally")
    fun finishedSubmissionExternally(@RequestParam identifier: String, redirectAttributes: RedirectAttributes): String {
        val submission = submissionRepository.findByIdentifier(identifier)
        var messagePair = checkForAdminActions(identifier)
        if (messagePair.first.isNotBlank()) {
            submissionService.finishSubmissionExternally(submission!!)
            mailService.sendFinishedExternallyMail(submission)
            messagePair = "success" to "Submission $identifier has been labeled as 'finished externally'"
        }
        val (type, message) = messagePair
        redirectAttributes.addFlashAttribute("${type}Message", message)
        redirectAttributes.addFlashAttribute(type, true)
        return "redirect:/metadata-validator/overview/admin"
    }

    private fun resetSubmission(submission: Submission) {
        if (submission.identifier.startsWith("i")) {
            ilseImportService.reimport(submission)
        } else {
            jsonImportService.reimport(submission)
        }
    }

    @PostMapping("/reset")
    fun resetSubmission(
        @RequestParam submissionUuid: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val submission = submissionRepository.findByUuid(UUID.fromString(submissionUuid))!!
        try {
            resetSubmission(submission)
            redirectAttributes.addFlashAttribute("warning", true)
            redirectAttributes.addFlashAttribute(
                "warningMessage",
                bundle.getString("details.resetSuccessful")
                    .replace("{0}", collectorService.getFormattedIdentifier(submission.identifier))
            )
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                bundle.getString("details.resetNotPossible.explanation")
                    .replace("{0}", collectorService.getFormattedIdentifier(submission.identifier))
            )
            logger.warn("Could not reset submission. Please contact ODCF Service!:\n" + e.stackTraceToString())
        }
        return controllerHelper.getRedirectPage(submission)
    }

    @PostMapping("/toggle-on-hold")
    fun toggleSubmissionOnHold(
        @RequestParam identifier: String,
        @RequestParam stateComment: String,
        @RequestParam(required = false) redirectToOverview: Boolean?,
        redirectAttributes: RedirectAttributes
    ): String {
        val submission = submissionRepository.findByIdentifier(identifier)!!
        val state = if (submission.isOnHold) {
            Submission.Status.UNLOCKED
        } else {
            Submission.Status.ON_HOLD
        }
        submissionService.changeSubmissionState(
            submission = submission,
            status = state,
            username = ldapService.getPerson().username,
            stateComment = stateComment,
        )

        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Submission [${collectorService.getFormattedIdentifier(submission.identifier)}] successfully set to: ${state.name}."
        )
        redirectAttributes.addFlashAttribute("success", true)
        return if (redirectToOverview != null) {
            "redirect:/metadata-validator/overview/admin"
        } else {
            "redirect:/metadata-validator/submission/simple/admin?identifier=${submission.identifier}"
        }
    }

    @PostMapping("/prevent-termination")
    fun preventTermination(@RequestParam identifier: String): String {
        val submission = submissionRepository.findByIdentifier(identifier) ?: throw GuideRuntimeException("Submission $identifier not found")
        submission.terminationState = Submission.TerminationState.PREVENT_TERMINATION
        submissionRepository.save(submission)

        return "redirect:$SIMPLE_TABLE_PAGE_ADMIN?identifier=${submission.identifier}"
    }

    @PostMapping("/retrigger-sequencing-technology")
    fun retriggerSequencingTechnology(@RequestParam identifier: String): String {
        val submission = apiSubmissionRepository.findByIdentifier(identifier) ?: throw GuideRuntimeException("Submission $identifier not found")
        val sequencingTechnologyName = submission.samples.first().requestedSequencingInfo
        val sequencingTechnology = sequencingTechnologyService.getSequencingTechnology(sequencingTechnologyName)
        submission.sequencingTechnology = sequencingTechnology ?: submission.sequencingTechnology
        submission.validationLevel = sequencingTechnology?.validationLevel ?: submission.validationLevel
        submissionRepository.save(submission)

        return "redirect:$SIMPLE_TABLE_PAGE_ADMIN?identifier=${submission.identifier}"
    }

    @PostMapping("/trigger-cluster-job")
    fun retriggerClusterJob(@RequestParam id: Int): String {
        val job = clusterJobRepository.getOne(id)
        val jobToStart = job.takeIf { it.remoteId == -1 } ?: ClusterJob(job)
        clusterJobRepository.save(jobToStart)
        val subsequentJob = clusterJobRepository.findByParentJobAndRestartedJobIsNull(job)
        if (subsequentJob != null) {
            subsequentJob.parentJob = jobToStart
            clusterJobRepository.save(subsequentJob)
        }
        lsfCommandService.tryToRunJob(jobToStart, forceStart = true)
        return "redirect:$SIMPLE_READ_ONLY?uuid=${job.submission.uuid}"
    }

    @PostMapping("/register-ticket-number")
    fun registerTicketNumber(
        @RequestHeader(value = "User-Token", required = false) token: String?,
        @RequestParam identifier: String,
        @RequestParam ticketNumber: String
    ): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }
        val submission = submissionRepository.findByIdentifier(identifier)
            ?: return ResponseEntity("Submission $identifier not found", HttpStatus.NOT_FOUND)
        if (submission.ticketNumber.isNotBlank()) {
            return ResponseEntity("Submission has already the ticket number ${submission.ticketNumber}", HttpStatus.CONFLICT)
        }
        submission.ticketNumber = ticketNumber
        submissionRepository.save(submission)
        return ResponseEntity("Ticket number added", HttpStatus.OK)
    }
}
