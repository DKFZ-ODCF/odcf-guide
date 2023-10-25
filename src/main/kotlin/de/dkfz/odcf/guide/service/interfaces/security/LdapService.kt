package de.dkfz.odcf.guide.service.interfaces.security

import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.exceptions.UserNotFoundException

interface LdapService {

    /**
     * Returns the currently logged-in user as a Person object. Alternatively returns the backdoor user.
     *
     * @return Person Currently logged-in user.
     */
    fun getPerson(): Person

    /**
     * @return `true` if the currently logged-in user is an admin.
     */
    fun isCurrentUserAdmin(): Boolean

    /**
     * Searches for a specific user object by a given username.
     *
     * If the user isn't found in the Person repository of the database,
     * it tries to search LDAP and notes down the attempt in the Logs.
     *
     * If a matching user is found in LDAP, saves the Person object for the user into the Person repository.
     *
     * @return Person object of the found user.
     * @throws UserNotFoundException If no user with a matching username can be found in the database or LDAP.
     */
    @Throws(UserNotFoundException::class)
    fun getPersonByUsername(username: String): Person

    /**
     * Searches for a specific user object by a given mail address.
     *
     * If the user isn't found in Person repository of the database,
     * tries to search in LDAP and notes its attempt in the Logs.
     *
     * If a matching user is found in LDAP, saves the Person object for the user into the Person repository.
     *
     * @return Person object of the found user.
     * @throws UserNotFoundException If no user with a matching mail address can be found in the database or LDAP.
     */
    fun getPersonByMail(mail: String): Person
}
