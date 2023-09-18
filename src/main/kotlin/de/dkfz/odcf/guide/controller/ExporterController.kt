package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.ImportSourceDataRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.OutputFileNotWritableException
import de.dkfz.odcf.guide.helperObjects.exportObjects.IlseJsonExportObject
import de.dkfz.odcf.guide.helperObjects.exportObjects.JsonExportObject
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/exporter")
class ExporterController(
    private val authorizationService: AuthorizationService,
    private val sampleRepository: SampleRepository,
    private val submissionRepository: SubmissionRepository,
    private val submissionService: SubmissionService,
    private val importSourceDataRepository: ImportSourceDataRepository,
    private val fileService: FileService,
    private val env: Environment
) {

    @PostMapping("/ilse")
    fun exportSubmissionByIlse(@RequestBody json: IlseJsonExportObject): ResponseEntity<*> {
        return exportSubmission(json.getIlseId(), json.secret)
    }

    @PostMapping("/internal")
    fun exportSubmissionByIdentifier(@RequestBody json: JsonExportObject): ResponseEntity<*> {
        return exportSubmission(json.identifier, json.secret)
    }

    @PostMapping("/table")
    fun exportSubmissionByIdentifierToTable(@RequestParam identifier: String): ResponseEntity<*> {
        val submission = submissionRepository.findByIdentifier(identifier)!!
        val writtenToReport = try {
            fileService.writeLongTsvFile(submission, true)
        } catch (e: OutputFileNotWritableException) {
            return ResponseEntity(e.message, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return ResponseEntity(writtenToReport, HttpHeaders(), HttpStatus.CREATED)
    }

    @GetMapping("/get-imported-json")
    @ResponseBody
    fun getImportedJson(
        @RequestParam identifier: String,
        @RequestHeader(value = "User-Token", required = false) token: String?
    ): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }

        val importSourceData = importSourceDataRepository.findBySubmissionIdentifier(identifier)
            ?: return ResponseEntity("Did not find import source data for submission with id '$identifier'.\n", HttpStatus.NOT_FOUND)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("application/json")
        return ResponseEntity(importSourceData.jsonContent, headers, HttpStatus.OK)
    }

    private fun exportSubmission(identifier: String?, secret: String?): ResponseEntity<*> {
        if (secret == null || secret != env.getProperty("export.secret")) {
            return ResponseEntity("Wrong secret:\n", HttpHeaders(), HttpStatus.FORBIDDEN)
        }

        val submission = submissionRepository.findByIdentifier(identifier!!)
            ?: return ResponseEntity("Identifier: [$identifier] not found.\n", HttpHeaders(), HttpStatus.CONFLICT)
        if (!submission.isFinished) {
            return ResponseEntity("Identifier: [$identifier] not closed yet.\n", HttpHeaders(), HttpStatus.NOT_ACCEPTABLE)
        }

        submissionService.changeSubmissionState(submission, Submission.Status.EXPORTED, null)
        return ResponseEntity(sampleRepository.findBySubmissionOrderByIdAsc(submission).filter { !it.isStopped }, HttpHeaders(), HttpStatus.CREATED)
    }

    @GetMapping(value = ["/download/metadata-template"])
    fun downloadMetadataTemplate(): ResponseEntity<*> {
        val template = fileService.createMetadataTemplate()
        val filename = "metadataTemplate.tsv"

        val responseHeaders = HttpHeaders()
        responseHeaders.add("content-disposition", "attachment; filename=$filename")
        responseHeaders.add("Content-Type", "application/octet-stream")

        return ResponseEntity(template, responseHeaders, HttpStatus.OK)
    }
}
