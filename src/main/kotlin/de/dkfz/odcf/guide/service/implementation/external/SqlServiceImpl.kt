package de.dkfz.odcf.guide.service.implementation.external

import de.dkfz.odcf.guide.service.interfaces.external.SqlService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.sql.Connection

@Service
class SqlServiceImpl() : SqlService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getFromRemote(connection: Connection, propertyToGet: String, sql: String): List<String> {
        val results: MutableList<String> = ArrayList()
        try {
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            while (resultSet.next()) {
                results.add(resultSet.getString(propertyToGet))
            }
        } catch (e: Exception) {
            logger.error(e.message)
        } finally {
            connection.close()
        }
        return results
    }

    override fun getMultipleFromRemote(connection: Connection, propertiesToGet: List<String>, sql: String): List<Map<String, String>> {
        val results: MutableList<Map<String, String>> = ArrayList()
        try {
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            while (resultSet.next()) {
                val map = emptyMap<String, String>().toMutableMap()
                for (propertyToGet in propertiesToGet) {
                    map[propertyToGet] = resultSet.getString(propertyToGet).orEmpty()
                }
                results.add(map)
            }
        } catch (e: Exception) {
            logger.error(e.message)
        } finally {
            connection.close()
        }
        return results
    }

    override fun getSetFromRemote(connection: Connection, propertyToGet: String, sql: String): Set<String> {
        return getFromRemote(connection, propertyToGet, sql).toSet()
    }
}
