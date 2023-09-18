package de.dkfz.odcf.guide.configuration

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
            listOf(env.getRequiredProperty("externalMetadataSourceService.adapter.url")).forEach {
                val url = URL("$it/actuator/health")
                val con = url.openConnection() as HttpURLConnection
                if (con.responseCode != HTTP_OK) {
                    throw ConnectException("$url not reachable")
                }
            }
        }
    }
}
