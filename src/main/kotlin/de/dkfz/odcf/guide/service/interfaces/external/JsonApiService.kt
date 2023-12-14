package de.dkfz.odcf.guide.service.interfaces.external

import com.fasterxml.jackson.core.type.TypeReference
import de.dkfz.odcf.guide.exceptions.BlankJsonException
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helperObjects.enums.ApiType

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

    /**
     * Get the metadata from an external source. The URL is defined in the application.properties file.
     * The maximum file size is 10MB.
     *
     * @param urlSuffix the name of the metadata to be retrieved
     * @return The metadata as a JSON string
     */
    fun getJsonFromApi(urlSuffix: String, apiType: ApiType): String

    /**
     * Sends information encoded in the URL String to an external source. The URL is defined in the application.properties file.
     * The maximum file size is 10MB.
     *
     * @param urlSuffix the addition to the URL containing the URI encoded information to be sent
     * @return The response from the external source as a JSON string
     */
    fun sendJsonToApi(urlSuffix: String, apiType: ApiType): String

    /**
     * Calls `getJsonFromApi` and parses the metadata JSON String that was returned into an object.
     *
     * @param methodName The name of the function being called from the external API
     * @param params The necessary parameters this function needs
     * @param apiType The API that is being queried
     * @param R The class of the object to be returned
     * @return JSON String parsed into object specified by R, returns an empty object of type R when the json is empty
     * @throws BlankJsonException if the JSON String was blank
     */
    @Throws(BlankJsonException::class)
    fun <R> getValues(methodName: String, params: Map<String, String> = emptyMap(), apiType: ApiType, typeReference: TypeReference<R>): R
}
