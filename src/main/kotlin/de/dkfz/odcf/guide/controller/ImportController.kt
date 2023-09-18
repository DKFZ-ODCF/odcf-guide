package de.dkfz.odcf.guide.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_READ_ONLY
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_TABLE_PAGE_ADMIN
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.*
import de.dkfz.odcf.guide.helperObjects.importObjects.IlseSubmissionImportObject
import de.dkfz.odcf.guide.helperObjects.importObjects.SubmissionImportObject
import de.dkfz.odcf.guide.service.deprecated.JsonImportService
import de.dkfz.odcf.guide.service.interfaces.*
import de.dkfz.odcf.guide.service.interfaces.importer.CsvImportService
import de.dkfz.odcf.guide.service.interfaces.importer.IlseImportService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.view.RedirectView
import java.io.IOException
import java.util.*

@Controller
@RequestMapping("/importer")
class ImportController(
    private val importService: ImportService,
    private val csvImportService: CsvImportService,
    private val ilseImportService: IlseImportService,
    private val jsonImportService: JsonImportService,
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val collectorService: CollectorService,
    private val mergingService: MergingService,
    private val ldapService: LdapService,
    private val sampleRepository: SampleRepository,
    private val submissionService: SubmissionService,
    private val submissionRepository: SubmissionRepository,
    private val deletionService: DeletionService,
) {

    val bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/ilse")
    fun importSubmissionByIlse(@RequestBody json: IlseSubmissionImportObject): ResponseEntity<*> {
        return importAndSendMails(json, json.getIlseId())
    }

    @GetMapping("/api")
    fun importSubmissionByIlseApi(
        @RequestParam("ilse") ilse: Int,
        @RequestParam("ticketNumber") ticketNumber: String
    ): ResponseEntity<*> {
        val submission: ApiSubmission
        try {
            submission = ilseImportService.import(ilse, ticketNumber)!! as ApiSubmission
            ilseImportService.summariesAfterImport(submission)
        } catch (e: DuplicateKeyException) {
            return ResponseEntity(e.message, HttpHeaders(), HttpStatus.CONFLICT)
        } catch (e: ExternalApiReadException) {
            return ResponseEntity("We cannot read this ILSe\n${e.message}", HttpHeaders(), HttpStatus.BAD_REQUEST)
        } catch (e: JsonProcessingException) {
            return ResponseEntity(e.message, HttpHeaders(), HttpStatus.BAD_REQUEST)
        } catch (e: JsonMappingException) {
            return ResponseEntity(e.message, HttpHeaders(), HttpStatus.BAD_REQUEST)
        } catch (e: IllegalStateException) {
            return ResponseEntity(e.message, HttpHeaders(), HttpStatus.FORBIDDEN)
        } catch (e: DataIntegrityViolationException) {
            val submission1 = submissionRepository.findByIdentifier(importService.generateIlseIdentifier(ilse))!!
            return ResponseEntity(sampleRepository.findBySubmission(submission1)[0].project + "\n", HttpHeaders(), HttpStatus.OK)
        }
        return sendMails(submission)
    }

    @PostMapping("/gui-api")
    fun guiImportSubmissionByIlseApi(
        @RequestParam("ilse") ilse: Int,
        @RequestParam("ticketNumber") ticketNumber: String,
        @RequestParam("notify", required = false) notify: Boolean,
        @RequestParam("dataAvailable", required = false) dataAvailable: Boolean,
        redirectAttributes: RedirectAttributes
    ): RedirectView {

        var submission: Submission
        var successMessage: String
        var jsonExtractionMailSubject: String
        try {
            submission = ilseImportService.import(ilse, ticketNumber)!! as ApiSubmission
            ilseImportService.summariesAfterImport(submission)
        } catch (e: ExternalApiReadException) {
            return showError(redirectAttributes, ilse, ticketNumber, "import.ilse.manualImport.notReadable", e.message.orEmpty())
        } catch (e: JsonProcessingException) {
            return showError(redirectAttributes, ilse, ticketNumber, "import.ilse.manualImport.notParsable", e.message.orEmpty())
        } catch (e: JsonMappingException) {
            return showError(redirectAttributes, ilse, ticketNumber, "import.ilse.manualImport.notParsable", e.message.orEmpty())
        } catch (e: DuplicateKeyException) {
            submission = submissionRepository.findByIdentifier(importService.generateIlseIdentifier(ilse))!!
            redirectAttributes.addFlashAttribute("warning", true)
            redirectAttributes.addFlashAttribute(
                "warningMessage",
                bundle.getString("import.ilse.manualImport.already")
                    .replace("{0}", collectorService.getFormattedIdentifier(submission.identifier))
            )
            return when {
                submission.isWriteProtected -> RedirectView("$SIMPLE_READ_ONLY?uuid=${submission.uuid}", false)
                else -> RedirectView(SIMPLE_TABLE_PAGE_ADMIN + "?identifier=" + submission.identifier, false)
            }
        } catch (e: DataIntegrityViolationException) {
            submission = submissionRepository.findByIdentifier(importService.generateIlseIdentifier(ilse))!!
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                bundle.getString("import.ilse.manualImport.somethingWentWrong")
                    .replace("{0}", collectorService.getFormattedIdentifier(submission.identifier))
            )
            redirectAttributes.addFlashAttribute("error", true)
            return when {
                submission.isWriteProtected -> RedirectView("$SIMPLE_READ_ONLY?uuid=${submission.uuid}", false)
                else -> RedirectView(SIMPLE_TABLE_PAGE_ADMIN + "?identifier=" + submission.identifier, false)
            }
        } catch (e: NullPointerException) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                bundle.getString("import.ilse.manualImport.notReadable")
                    .replace("{0}", ilse.toString())
            )
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("ilseId", ilse)
            redirectAttributes.addFlashAttribute("ticketNumber", ticketNumber)
            return RedirectView("/metadata-input/ilse-import", false)
        }

        successMessage = "Successfully imported ${submission.identifier} via ILSe API."
        if (dataAvailable) {
            submissionService.setExternalDataAvailableForMerging(submission, dataAvailable, submission.importDate)
            if (!submission.isAutoClosed) {
                // JSON EXTRACTION
                jsonExtractionMailSubject = "${mailContentGeneratorService.getTicketSubjectPrefix(submission)} Validation Service: "
                val jsonExtractionResponse: String
                try {
                    jsonExtractionResponse = mergingService.runJsonExtractorScript(submission)
                } catch (e: JsonExtractorException) {
                    submission = submissionRepository.findByIdentifier(importService.generateIlseIdentifier(ilse))!!
                    val errorMessage = "Could not run JsonExtraction for submission ${submission.identifier}"
                    jsonExtractionMailSubject += errorMessage
                    mailSenderService.sendMailToTicketSystem(jsonExtractionMailSubject, e.message!!)
                    redirectAttributes.addFlashAttribute("error", true)
                    redirectAttributes.addFlashAttribute("errorMessage", "$errorMessage:\n${e.message}")
                    redirectAttributes.addFlashAttribute("ilseId", ilse)
                    redirectAttributes.addFlashAttribute("ticketNumber", ticketNumber)
                    return RedirectView(SIMPLE_TABLE_PAGE_ADMIN + "?identifier=" + submission.identifier, false)
                }

                if (jsonExtractionResponse.startsWith("Did not trigger")) {
                    submission = submissionRepository.findByIdentifier(importService.generateIlseIdentifier(ilse))!!
                    val errorMessage = "Should not run JsonExtraction for submission ${submission.identifier}"
                    jsonExtractionMailSubject += jsonExtractionResponse
                    mailSenderService.sendMailToTicketSystem(jsonExtractionMailSubject, jsonExtractionResponse)
                    redirectAttributes.addFlashAttribute("error", true)
                    redirectAttributes.addFlashAttribute("errorMessage", "$errorMessage:\n$jsonExtractionResponse")
                    redirectAttributes.addFlashAttribute("ilseId", ilse)
                    redirectAttributes.addFlashAttribute("ticketNumber", ticketNumber)
                    return RedirectView(SIMPLE_TABLE_PAGE_ADMIN + "?identifier=" + submission.identifier, false)
                }

                successMessage += "\nJsonExtraction has been performed successfully as well."
                jsonExtractionMailSubject += "Successfully ran JsonExtraction for manually API-imported submission ${submission.identifier}"
                mailSenderService.sendMailToTicketSystem(jsonExtractionMailSubject, "Json extracted file:\n$jsonExtractionResponse")
            }
        }

        val redirectView = RedirectView(SIMPLE_TABLE_PAGE_ADMIN + "?identifier=" + submission.identifier, true)
        redirectAttributes.addFlashAttribute("successMessage", successMessage)
        redirectAttributes.addFlashAttribute("success", true)

        try {
            if (submission.isFinished) {
                mailSenderService.sendFinallySubmittedMail(submission, includeSubmissionReceived = true)
            } else {
                mailSenderService.sendReceivedSubmissionMail(submission, sendToUser = notify)
            }
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
            redirectAttributes.addFlashAttribute("warning", true)
            redirectAttributes.addFlashAttribute("warningMessage", "Imported, but can not send mail(s)")
        }

        return redirectView
    }

    @PostMapping("/internal")
    fun importSubmissionByIdentifier(@RequestBody json: SubmissionImportObject): ResponseEntity<*> {
        return importAndSendMails(json, importService.generateInternalIdentifier())
    }

    @PostMapping(value = ["/upload"], consumes = ["multipart/form-data"])
    @Throws(IOException::class)
    fun uploadMultipart(
        @RequestParam file: MultipartFile,
        @RequestParam(required = false) ticket: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam customName: String,
        @RequestParam comment: String,
        @RequestParam button: String?,
        @RequestParam(required = false) submissions: List<String>?,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = ldapService.getPerson()
        val errorPageSuffix = if (user.isAdmin) "admin" else "user"
        return try {
            var ignoreMd5Check = false
            when (button) {
                "Continue" ->
                    ignoreMd5Check = true
                "Continue and delete selected" -> {
                    ignoreMd5Check = true
                    submissions?.forEach {
                        deletionService.deleteSubmission(submissionRepository.findByIdentifier(it)!!)
                    }
                }
            }
            csvImportService.import(file, ticket.orEmpty(), email ?: user.mail, customName, comment, ignoreMd5Check)

            redirectAttributes.addFlashAttribute("success", true)
            redirectAttributes.addFlashAttribute("successMessage", "We have received your submission. We will send you an email when everything is ready and you can start editing.")
            if (user.isAdmin) {
                "redirect:/metadata-validator/overview/uploaded/admin"
            } else {
                "redirect:/metadata-validator/overview/uploaded/user"
            }
        } catch (io: IOException) {
            redirectAttributes.addFlashAttribute("error", true)
            if (io.localizedMessage == "Fastq filename not found.")
                redirectAttributes.addFlashAttribute("errorMessage", io.localizedMessage + " Unable to read file. Please ensure that it is a valid TSV file.")
            else
                redirectAttributes.addFlashAttribute("errorMessage", "Unable to read file. Please ensure that it is a valid TSV file.")
            redirectAttributes.addFlashAttribute("admin", user.isAdmin)
            "redirect:/metadata-validator/overview/uploaded/$errorPageSuffix"
        } catch (md5fids: Md5SumFoundInDifferentSubmission) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", md5fids.localizedMessage)
            redirectAttributes.addFlashAttribute("duplicatedMd5", true)
            redirectAttributes.addFlashAttribute("duplicatedMd5Message", md5fids.localizedMessage)
            redirectAttributes.addFlashAttribute("submissions", md5fids.submissions)
            redirectAttributes.addFlashAttribute("countMd5InSubmissions", md5fids.countMd5InSubmissions)
            redirectAttributes.addFlashAttribute("admin", user.isAdmin)
            "redirect:/metadata-validator/overview/uploaded/$errorPageSuffix"
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", e.localizedMessage ?: "Unexpected error occurred")
            redirectAttributes.addFlashAttribute("admin", user.isAdmin)
            "redirect:/metadata-validator/overview/uploaded/$errorPageSuffix"
        }
    }

    /*@PostMapping("/midterm")
    fun importSubmissionFromMidterm(
        @RequestParam ilse: Int,
        @RequestParam token: String
    ): ResponseEntity<*> {
        val importUser = personRepository.findByApiToken(token)
        return if (importUser?.isAdmin == true) {
            val subject = bundle.getString("import.subject")
            val (submission, warnings) = try {
                csvImportService.importAdditional(ilse)
            } catch (e: GuideMergerException) {
                mailSenderService.sendMailToTicketSystem(
                    subject,
                    bundle.getString("import.error")
                        .replace("{0}", importService.generateIlseIdentifier(ilse))
                        .replace("{1}", e.message!!)
                )
                return ResponseEntity(e.message, HttpHeaders(), HttpStatus.BAD_REQUEST)
            }
            val validationErrors = emptyMap<Int, Map<String, Boolean>>().toMutableMap()
//            validationErrors.putAll(sampleService.validateSamples(submission.samples!!))
//            validationErrors.putAll(sampleService.validateFiles(fileRepository.findAllBySampleIn(submission.samples!!)))

            var body = if (validationErrors.isEmpty() && submission.isFinished) {
                val importLinks = fileService.writeLongTsvFile(submission).split("\n").map {
                    runtimeOptionsRepository.findByName("otpImportLink")!!.value
                        .replace("TICKET_NUMBER", submission.ticketNumber)
                        .replace("FILE_PATH", it)
                }
                bundle.getString("import.allDone")
                    .replace("{1}", submission.identifier)
                    .replace("{2}", importLinks.joinToString("\n"))
            } else if (!submission.isFinished) {
                bundle.getString("import.notFinishedByUser")
            } else {
                bundle.getString("import.validationErrors")
            }
            body += if (warnings.isNotEmpty()) {
                "\n" + bundle.getString("merging.warnings").replace("{1}", warnings.joinToString("\n"))
            } else {
                "\n" + bundle.getString("merging.allFine")
            }
            mailSenderService.sendMailToTicketSystem(subject, body)
            ResponseEntity(submission, HttpHeaders(), HttpStatus.OK)
        } else {
            ResponseEntity("You are not allowed to import", HttpHeaders(), HttpStatus.FORBIDDEN)
        }
    }*/

    private fun importAndSendMails(json: SubmissionImportObject, identifier: String): ResponseEntity<*> {
        val submission: Submission
        try {
            submission = jsonImportService.import(json, identifier)
        } catch (e: DuplicateKeyException) {
            return ResponseEntity(e.message, HttpHeaders(), HttpStatus.CONFLICT)
        } catch (e: DataIntegrityViolationException) {
            val submission1 = submissionRepository.findByIdentifier(identifier)!!
            return ResponseEntity(sampleRepository.findBySubmission(submission1)[0].project + "\n", HttpHeaders(), HttpStatus.OK)
        }
        return sendMails(submission)
    }

    private fun sendMails(submission: Submission): ResponseEntity<*> {
        try {
            if (submission.isFinished) {
                mailSenderService.sendFinallySubmittedMail(submission, includeSubmissionReceived = true)
            } else {
                mailSenderService.sendReceivedSubmissionMail(submission, sendToUser = true)
            }
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
            return ResponseEntity("Imported, but can not send mail(s)", HttpHeaders(), HttpStatus.PARTIAL_CONTENT)
        }
        return ResponseEntity(sampleRepository.findBySubmission(submission).map { it.project }.toSet().joinToString() + "\n", HttpHeaders(), HttpStatus.OK)
    }

    private fun showError(redirectAttributes: RedirectAttributes, ilse: Int, ticketNumber: String, errorKey: String, error: String): RedirectView {
        val bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

        redirectAttributes.addFlashAttribute(
            "errorMessage",
            "${bundle.getString(errorKey).replace("{0}", ilse.toString())}\nThe exception message was: '$error'"
        )
        redirectAttributes.addFlashAttribute("error", true)
        redirectAttributes.addFlashAttribute("ilseId", ilse)
        redirectAttributes.addFlashAttribute("ticketNumber", ticketNumber)
        return RedirectView("/metadata-input/ilse-import", false)
    }
}
