package de.dkfz.odcf.guide

import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.Locale

@Bean
fun localeResolver(): LocaleResolver {
    val slr = SessionLocaleResolver()
    slr.setDefaultLocale(Locale.US)
    return slr
}

@Bean
fun localeChangeInterceptor(): LocaleChangeInterceptor {
    val lci = LocaleChangeInterceptor()
    lci.paramName = "lang"
    return lci
}
