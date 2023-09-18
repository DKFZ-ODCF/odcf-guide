package de.dkfz.odcf.guide.configuration

import org.apache.catalina.connector.Connector
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
open class TomcatCustomizationConfiguration : WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    @Value("\${server.tomcat.max-parameter-count:10000}")
    private val maxParameterCount = 0
    override fun customize(factory: TomcatServletWebServerFactory) {
        factory.addConnectorCustomizers(
            TomcatConnectorCustomizer { connector: Connector ->
                connector.maxParameterCount = maxParameterCount
            }
        )
    }
}
