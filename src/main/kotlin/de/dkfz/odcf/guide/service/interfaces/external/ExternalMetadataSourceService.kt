package de.dkfz.odcf.guide.service.interfaces.external

import de.dkfz.odcf.guide.entity.Person

interface ExternalMetadataSourceService {

    /**
     * Retrieve a single value from the external metadata source.
     * It makes use of `getJsonFromApi` to fetch a single value using the specified `methodName` and `params`.
     * The method name and parameters are transformed into a URL suffix.
     *
     * @param methodName The name of the method to be called on the external API.
     * @param params     A map of parameters to be passed to the method.
     * @return The retrieved value as a String.
     */
    fun getSingleValue(methodName: String, params: Map<String, String> = emptyMap()): String

    /**
     * Retrieve values as a sorted set from the external source.
     * The method name and parameters are transformed into a URL suffix.
     *
     * @param methodName The name of the method to be called on the external API.
     * @param params     A map of parameters to be passed to the method.
     * @return A sorted set of retrieved values.
     */
    fun getValuesAsSet(methodName: String, params: Map<String, String> = emptyMap()): Set<String>

    /**
     * Retrieve values as a set of maps from the external source.
     * The method name and parameters are transformed into a URL suffix.
     *
     * @param methodName The name of the method to be called on the external API.
     * @param params     A map of parameters to be passed to the method.
     * @return A set of maps containing the retrieved values.
     */
    fun getValuesAsSetMap(methodName: String, params: Map<String, String> = emptyMap()): Set<Map<String, String>>

    /**
     * Returns a set of principal investigators for the specified project.
     *
     * @param projectName the name of the project
     * @return a set of [Person] objects representing the principal investigators of the project
     */
    fun getPrincipalInvestigatorsAsPersonSet(projectName: String): Set<Person>
}
