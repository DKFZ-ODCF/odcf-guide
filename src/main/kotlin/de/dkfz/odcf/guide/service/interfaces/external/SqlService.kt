package de.dkfz.odcf.guide.service.interfaces.external

import java.sql.Connection

interface SqlService {

    /**
     * Performs an SQL query to get the results for one property.
     *
     * @param connection The database connection to be used
     * @param propertyToGet The name of the property that is being queried
     * @param sql The whole SQl query as a string
     * @return A list of strings that are the result of the query
     */
    fun getFromRemote(connection: Connection, propertyToGet: String, sql: String): List<String>

    /**
     * Performs an SQL query to get the results for multiple properties.
     *
     * @param connection The database connection to be used
     * @param propertiesToGet A list of properties that are being queried
     * @param sql The whole SQl query as a string
     * @return A list of the name of the queried properties mapped to the resulting string
     */
    fun getMultipleFromRemote(connection: Connection, propertiesToGet: List<String>, sql: String): List<Map<String, String>>

    /**
     * Performs an SQL query to get the results for one property where the result is more than one string.
     *
     * @param connection The database connection to be used
     * @param propertyToGet The name of the property that is being queried
     * @param sql The whole SQl query as a string
     * @return A set of strings containing all the resulting strings of the query
     */
    fun getSetFromRemote(connection: Connection, propertyToGet: String, sql: String): Set<String>
}
