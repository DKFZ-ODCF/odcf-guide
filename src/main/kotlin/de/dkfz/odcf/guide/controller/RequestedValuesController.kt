package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.FieldRequestedValuesRepository
import de.dkfz.odcf.guide.SeqTypeRequestedValuesRepository
import de.dkfz.odcf.guide.entity.requestedValues.FieldRequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.SeqTypeRequestedValue
import de.dkfz.odcf.guide.service.interfaces.RequestedValueService
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/admin/requested-values")
class RequestedValuesController(
    private val ldapService: LdapService,
    private val authorizationService: AuthorizationService,
    private val requestedValueService: RequestedValueService,
    private val fieldRequestedValuesRepository: FieldRequestedValuesRepository,
    private val seqTypeRequestedValuesRepository: SeqTypeRequestedValuesRepository,
    private val seqTypeMappingService: SeqTypeMappingService,
) {

    @GetMapping("overview")
    fun showRequestedValues(model: Model): String {
        if (!ldapService.isCurrentUserAdmin()) {
            return "redirect:/error/403"
        }
        model["requestedValues"] = fieldRequestedValuesRepository.findAll()
        return "admin/requested-values"
    }

    @GetMapping("/get-active-requests")
    fun getActiveRequests(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }
        val requestedValues = fieldRequestedValuesRepository.findAllByState(RequestedValue.State.REQUESTED)
        val result = getResultMap(requestedValues)
        return ResponseEntity(result, HttpHeaders(), HttpStatus.OK)
    }

    @GetMapping("/get-finished-requests")
    fun getFinishedRequests(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }
        val requestedValues = fieldRequestedValuesRepository.findAllByStateIn(RequestedValue.State.values().filter { it.process == "finished" })
        val result = getResultMap(requestedValues)
        return ResponseEntity(result, HttpHeaders(), HttpStatus.OK)
    }

    @GetMapping("/get-requested-seq-types")
    fun getRequestedSeqTypes(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }

        val requestedValues = seqTypeRequestedValuesRepository.findAllByState(RequestedValue.State.REQUESTED)

        val reqValMap = requestedValues.map {
            val reqSeqType = it.requestedSeqType!!
            mapOf(
                "id" to it.id,
                "seqTypeName" to it.requestedValue,
                "seqTypeId" to reqSeqType.id,
                "basicSeqType" to reqSeqType.basicSeqType,
                "seqTypeOptions" to reqSeqType.seqTypeOptions,
                "requester" to "${it.requester.fullName} (${it.requester.username})",
                "originSubmission" to it.originSubmission.identifier,
                "usedSubmissions" to it.usedSubmissions
                    .sortedBy { it.identifier }
                    .joinToString { submission -> "<s>${submission.identifier}</s>".takeIf { submission.isFinished } ?: submission.identifier },
                "createdValue" to (it.state.takeIf { it == RequestedValue.State.REJECTED } ?: it.createdValueAs),
                "formattedDateCreated" to it.formattedDateCreated,
                "formattedLastUpdate" to it.formattedLastUpdate
            )
        }

        return ResponseEntity(mapOf("data" to reqValMap), HttpHeaders(), HttpStatus.OK)
    }

    @PostMapping("/accept-value")
    fun acceptValue(@RequestParam id: Int, @RequestParam(required = false) newValue: String?, redirectAttributes: RedirectAttributes): String {
        var redirectPage = "redirect:/admin/requested-values/overview"
        if (!ldapService.isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to use this page")
            redirectAttributes.addFlashAttribute("error", true)
        } else {
            val requestedValue = fieldRequestedValuesRepository.findById(id).orElse(null) ?: seqTypeRequestedValuesRepository.getOne(id)
            if (newValue.isNullOrBlank()) {
                requestedValueService.acceptValue(requestedValue)
            } else {
                requestedValueService.acceptAndCorrectValue(requestedValue, newValue)
            }
            redirectAttributes.addFlashAttribute("successMessage", "Value accepted")
            redirectAttributes.addFlashAttribute("success", true)
            if (requestedValue is SeqTypeRequestedValue) redirectPage = "redirect:/metadata-input/sequencing-type"
        }
        return redirectPage
    }

    @PostMapping("/reject-value")
    fun rejectValue(@RequestParam id: Int, redirectAttributes: RedirectAttributes): String {
        if (!ldapService.isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to use this page")
            redirectAttributes.addFlashAttribute("error", true)
        }
        val requestedValue = fieldRequestedValuesRepository.findById(id).orElse(null) ?: seqTypeRequestedValuesRepository.getOne(id)
        try {
            requestedValueService.rejectValue(requestedValue)
            redirectAttributes.addFlashAttribute("successMessage", "Value rejected")
            redirectAttributes.addFlashAttribute("success", true)
        } catch (e: DataIntegrityViolationException) {
            redirectAttributes.addFlashAttribute("errorMessage", "There were samples found that use this seqType but couldn't be corrected.")
            redirectAttributes.addFlashAttribute("error", true)
        }
        return "redirect:/admin/requested-values/overview".takeIf { requestedValue !is SeqTypeRequestedValue } ?: "redirect:/metadata-input/sequencing-type"
    }

    private fun getResultMap(requestedValues: Set<FieldRequestedValue>): Map<String, Any> {
        val result = requestedValues.map {
            mapOf(
                "id" to it.id,
                "fieldName" to it.fieldName,
                "requestedValue" to it.requestedValue,
                "requester" to "${it.requester.fullName} (${it.requester.username})",
                "originSubmission" to it.originSubmission.identifier,
                "usedSubmissions" to it.usedSubmissions
                    .sortedBy { it.identifier }
                    .joinToString { submission -> "<s>${submission.identifier}</s>".takeIf { submission.isFinished } ?: submission.identifier },
                "createdValue" to (it.state.takeIf { it == RequestedValue.State.REJECTED } ?: it.createdValueAs),
                "formattedDateCreated" to it.formattedDateCreated,
                "formattedLastUpdate" to it.formattedLastUpdate
            )
        }

        return mapOf("data" to result)
    }

    @PostMapping("/request-seq-type")
    @ResponseBody
    fun requestSeqType(
        @RequestParam name: String,
        @RequestParam basicSeqType: String,
        @RequestParam(required = false) needAntibodyTarget: Boolean?,
        @RequestParam(required = false) singleCell: Boolean?,
    ): String {
        val seqTypeOptions = listOf("needAntibodyTarget" to needAntibodyTarget, "singleCell" to singleCell)
            .filter { it.second ?: false }
            .joinToString(",") { it.first }
        val savedSeqType = seqTypeMappingService.saveSeqType(
            name = name,
            seqTypeId = null,
            basicSeqType = basicSeqType,
            ilseNames = null,
            seqTypeOptions = "isRequested,$seqTypeOptions"
        )
        return savedSeqType.id.toString()
    }
}
