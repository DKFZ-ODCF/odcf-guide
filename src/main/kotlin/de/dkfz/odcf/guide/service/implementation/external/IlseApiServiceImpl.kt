package de.dkfz.odcf.guide.service.implementation.external

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.dkfz.odcf.guide.ImportSourceDataRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.submissionData.ImportSourceData
import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helperObjects.importObjects.ExternalIlseSubmissionImportObject
import de.dkfz.odcf.guide.service.interfaces.external.IlseApiService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.Base64Utils
import java.net.URL

@Service
class IlseApiServiceImpl(
    private val importSourceDataRepository: ImportSourceDataRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val jsonApiService: JsonApiService,
    private val env: Environment,
) : IlseApiService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Reads the content of a JSON file from the ILSe API which contains all the information about the samples
     * belonging to an ILSe submission. The maximum file size is 10MB.
     *
     * @param ilseId The ILSe number of a submission as an Integer
     * @return JSON content as a String or empty String if the JSON file is empty.
     */
    fun getJsonFromApi(ilseId: Int): String {
        val username = env.getRequiredProperty("ilse.api.username")
        val password = env.getRequiredProperty("ilse.api.password")
        val basicAuthHeader = "basic ${Base64Utils.encodeToString(("$username:$password").toByteArray())}"
        val headers = mapOf(HttpHeaders.AUTHORIZATION to basicAuthHeader)
        val url = URL(env.getRequiredProperty("ilse.api.url") + ilseId)
        return jsonApiService.getJsonFromApi(url.toString(), headers, ApiType.ILSe)
    }

    @Throws(
        JsonProcessingException::class,
        JsonMappingException::class,
        ExternalApiReadException::class
    )
    override fun getSubmissionImportObjectFromApi(ilseId: Int, replaceExistingEntry: Boolean): ExternalIlseSubmissionImportObject {
        val json = try {
            getJsonFromApi(ilseId).replaceFirst(ilseId.toString().toRegex(), "samples")
        } catch (e: Exception) {
            logger.warn(e.stackTraceToString())
            throw ExternalApiReadException(e.message.orEmpty(), ApiType.ILSe)
        }
        if (json.replace(" ", "") == "{\"samples\":[{}]}") {
            throw ExternalApiReadException("JSON is empty", ApiType.ILSe)
        }

        if (runtimeOptionsRepository.findByName("jsonToDB")?.value.toBoolean() && replaceExistingEntry) {
            val internalIdentifier = String.format("i%07d", ilseId)
            val importSourceData = ImportSourceData(internalIdentifier, json)
            importSourceDataRepository.saveAndFlush(importSourceData)
        }
        val objectMapper = ObjectMapper()
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        val externalIlseSubmissionImportObjects = objectMapper.readValue(json, ExternalIlseSubmissionImportObject::class.java)

        if (externalIlseSubmissionImportObjects.samples!!.any { it.submission_id != ilseId.toString() }) {
            throw ExternalApiReadException("At least one ILSe number from API does not match with the given ILSe number '$ilseId'", ApiType.ILSe)
        }

        return externalIlseSubmissionImportObjects
    }
}
