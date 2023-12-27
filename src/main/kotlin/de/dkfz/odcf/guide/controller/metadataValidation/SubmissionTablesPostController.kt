package de.dkfz.odcf.guide.controller.metadataValidation

import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.EXTENDED_READ_ONLY
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_READ_ONLY
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.Md5SumsDontMatchException
import de.dkfz.odcf.guide.exceptions.RowNotFoundException
import de.dkfz.odcf.guide.exceptions.SampleNamesDontMatchException
import de.dkfz.odcf.guide.helperObjects.ControllerHelper
import de.dkfz.odcf.guide.helperObjects.SampleForm
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.ParserService
import de.dkfz.odcf.guide.service.interfaces.importer.CsvImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.SampleService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.IOException
import java.util.*

@Controller
@RequestMapping("/metadata-validator/submission")
class SubmissionTablesPostController(
    private val submissionRepository: SubmissionRepository,
    private val submissionService: SubmissionService,
    private val sampleService: SampleService,
    private val mailService: MailSenderService,
    private val ldapService: LdapService,
    private val collectorService: CollectorService,
    private val fileService: FileService,
    private val csvImportService: CsvImportService,
    private val parserService: ParserService,
    private val env: Environment
) {

    private val controllerHelper = ControllerHelper(ldapService)

    @InitBinder
    fun initBinder(binder: WebDataBinder) {
        binder.autoGrowCollectionLimit = 2048
    }

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @PostMapping("/simple/next", "/extended/next")
    fun updateExtendedMataDataValidator(
        @ModelAttribute form: SampleForm,
        @RequestParam button: String,
        @RequestParam submissionIdentifier: String,
        @RequestParam(required = false) noFieldConstraints: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        return handleForm(form, button, submissionIdentifier, noFieldConstraints, redirectAttributes)
    }

    private fun handleForm(
        form: SampleForm,
        button: String,
        submissionIdentifier: String,
        noFieldConstraints: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        val submission = submissionRepository.findByIdentifier(submissionIdentifier)!!

        if (button == "Leave session without saving") {
            return leaveWithoutSaving(submission)
        }
        if (submission.isWriteProtected) {
            return buttonActions(submission, "isWriteProtected", false, redirectAttributes)
        }
        sampleService.updateSamples(submission, form)
        if (!noFieldConstraints) {
            var errors = sampleService.validateSamples(form, submission.validationLevel)
            if (form.sampleList?.any { !it.files.isNullOrEmpty() } == true) {
                errors = errors.toMutableMap()
                errors.putAll(sampleService.validateFiles(form, submission.validationLevel))
            }
            if (errors.isNotEmpty()) {
                redirectAttributes.addFlashAttribute("validationErrors", errors)
                redirectAttributes.addFlashAttribute("wasValidated", true)
                redirectAttributes.addFlashAttribute("error", true)
                redirectAttributes.addFlashAttribute("errorMessage", "Some errors were found in the data submitted. Please check again.")
                return controllerHelper.getRedirectPage(submission)
            }
        }
        return buttonActions(submission, button, form.sampleList.orEmpty().any { !it.files.isNullOrEmpty() }, redirectAttributes)
    }

    private fun leaveWithoutSaving(submission: Submission): String {
        val user = ldapService.getPerson()
        if (!user.isAdmin) {
            submissionService.changeSubmissionState(submission, Submission.Status.UNLOCKED, user.username)
        }
        return "redirect:/metadata-validator/overview/" + "uploaded/".takeIf { submission.isExtended }.orEmpty()
    }

    private fun buttonActions(
        submission: Submission,
        actionName: String,
        extended: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {

        val redirectReadOnly = if (extended) {
            "redirect:$EXTENDED_READ_ONLY?uuid=${submission.uuid}"
        } else {
            "redirect:$SIMPLE_READ_ONLY?uuid=${submission.uuid}"
        }

        when (actionName) {
            "isWriteProtected" -> {
                redirectAttributes.addFlashAttribute(
                    "header",
                    "We detected this submission has been marked as read-only. " +
                        "Please contact us if you are unable to return to the editing page."
                )
                return redirectReadOnly
            }
            "Save and stay in edit mode" -> submissionService.changeSubmissionState(submission, Submission.Status.EDITED, ldapService.getPerson().username)
            "Save on timeout" -> {
                submissionService.changeSubmissionState(submission, Submission.Status.UNLOCKED, ldapService.getPerson().username)
                redirectAttributes.addFlashAttribute("header", "Session expired for")

                return redirectReadOnly
            }
            "Save and leave edit mode" -> {
                submissionService.changeSubmissionState(submission, Submission.Status.EDITED, ldapService.getPerson().username)
                redirectAttributes.addFlashAttribute("success", true)
                redirectAttributes.addFlashAttribute("successMessage", "Submission has been saved. Please don't forget to complete it later.")
                return "redirect:/metadata-validator/overview/" + "uploaded/".takeIf { submission.isExtended }.orEmpty()
            }
            // Validate and save
            "Next" -> {
                submissionService.changeSubmissionState(submission, Submission.Status.VALIDATED, ldapService.getPerson().username)
                return redirectReadOnly
            }
            "Apply Parser" -> {
                try {
                    parserService.applyParser(submission)
                } catch (e: Exception) {
                    redirectAttributes.addFlashAttribute("warning", true)
                    redirectAttributes.addFlashAttribute("warningMessage", e.message!!)
                }
            }
        }

        return controllerHelper.getRedirectPage(submission)
    }

    @PostMapping(value = ["/simple/next", "/extended/next"], params = ["button=save+export"])
    fun saveAndExportExtended(
        @ModelAttribute form: SampleForm,
        @RequestParam submissionIdentifier: String
    ): ResponseEntity<*> {
        form.sampleList = form.sampleList.orEmpty().filter { it.id != -1 } // samples with -1 are artifacts from data binding
        return createFile(submissionIdentifier, form)
    }

    private fun createFile(submissionIdentifier: String, form: SampleForm): ResponseEntity<*> {
        val submission = submissionRepository.findByIdentifier(submissionIdentifier)!!
        if (!submission.isWriteProtected) {
            sampleService.updateSamples(submission, form)
        }
        val tsvContent = if (submission.isExtended) fileService.createLongTsvFile(
            submission,
            withImportIdentifier = false,
            withExportNames = false
        ) else fileService.createTsvFile(submission)
        val filename = "submission_${if (submission.isExtended) submission.identifier else submission.identifier.substring(1).toInt().toString()}.tsv"

        val responseHeaders = HttpHeaders()
        responseHeaders.add("content-disposition", "attachment; filename=$filename")
        responseHeaders.add("Content-Type", "application/octet-stream")

        return ResponseEntity(tsvContent, responseHeaders, HttpStatus.OK)
    }

    @PostMapping(value = ["/simple/next"], params = ["button=import+parse"])
    fun importAndApplyParser(
        @RequestParam file: MultipartFile,
        @RequestParam submissionIdentifier: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val submission = submissionRepository.findByIdentifier(submissionIdentifier)!!
        return try {
            val redirectPage = importAndSave(file, submissionIdentifier, redirectAttributes)
            parserService.applyParser(submission)
            redirectPage
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("warning", true)
            redirectAttributes.addFlashAttribute("warningMessage", e.message!!)
            controllerHelper.getRedirectPage(submission)
        }
    }

    @PostMapping(value = ["/simple/next"], params = ["button=import+save"])
    fun importAndSave(
        @RequestParam file: MultipartFile,
        @RequestParam submissionIdentifier: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val submission = submissionRepository.findByIdentifier(submissionIdentifier)!!
        if (submission.isWriteProtected) {
            return buttonActions(submission, "isWriteProtected", false, redirectAttributes)
        }

        try {
            val newSamples = fileService.readTsvFile(submission, file)
            sampleService.updateSamples(submission, newSamples)
            submissionService.changeSubmissionState(submission, Submission.Status.EDITED, ldapService.getPerson().username)
        } catch (e: RowNotFoundException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", e.message)
        } catch (e: SampleNamesDontMatchException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", e.message)
        } catch (e: RuntimeJsonMappingException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", e.message!!.substringBefore('(').replace("field", "column"))
        } catch (e: UnrecognizedPropertyException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Unfortunately, we don’t know the column header [${e.propertyName}]. " +
                    "It must match a header from the exported Guide table."
            )
        } catch (e: IOException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to read file. Please ensure that it is a valid TSV file.")
        }
        return controllerHelper.getRedirectPage(submission)
    }

    @PostMapping(value = ["/extended/next"], params = ["button=import+save"])
    fun importAndSaveExtended(
        @RequestParam file: MultipartFile,
        @RequestParam submissionIdentifier: String,
        redirectAttributes: RedirectAttributes
    ): String {
        val submission = submissionRepository.findByIdentifier(submissionIdentifier)!!
        if (submission.isWriteProtected) {
            return buttonActions(submission, "isWriteProtected", false, redirectAttributes)
        }

        try {
            val rows = fileService.readFromCsv(file.inputStream)
            csvImportService.saveFilesAndSamples(submission, rows, override = true)
            submissionService.changeSubmissionState(submission, Submission.Status.EDITED, ldapService.getPerson().username)
        } catch (e: RowNotFoundException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", e.message)
        } catch (e: Md5SumsDontMatchException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", e.message)
        } catch (e: RuntimeJsonMappingException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", e.message!!.substringBefore('(').replace("field", "column"))
        } catch (e: IOException) {
            redirectAttributes.addFlashAttribute("error", true)
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to read file. Please ensure that it is a valid TSV file.")
        }
        return controllerHelper.getRedirectPage(submission)
    }
}
