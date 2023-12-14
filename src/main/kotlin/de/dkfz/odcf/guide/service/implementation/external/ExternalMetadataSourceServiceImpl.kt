package de.dkfz.odcf.guide.service.implementation.external

import com.fasterxml.jackson.core.type.TypeReference
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.exceptions.BlankJsonException
import de.dkfz.odcf.guide.helperObjects.encodeUtf8
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.helperObjects.toKebabCase
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExternalMetadataSourceServiceImpl(
    private val jsonApiService: JsonApiService,
    private val ldapService: LdapService,
) : ExternalMetadataSourceService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getSingleValue(methodName: String, params: Map<String, String>): String {
        val paramString = params.map { "${it.key.encodeUtf8()}=${it.value.encodeUtf8()}" }.joinToString("&")
        return jsonApiService.getJsonFromApi(methodName.toKebabCase() + "?$paramString".takeIf { paramString.isNotBlank() }.orEmpty(), ApiType.OTP)
    }

    override fun getValuesAsSet(methodName: String, params: Map<String, String>): Set<String> {
        return try {
            return jsonApiService.getValues(methodName, params, ApiType.OTP, object : TypeReference<List<String>>() {}).sorted().toSet()
        } catch (e: BlankJsonException) {
            logger.warn(e.message)
            emptySet()
        }
    }

    override fun getValuesAsSetMap(methodName: String, params: Map<String, String>): Set<Map<String, String>> {
        return try {
            jsonApiService.getValues(methodName, params, ApiType.OTP, object : TypeReference<Set<Map<String, String>>>() {})
        } catch (e: BlankJsonException) {
            logger.warn(e.message)
            emptySet()
        }
    }

    override fun getPrincipalInvestigatorsAsPersonSet(projectName: String): Set<Person> {
        return getValuesAsSet("pis-by-project", mapOf("project" to projectName)).mapNotNull { username ->
            try {
                ldapService.getPersonByUsername(username)
            } catch (e: Exception) {
                logger.warn("user '$username' not added as PI to project '$projectName' with reason:\n${e.localizedMessage}")
                null
            }
        }.toSet()
    }
}
