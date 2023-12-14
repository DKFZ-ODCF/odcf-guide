package de.dkfz.odcf.guide.service.interfaces.external

interface ProjectTargetService {

    /**
     * Send values to an external source's API.
     *
     * @param methodName The name of the function being called on the external API
     * @param params The parameters that are being sent to the external API
     * @return The response from the external source as a JSON string
     */
    fun sendValues(methodName: String, params: Map<String, String> = emptyMap()): String

    /**
     * Get a single value from an external source's API by querying it with a set of values
     *
     * @param methodName The name of the function being called from the external API
     * @param params The necessary parameters this function needs
     * @return The value from the API as a String
     */
    fun getSingleValueFromSet(methodName: String, params: Map<String, Set<String>> = emptyMap()): String

    /**
     * Get a set of values from an external source's API by querying it with a set of values
     *
     * @param methodName The name of the function being called from the external API
     * @param params The necessary parameters this function needs
     * @return The values from the API as a `Set<String>`
     */
    fun getSetOfValuesFromSet(methodName: String, params: Map<String, Set<String>> = emptyMap()): Set<String>

    /** Regularly checks the projects from the externalMetadataSourceService and updates them in the projectTargetService. */
    fun updateProjectsInTarget(): Boolean
}
