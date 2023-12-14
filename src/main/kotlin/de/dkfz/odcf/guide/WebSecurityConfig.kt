package de.dkfz.odcf.guide

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher

@Configuration
open class WebSecurityConfig(private val env: Environment) : WebSecurityConfigurerAdapter() {

    @Value("\${ldap.urls}")
    private val ldapUrls: String? = null
    @Value("\${ldap.base.dn}")
    private val ldapBaseDn: String? = null
    @Value("\${ldap.username}")
    private val ldapSecurityPrincipal: String? = null
    @Value("\${ldap.password}")
    private val ldapPrincipalPassword: String? = null
    @Value("\${ldap.user.search.filter}")
    private val ldapUserSearchFilter: String? = null

    @Throws(Exception::class)
    fun ldapAuthoritiesPopulator(): LdapAuthoritiesPopulator {
        val populator = DefaultLdapAuthoritiesPopulator(contextSource(), "")
        populator.setIgnorePartialResultException(true)
        return populator
    }

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth.ldapAuthentication()
            .userSearchFilter(ldapUserSearchFilter)
            .contextSource(contextSource())
            .ldapAuthoritiesPopulator(ldapAuthoritiesPopulator())
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        var page = "/login"
        if (env.getProperty("application.maintenance", "false").toBoolean()) {
            page = "/error/503"
        }
        if (!env.getProperty("backdoor.user", "false").toBoolean()) {
            http.authorizeRequests()
                .requestMatchers(
                    AndRequestMatcher(
                        OrRequestMatcher(
                            AntPathRequestMatcher("/services/ilse/api"),
                            AntPathRequestMatcher("/services/otp/get-users-to-be-notified"),
                            AntPathRequestMatcher("/services/get-project-path"),
                            AntPathRequestMatcher("/services/refresh-projects"),
                            AntPathRequestMatcher("/metadata-validator/submission-actions/register-ticket-number"),
                        ),
                        RequestHeaderRequestMatcher("User-Token")
                    )
                ).permitAll()
                .antMatchers("/metadata-*/**").fullyAuthenticated()
                .antMatchers("/project/overview/**").fullyAuthenticated()
                .antMatchers("/services/**").fullyAuthenticated()
                .antMatchers("/admin/**").fullyAuthenticated()
                .antMatchers("/feedback").fullyAuthenticated()
                .antMatchers("/parser").fullyAuthenticated()
                .and()
                .formLogin().loginPage(page).permitAll()
                .and()
                .logout().permitAll()
        }
        http.csrf().disable()
    }

    @Bean
    open fun contextSource(): LdapContextSource {
        val contextSource = LdapContextSource()
        contextSource.setUrl(ldapUrls)
        contextSource.setBase(ldapBaseDn)
        contextSource.userDn = ldapSecurityPrincipal
        contextSource.password = ldapPrincipalPassword
        contextSource.isAnonymousReadOnly = false
        contextSource.isPooled = true
        contextSource.afterPropertiesSet()
        return contextSource
    }
}
