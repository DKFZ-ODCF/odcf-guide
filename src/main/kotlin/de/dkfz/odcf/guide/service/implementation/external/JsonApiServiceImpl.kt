package de.dkfz.odcf.guide.service.implementation.external

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.dkfz.odcf.guide.annotation.ExcludeFromJacocoGeneratedReport
import de.dkfz.odcf.guide.exceptions.BlankJsonException
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helperObjects.encodeUtf8
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.helperObjects.toKebabCase
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

@Service
class JsonApiServiceImpl(private val env: Environment) : JsonApiService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Throws(ExternalApiReadException::class)
    @ExcludeFromJacocoGeneratedReport
    override fun getJsonFromApi(url: String, headers: Map<String, String>, apiType: ApiType): String {
        // Send a GET request to the API endpoint with specified headers
        val response = getWebClient().get()
            .uri(URI.create(url.replace(" ", "%20")))
            .accept(MediaType.APPLICATION_JSON)
            .headers { h -> headers.forEach { h.add(it.key, it.value) } }
            .retrieve()
            .bodyToMono(String::class.java)
        return response.block() ?: throw ExternalApiReadException("Content of url $url is empty", apiType)
    }

    @Throws(ExternalApiReadException::class)
    @ExcludeFromJacocoGeneratedReport
    override fun postJsonToApi(url: String, headers: Map<String, String>, body: String, apiType: ApiType): String {
        // Send a POST request to the API endpoint with specified headers and body
        val response = getWebClient().post()
            .uri(URI.create(url.replace(" ", "%20")))
            .body(BodyInserters.fromValue(body))
            .accept(MediaType.APPLICATION_JSON)
            .headers { h -> headers.forEach { h.add(it.key, it.value) } }
            .retrieve()
            .bodyToMono(String::class.java)
        return response.block() ?: throw ExternalApiReadException("Content of url $url is empty", apiType)
    }

    /**
     * Creates and configures a WebClient instance for making HTTP requests.
     *
     * @return A configured WebClient instance.
     */
    @ExcludeFromJacocoGeneratedReport
    private fun getWebClient(): WebClient {
        // Build an exchange strategy with max in-memory size of 10 MB
        val exchangeStrategy = ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
        }.build()

        // FOR LOCAL TESTING
        /*val sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()
        val httpClient = HttpClient.create().secure { t -> t.sslContext(sslContext) }
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(exchangeStrategy)
            .build()*/

        // Create a WebClient instance with the specified exchange strategy
        return WebClient.builder()
            .exchangeStrategies(exchangeStrategy)
            .build()
    }

    override fun getJsonFromApi(urlSuffix: String, apiType: ApiType): String {
        return if (apiType.adapterService.isNotEmpty()) {
            try {
                val url = "${env.getRequiredProperty("${apiType.adapterService}.adapter.url")}${env.getRequiredProperty("${apiType.adapterService}.adapter.urlAddition")}$urlSuffix"
                val headers = mapOf(
                    "User-Token" to env.getRequiredProperty("${apiType.adapterService}.adapter.token")
                )
                getJsonFromApi(url, headers, apiType)
            } catch (e: ExternalApiReadException) {
                logger.warn("There was an error while retrieving from the external API with the message: ${e.message}")
                ""
            } catch (e: IllegalStateException) {
                logger.warn("There is no adapterService url configured for $apiType")
                ""
            }
        } else {
            logger.warn("There is no adapterService configured for $apiType")
            ""
        }
    }

    override fun sendJsonToApi(urlSuffix: String, apiType: ApiType): String {
        return if (apiType.adapterService.isNotEmpty()) {
            try {
                val url = "${env.getRequiredProperty("${apiType.adapterService}.adapter.url")}${env.getRequiredProperty("${apiType.adapterService}.adapter.urlAddition")}$urlSuffix"
                val headers = mapOf(
                    "User-Token" to env.getRequiredProperty("${apiType.adapterService}.adapter.token")
                )
                postJsonToApi(url, headers, "", apiType)
            } catch (e: ExternalApiReadException) {
                logger.warn("There was an error while retrieving from the external API with the message: ${e.message}")
                ""
            } catch (e: IllegalStateException) {
                logger.warn("There is no adapterService url configured for $apiType")
                ""
            }
        } else {
            logger.warn("There is no adapterService configured for $apiType")
            ""
        }
    }

    @Throws(BlankJsonException::class)
    override fun <R> getValues(methodName: String, params: Map<String, String>, apiType: ApiType, typeReference: TypeReference<R>): R {
        val paramString = params.map { "${it.key.encodeUtf8()}=${it.value.encodeUtf8()}" }.joinToString("&")
        val json = getJsonFromApi(methodName.toKebabCase() + "?$paramString".takeIf { paramString.isNotBlank() }.orEmpty(), apiType)
        val objectMapper = ObjectMapper()
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

        if (json.isBlank()) {
            throw BlankJsonException("There was a problem when reading out the JSON '$json'")
        }
        return objectMapper.readValue(json, typeReference)
    }
}
