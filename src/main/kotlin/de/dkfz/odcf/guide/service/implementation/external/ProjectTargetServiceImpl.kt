package de.dkfz.odcf.guide.service.implementation.external

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.dkfz.odcf.guide.exceptions.BlankJsonException
import de.dkfz.odcf.guide.helperObjects.encodeUtf8
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.helperObjects.toKebabCase
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.external.ProjectTargetService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ProjectTargetServiceImpl(
    private val jsonApiService: JsonApiService,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
) : ProjectTargetService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun sendValues(methodName: String, params: Map<String, String>): String {
        val paramString = params.map { (key, value) ->
            "${key.encodeUtf8()}=${value.encodeUtf8()}"
        }.joinToString("&")
        return jsonApiService.sendJsonToApi(methodName.toKebabCase() + "?$paramString".takeIf { paramString.isNotBlank() }.orEmpty(), ApiType.PROJECT_TARGET_SERVICE)
    }

    override fun getSingleValueFromSet(methodName: String, params: Map<String, Set<String>>): String {
        val paramString = params.flatMap { (key, paramList) ->
            paramList.map { value ->
                "${key.encodeUtf8()}=${value.encodeUtf8()}"
            }
        }.joinToString("&")
        val encodedUrl = methodName.toKebabCase() + "?$paramString".takeIf { paramString.isNotBlank() }.orEmpty()

        return jsonApiService.getJsonFromApi(encodedUrl, ApiType.PROJECT_TARGET_SERVICE)
    }

    override fun getSetOfValuesFromSet(methodName: String, params: Map<String, Set<String>>): Set<String> {
        val json = getSingleValueFromSet(methodName.toKebabCase(), params)
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(json, object : TypeReference<Set<String>>() {}).sorted().toSet()
    }

    @Scheduled(cron = "\${projectTargetService.adapter.cron}") // 0 0 1 * * *: At 01:00 every day
    override fun updateProjectsInTarget(): Boolean {
        val otpProjects = externalMetadataSourceService.getValuesAsSet("projects-after-year", mapOf("year" to "2023"))
        val ilseProjects = try {
            jsonApiService.getValues("project-names", apiType = ApiType.PROJECT_TARGET_SERVICE, typeReference = object : TypeReference<Set<String>>() {})
        } catch (e: BlankJsonException) {
            logger.warn(e.message)
            return false
        }
        if (otpProjects.isEmpty()) {
            logger.warn("There was a problem while trying to get the project lists, did not update projects in target.")
            return false
        }
        val projectsToAdd = otpProjects - ilseProjects

        projectsToAdd.forEach { project ->
            val pis = externalMetadataSourceService.getPrincipalInvestigatorsAsPersonSet(project).joinToString { it.fullName }
            val affectedRows = sendValues("insert-into-project-view", mapOf("project" to project, "description" to pis))
            if (affectedRows.toIntOrNull() != 1) {
                logger.warn("There was a problem while sending project '$project' to the projectTargetService.")
            }
        }
        return true
    }
}
