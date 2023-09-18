package de.dkfz.odcf.guide.service.interfaces.external

import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException

interface JsonApiService {

    /**
     * Sends a GET request to an API endpoint to retrieve JSON content.
     *
     * @param url The URL of the API endpoint.
     * @param headers A map of headers to include in the request.
     * @param apiType The type of API being called.
     * @return The JSON response from the API as a string.
     * @throws ExternalApiReadException If the response content is empty.
     */
    @Throws(ExternalApiReadException::class)
    fun getJsonFromApi(url: String, headers: Map<String, String>, apiType: ApiType): String

    /**
     * Sends a POST request with JSON content to an API endpoint.
     *
     * @param url The URL of the API endpoint.
     * @param headers A map of headers to include in the request.
     * @param body The JSON payload to be sent in the request body.
     * @param apiType The type of API being called.
     * @return The response from the API as a string.
     * @throws ExternalApiReadException If the response content is empty.
     */
    @Throws(ExternalApiReadException::class)
    fun postJsonToApi(url: String, headers: Map<String, String>, body: String, apiType: ApiType): String
}
