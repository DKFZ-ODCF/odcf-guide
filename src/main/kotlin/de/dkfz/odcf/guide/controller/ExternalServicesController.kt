package de.dkfz.odcf.guide.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import de.dkfz.odcf.guide.ProjectRepository
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.IlseApiService
import de.dkfz.odcf.guide.service.interfaces.external.ProjectTargetService
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import org.slf4j.LoggerFactory
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
    private val projectTargetService: ProjectTargetService,
) {
    val bundle = ResourceBundle.getBundle("messages", Locale.getDefault())
    private val logger = LoggerFactory.getLogger(this::class.java)

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

        val mailAdresses = externalMetadataSourceService.getValuesAsSet("usersToBeNotifiedByProject", mapOf("project" to projectName)).joinToString("\n")
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

    @PostMapping("/refresh-projects")
    @ResponseBody
    fun refreshProjects(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }

        return if (projectTargetService.updateProjectsInTarget()) {
            logger.info("Successfully updated projects in target.")
            ResponseEntity("Successfully updated projects.\n", HttpHeaders(), HttpStatus.OK)
        } else {
            ResponseEntity("Something went wrong during the update, please try again later or check the logs.\n", HttpHeaders(), HttpStatus.OK)
        }
    }
}
