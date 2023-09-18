package de.dkfz.odcf.guide.service.implementation.security

import de.dkfz.odcf.guide.PersonRepository
import de.dkfz.odcf.guide.entity.Role
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
open class AuthorizationServiceImpl(
    private val ldapService: LdapService,
    private val personRepository: PersonRepository,
) : AuthorizationService {

    override fun checkAuthorization(token: String?): ResponseEntity<String>? {
        if (!ldapService.isCurrentUserAdmin()) {
            if (token.isNullOrBlank()) return ResponseEntity("No token was found", HttpStatus.UNAUTHORIZED)
            val user = personRepository.findByApiToken(token)
            if (user == null) {
                return ResponseEntity("Invalid token", HttpStatus.UNAUTHORIZED)
            } else if (!user.isAdmin) {
                return ResponseEntity("You are not authorized to use this page", HttpStatus.FORBIDDEN)
            }
        }
        return null
    }

    override fun checkIfTokenIsAuthorized(token: String, role: Role): ResponseEntity<String>? {
        val user = personRepository.findByApiToken(token)
        if (user == null) {
            return ResponseEntity("Invalid token", HttpStatus.UNAUTHORIZED)
        } else if (!user.roles.contains(role)) {
            return ResponseEntity("You are not authorized to use this page", HttpStatus.FORBIDDEN)
        }
        return null
    }
}
