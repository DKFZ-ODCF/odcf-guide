package de.dkfz.odcf.guide.service.implementation.external

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helperObjects.toKebabCase
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class ExternalMetadataSourceServiceImpl(
    private val env: Environment,
    private val jsonApiService: JsonApiService,
) : ExternalMetadataSourceService {

    /**
     * This method sends a GET request to an external API endpoint to retrieve JSON content.
     * The URL and headers are constructed using the properties defined in the application.properties file and the specified `urlSuffix`.
     * The maximum file size is 10MB.
     * If an exception occurs during the retrieval, including when the response content is empty, an empty string is returned.
     *
     * @param urlSuffix the name of the metadata to be retrieved.
     * @return The metadata as a JSON string.
     */
    fun getJsonFromApi(urlSuffix: String): String {
        val url = "${env.getRequiredProperty("externalMetadataSourceService.adapter.url")}/rest/otp/$urlSuffix"
        val headers = mapOf(
            "User-Token" to env.getRequiredProperty("externalMetadataSourceService.adapter.token")
        )
        return try {
            jsonApiService.getJsonFromApi(url, headers, ApiType.OTP)
        } catch (e: ExternalApiReadException) {
            ""
        }
    }

    override fun getSingleValue(methodName: String, params: Map<String, String>): String {
        val paramString = params.map { "${it.key}=${it.value}" }.joinToString("&")
        return getJsonFromApi(methodName.toKebabCase() + "?$paramString".takeIf { paramString.isNotBlank() }.orEmpty())
    }

    override fun getValuesAsSet(methodName: String, params: Map<String, String>): Set<String> {
        return getValues(methodName, params, object : TypeReference<List<String>>() {}).sorted().toSet()
    }

    override fun getValuesAsSetMap(methodName: String, params: Map<String, String>): Set<Map<String, String>> {
        return getValues(methodName, params, object : TypeReference<Set<Map<String, String>>>() {})
    }

    override fun <R> getValues(methodName: String, params: Map<String, String>, typeReference: TypeReference<R>): R {
        val json = getSingleValue(methodName.toKebabCase(), params)
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(json, typeReference)
    }
}
