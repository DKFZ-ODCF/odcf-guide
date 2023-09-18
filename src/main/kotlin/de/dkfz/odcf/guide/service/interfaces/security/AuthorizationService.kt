package de.dkfz.odcf.guide.service.interfaces.security

import de.dkfz.odcf.guide.entity.Role
import org.springframework.http.ResponseEntity

interface AuthorizationService {

    /**
     * Uses the provided token to check whether the corresponding user is allowed to view a page.
     *
     * @param token Token representing a user in the database
     * @return `null` if the given token authorizes the user to view a page
     *          or a ResponseEntity with an explanatory message if the token is faulty
     */
    fun checkAuthorization(token: String?): ResponseEntity<String>?

    /**
     * Check if a token is authorized for the given role.
     *
     * @param token token of the user to be checked
     * @param role role that the user needs
     * @return `null` if the given token is authorized
     *          or a ResponseEntity with an explanatory message if the token is faulty
     */
    fun checkIfTokenIsAuthorized(token: String, role: Role): ResponseEntity<String>?
}
