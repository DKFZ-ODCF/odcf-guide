package de.dkfz.odcf.guide.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import de.dkfz.odcf.guide.ProjectRepository
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.IlseApiService
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*

@Controller
@RequestMapping("/services")
class ExternalServicesController(
    private val authorizationService: AuthorizationService,
    private val projectRepository: ProjectRepository,
    private val ilseService: IlseApiService,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
) {
    val bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @GetMapping("/ilse/api")
    @ResponseBody
    fun getContentFromIlseApi(
        @RequestParam("ilse") ilse: Int,
        @RequestHeader(value = "User-Token", required = false) token: String?
    ): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }

        return try {
            val ilseObject = ilseService.getSubmissionImportObjectFromApi(ilse)
            ResponseEntity(ilseObject, HttpHeaders(), HttpStatus.OK)
        } catch (e: ExternalApiReadException) {
            ResponseEntity(bundle.getString("import.ilse.manualImport.notReadable").replace("{0}", "$ilse"), HttpHeaders(), HttpStatus.BAD_REQUEST)
        } catch (e: JsonProcessingException) {
            ResponseEntity(bundle.getString("import.ilse.manualImport.notParsable").replace("{0}", "$ilse"), HttpHeaders(), HttpStatus.BAD_REQUEST)
        } catch (e: JsonMappingException) {
            ResponseEntity(bundle.getString("import.ilse.manualImport.notParsable").replace("{0}", "$ilse"), HttpHeaders(), HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/otp/get-users-to-be-notified")
    @ResponseBody
    fun getUsersToBeNotified(
        @RequestParam("project") projectName: String,
        @RequestHeader(value = "User-Token", required = false) token: String?
    ): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }

        val mailAdresses = externalMetadataSourceService.getSetOfValues("usersToBeNotifiedByProject", mapOf("project" to projectName)).joinToString("\n")
        val headers = HttpHeaders()
        headers.add("Content-Type", TEXT_PLAIN_VALUE)
        val payload = StringBuilder()
        payload.append("#".repeat(60)).append("\n").append(mailAdresses).append("\n").append("#".repeat(60))
        return ResponseEntity(payload, headers, HttpStatus.OK)
    }

    @GetMapping("/get-project-path")
    @ResponseBody
    fun getProjectPath(
        @RequestParam("project") projectName: String,
        @RequestHeader(value = "User-Token", required = false) token: String?
    ): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }

        val path = projectRepository.findByName(projectName)?.pathProjectFolder.orEmpty()
        val headers = HttpHeaders()
        headers.add("Content-Type", TEXT_PLAIN_VALUE)
        return ResponseEntity(path, headers, HttpStatus.OK)
    }
}
