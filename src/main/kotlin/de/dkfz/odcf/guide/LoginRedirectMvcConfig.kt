package de.dkfz.odcf.guide

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class LoginRedirectMvcConfig(private val personRepository: PersonRepository) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(LoginPageInterceptor(personRepository))
    }
}
