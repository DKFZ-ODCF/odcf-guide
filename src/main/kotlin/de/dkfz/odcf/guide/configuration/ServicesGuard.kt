package de.dkfz.odcf.guide.configuration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL

@Component
class ServicesGuard(
    private val env: Environment,
) : InitializingBean {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val isTestEnvironment: Boolean
        get() {
            for (element in Thread.currentThread().stackTrace) {
                if (element.className.startsWith("org.junit.")) {
                    return true
                }
            }
            return false
        }

    private val isNotTestEnvironment: Boolean
        get() = !isTestEnvironment

    override fun afterPropertiesSet() {
        if (isNotTestEnvironment) {
            // required microservices
            listOf(env.getRequiredProperty("externalMetadataSourceService.adapter.url")).forEach {
                val url = URL("$it/actuator/health")
                val con = url.openConnection() as HttpURLConnection
                if (con.responseCode != HTTP_OK) {
                    throw ConnectException("$url not reachable")
                }
            }
            // optional microservices
            listOf(env.getRequiredProperty("projectTargetService.adapter.url")).forEach {
                val url = URL("$it/actuator/health")
                val con = url.openConnection() as HttpURLConnection
                try {
                    if (con.responseCode != HTTP_OK) {
                        logger.info("### $url not reachable ###")
                    }
                } catch (e: ConnectException) {
                    logger.info("### $url not reachable ###")
                }
            }
        }
    }
}
