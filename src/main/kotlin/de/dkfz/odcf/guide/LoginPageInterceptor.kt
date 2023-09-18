package de.dkfz.odcf.guide

import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.util.UrlPathHelper
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LoginPageInterceptor(private val personRepository: PersonRepository) : HandlerInterceptor {

    var urlPathHelper = UrlPathHelper()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (isAuthenticated() && (
            urlPathHelper.getLookupPathForRequest(request).contains("/metadata-") ||
                urlPathHelper.getLookupPathForRequest(request).contains("/project-overview") ||
                urlPathHelper.getLookupPathForRequest(request).contains("/admin") ||
                urlPathHelper.getLookupPathForRequest(request).contains("/feedback") ||
                urlPathHelper.getLookupPathForRequest(request).contains("/parser")
            )
        ) {
            val username = request.remoteUser
            val user = personRepository.findByUsername(username)
            if (user != null && !user.acceptedDataSecurityStatement) {
                val encodedRedirectURL = response.encodeRedirectURL(request.contextPath.toString() + "/privacy-policy")
                response.status = HttpStatus.TEMPORARY_REDIRECT.value()
                response.setHeader("Location", encodedRedirectURL)
                return false
            }
        }
        return true
    }

    private fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication == null || AnonymousAuthenticationToken::class.java.isAssignableFrom(authentication.javaClass)) {
            false
        } else authentication.isAuthenticated
    }
}
