package de.dkfz.odcf.guide.service.implementation.external

import de.dkfz.odcf.guide.annotation.ExcludeFromJacocoGeneratedReport
import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

@Service
class JsonApiServiceImpl : JsonApiService {

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
}
