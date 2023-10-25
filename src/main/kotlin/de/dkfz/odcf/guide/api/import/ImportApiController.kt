package de.dkfz.odcf.guide.api.import

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import de.dkfz.odcf.guide.RoleRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.service.interfaces.importer.IlseImportService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ImportApiController(
    private val importService: ImportService,
    private val ilseImportService: IlseImportService,
    private val mailSenderService: MailSenderService,
    private val authorizationService: AuthorizationService,
    private val submissionRepository: SubmissionRepository,
    private val roleRepository: RoleRepository
) : ImportApiInterface {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun importIlse(identifier: Int, body: String?, token: String): ResponseEntity<String> {
        val role = roleRepository.findByName("ILSE_IMPORT")
        authorizationService.checkIfTokenIsAuthorized(token, role)?.let { return it }

        logger.debug("Content from Import\n\nIdentifier: $identifier\nBody: $body")
        val submission: ApiSubmission
        try {
            submission = ilseImportService.import(identifier, "") as ApiSubmission
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
            val submission1 = submissionRepository.findByIdentifier(importService.generateIlseIdentifier(identifier))!!
            return ResponseEntity(submission1.uuid.toString(), HttpHeaders(), HttpStatus.ALREADY_REPORTED)
        }
        sendMails(submission)
        return ResponseEntity(submission.uuid.toString(), HttpHeaders(), HttpStatus.CREATED)
    }

    private fun sendMails(submission: Submission) {
        try {
            if (submission.isFinished) {
                mailSenderService.sendFinallySubmittedMail(submission, includeSubmissionReceived = true)
            } else {
                mailSenderService.sendReceivedSubmissionMail(submission, sendToUser = true)
            }
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
        }
    }
}
