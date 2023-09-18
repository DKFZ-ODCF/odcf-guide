package de.dkfz.odcf.guide

import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class MvcConfig(private val env: Environment) : WebMvcConfigurer {

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/index").setViewName("index")
        registry.addViewController("/").setViewName("index")
        registry.addViewController("").setViewName("index")
        registry.addViewController("/login").setViewName("login")
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/files/**")
            .addResourceLocations(env.getProperty("application.files.path", "file:files/"))
    }
}
