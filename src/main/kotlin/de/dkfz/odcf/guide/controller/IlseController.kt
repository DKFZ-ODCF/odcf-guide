package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.helperObjects.JsonSeqTypeTranslationRequestObject
import de.dkfz.odcf.guide.service.interfaces.ReminderService
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.TerminationService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/ilse")
class IlseController(
    private val submissionRepository: SubmissionRepository,
    private val collectorService: CollectorService,
    private val submissionService: SubmissionService,
    private val terminationService: TerminationService,
    private val seqTypeMappingService: SeqTypeMappingService,
    private val reminderService: ReminderService,
    private val env: Environment
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/set-data-is-available")
    @ResponseBody
    fun setIlseDataIsAvailable(
        @RequestParam("ilse") ilse: Int,
        @RequestParam(required = false, name = "secret") secret: String?
    ): ResponseEntity<*> {

        if (secret == null || secret != env.getProperty("export.secret")) {
            return ResponseEntity("Wrong secret!\n", HttpHeaders(), HttpStatus.FORBIDDEN)
        }

        val submission = submissionRepository.findByIdentifier(String.format("i%07d", ilse))
        if (submission != null) {
            if (submission.externalDataAvailableForMerging) {
                return ResponseEntity(
                    "Data availability for submission with ILSE ID " +
                        collectorService.getFormattedIdentifier(submission.identifier) +
                        " was already reported.\n",
                    HttpHeaders(), HttpStatus.ACCEPTED
                )
            }
            submissionService.setExternalDataAvailableForMerging(submission, true, null)

            if (submission.isFinished && !submission.isAutoClosed) {
                submissionService.postProceedWithSubmission(submission)
            } else {
                if (submission.isActive && !submission.isPaused) {
                    reminderService.sendDataReceivedReminderMail(submission)
                } else {
                    logger.info("Did not trigger merging for submission ${collectorService.getFormattedIdentifier(submission.identifier)} since submission has status ${submission.status}. [trigger event was: set-data-is-available]")
                }
            }
            return ResponseEntity(
                "Data availability for submission with ILSE ID " +
                    collectorService.getFormattedIdentifier(submission.identifier) +
                    " has been logged.\n",
                HttpHeaders(), HttpStatus.ACCEPTED
            )
        } else {
            return ResponseEntity("Did not find submission with ILSE ID $ilse\n", HttpHeaders(), HttpStatus.CONFLICT)
        }
    }

    @GetMapping("/check-ilse-data-state")
    @ResponseBody
    fun checkIlseDataIsAvailable(
        @RequestParam("ilse") ilse: Int,
        @RequestParam(required = false, name = "secret") secret: String?
    ): ResponseEntity<*> {

        if (secret == null || secret != env.getProperty("export.secret")) {
            return ResponseEntity("Wrong secret!\n", HttpHeaders(), HttpStatus.FORBIDDEN)
        }

        val submission = submissionRepository.findByIdentifier(String.format("i%07d", ilse))
        return if (submission != null) {
            val dataAvailable = submission.externalDataAvailableForMerging
            ResponseEntity(dataAvailable, HttpHeaders(), HttpStatus.ACCEPTED)
        } else {
            ResponseEntity("Did not find submission with ILSE ID $ilse\n", HttpHeaders(), HttpStatus.CONFLICT)
        }
    }

    @PostMapping(path = ["/terminate"])
    @ResponseBody
    fun terminate(
        @RequestParam("ilse") ilse: Int,
        @RequestParam(required = false, name = "secret") secret: String?
    ): ResponseEntity<*> {

        if (secret == null || secret != env.getProperty("export.secret")) {
            return ResponseEntity("Wrong secret!\n", HttpHeaders(), HttpStatus.FORBIDDEN)
        }

        val submission = submissionRepository.findByIdentifier(String.format("i%07d", ilse))
            ?: return ResponseEntity("Did not find submission ($ilse)\n", HttpHeaders(), HttpStatus.CONFLICT)
        if (submission.isRemovedByAdmin) {
            return ResponseEntity(
                "Submission (" + collectorService.getFormattedIdentifier(submission.identifier) +
                    ") was already deleted. Will not set it to terminated!\n",
                HttpHeaders(), HttpStatus.CONFLICT
            )
        }
        terminationService.terminateSubmission(submission, "terminatedMailSubjectAutomatic")
        return ResponseEntity(
            "Submission (" + collectorService.getFormattedIdentifier(submission.identifier) +
                ") has been terminated. \n",
            HttpHeaders(), HttpStatus.ACCEPTED
        )
    }

    @GetMapping("/get-validation-finished")
    @ResponseBody
    fun getValidationFinished(
        @RequestParam("ilse") ilse: Int,
        @RequestParam(required = false, name = "secret") secret: String?
    ): ResponseEntity<*> {

        if (secret == null || secret != env.getProperty("export.secret")) {
            return ResponseEntity("Wrong secret!\n", HttpHeaders(), HttpStatus.FORBIDDEN)
        }

        val identifier = String.format("i%07d", ilse)
        val submission = submissionRepository.findByIdentifier(identifier)

        return if (submission != null) {
            val validationFinished = submission.isFinished
            ResponseEntity(validationFinished, HttpHeaders(), HttpStatus.ACCEPTED)
        } else {
            ResponseEntity("Did not find submission with ILSE ID $ilse\n", HttpHeaders(), HttpStatus.CONFLICT)
        }
    }

    @PostMapping("/translate-ilse-seqtype")
    @ResponseBody
    fun getIlseSeqType(@RequestBody json: JsonSeqTypeTranslationRequestObject): ResponseEntity<*> {

        if (json.secret == null || json.secret != env.getProperty("export.secret")) {
            return ResponseEntity("Wrong secret!\n", HttpHeaders(), HttpStatus.FORBIDDEN)
        }

        val seqType = seqTypeMappingService.getSeqType(json.gpcfSeqType!!)
        val seqTypeMap = HashMap<String, String>()
        seqTypeMap["BasicSeqType"] = seqType?.basicSeqType.toString()
        seqTypeMap["isSingleCell"] = seqType?.singleCell.toString()
        seqTypeMap["isTagmentation"] = seqType?.tagmentation.toString()

        return ResponseEntity(seqTypeMap, HttpHeaders(), HttpStatus.CREATED)
    }
}
