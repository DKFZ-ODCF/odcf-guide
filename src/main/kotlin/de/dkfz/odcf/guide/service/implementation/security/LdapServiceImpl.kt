package de.dkfz.odcf.guide.service.implementation.security

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.dkfz.odcf.guide.PersonRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.annotation.ExcludeFromJacocoGeneratedReport
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.exceptions.UserNotFoundException
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.math.BigInteger
import javax.naming.NamingException
import javax.naming.directory.Attributes

@Service
open class LdapServiceImpl(
    private val jsonApiService: JsonApiService,
    private val ldapTemplate: LdapTemplate,
    private val personRepository: PersonRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val env: Environment
) : LdapService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        ldapTemplate.setIgnorePartialResultException(true)
    }

    override fun getPerson(): Person {
        val backdoorUser = env.getRequiredProperty("backdoor.user").toBoolean()
        if (backdoorUser) {
            val person = getPersonByMail(env.getRequiredProperty("backdoor.user.mail"))
            person.isAdmin = true
            return person
        }

        val loggedInUser = SecurityContextHolder.getContext().authentication
        val username = loggedInUser.name

        if (username == "anonymousUser") throw UserNotFoundException("user is not yet logged in")
        return getPersonByUsername(username)
    }

    override fun isCurrentUserAdmin(): Boolean {
        return try {
            getPerson().isAdmin
        } catch (e: UserNotFoundException) {
            false
        }
    }

    override fun getPersonByUsername(username: String): Person {
        val lowercaseUsername = username.lowercase()
        var resultPerson = personRepository.findByUsername(lowercaseUsername)
        if (resultPerson == null) {
            logger.debug("Did not find user with username '$lowercaseUsername' in DB. Will try to find user via LDAP now...")
            logger.debug(Thread.currentThread().stackTrace.take(10).joinToString("\n"))
            try {
                resultPerson = ldapTemplate.search("", "cn=$lowercaseUsername", PersonAttributesMapper())[0] as Person
                personRepository.save(resultPerson)
                updatePersonFromLdap(resultPerson)
                logger.debug("Created new DB entry for user '${resultPerson.username}' (${resultPerson.mail}) from LDAP")
            } catch (e: IndexOutOfBoundsException) {
                throw UserNotFoundException("This user was not found via LDAP.")
            } catch (e: NullPointerException) {
                throw UserNotFoundException("LDAP did not present a required attribute")
            } catch (e: Exception) {
                throw e
            }
        }
        return resultPerson
    }

    override fun getPersonByMail(mail: String): Person {
        val lowercaseMail = mail.lowercase()
        var resultPerson = personRepository.findByMail(lowercaseMail)
        if (resultPerson == null) {
            logger.debug("Did not find user with mail address '$lowercaseMail' in DB. Will try to find user via LDAP now...")
            logger.debug(Thread.currentThread().stackTrace.take(10).joinToString("\n"))
            try {
                resultPerson = ldapTemplate.search("", "mail=$lowercaseMail", PersonAttributesMapper())[0] as Person
                personRepository.save(resultPerson)
                updatePersonFromLdap(resultPerson)
                logger.debug("Created new DB entry for user '${resultPerson.username}' (${resultPerson.mail}) from LDAP")
            } catch (e: IndexOutOfBoundsException) {
                throw UserNotFoundException("This user was not found via LDAP.")
            } catch (e: NullPointerException) {
                throw UserNotFoundException("LDAP did not present a required attribute")
            } catch (e: Exception) {
                throw e
            }
        }
        return resultPerson
    }

    /**
     * Regularly updates all the person objects in the Person repository of the database with information from LDAP.
     * This is scheduled to happen at midnight on every Sunday.
     *
     * @throws UserNotFoundException If a person existing in the database is not found in LDAP.
     */
    @Scheduled(cron = "\${application.projectOverview.cron.ldap}")
    open fun updatePersonsFromLdap() {
        personRepository.findAllByAccountDisabled(true).forEach { person ->
            val resultPerson = ldapTemplate.search("", "cn=${person.username}", PersonAttributesMapper())[0]
            if (resultPerson.accountDisabled.not()) {
                person.accountDisabled = false
                personRepository.save(person)
            }
        }
        personRepository.findAllByAccountDisabled(false).forEach { person ->
            updatePersonFromLdap(person)
        }
    }

    fun updatePersonFromLdap(person: Person) {
        try {
            val resultPerson = ldapTemplate.search("", "cn=${person.username}", PersonAttributesMapper())[0]
            person.lastName = resultPerson.lastName
            person.firstName = resultPerson.firstName
            person.mail = resultPerson.mail
            person.department = resultPerson.department
            person.accountDisabled = resultPerson.accountDisabled

            val organizationalUnitsPathOption = runtimeOptionsRepository.findByName("organizationalUnitsPath")
            val organizationalUnitLeaderPathOption = runtimeOptionsRepository.findByName("organizationalUnitLeaderPath")
            if (organizationalUnitsPathOption != null &&
                organizationalUnitsPathOption.value.isNotBlank() &&
                organizationalUnitLeaderPathOption != null &&
                organizationalUnitLeaderPathOption.value.isNotBlank()
            ) {
                val objectMapper = ObjectMapper()
                var json = jsonApiService.getJsonFromApi(organizationalUnitsPathOption.value + person.department, emptyMap(), ApiType.ITCF)
                val organizationalUnits = objectMapper.readValue(json, object : TypeReference<Set<Map<String, String>>>() {})
                person.organizationalUnit = organizationalUnits.firstOrNull { it["Kst"] == person.department }?.get("OENummer") ?: "N/A"
                json = jsonApiService.getJsonFromApi(organizationalUnitLeaderPathOption.value + person.organizationalUnit, emptyMap(), ApiType.ITCF)
                val organizationalUnitLeader = objectMapper.readValue(json, object : TypeReference<Set<String>>() {})
                person.isOrganizationalUnitLeader = person.username == organizationalUnitLeader.firstOrNull()
            }

            personRepository.save(person)
        } catch (e: IndexOutOfBoundsException) {
            logger.warn("The user ${person.username} was not found via LDAP.")
        } catch (e: NullPointerException) {
            logger.warn("LDAP did not present a required attribute for user ${person.username}")
        } catch (e: Exception) {
            logger.error(e.localizedMessage)
        }
    }

    private inner class PersonAttributesMapper : AttributesMapper<Person> {

        @Throws(NamingException::class)
        @ExcludeFromJacocoGeneratedReport
        override fun mapFromAttributes(attrs: Attributes): Person {
            val person = Person()
            person.username = attrs.get("cn").get().toString()
            person.lastName = attrs.get("sn").get().toString()
            person.firstName = attrs.get("givenName").get().toString()
            person.mail = attrs.get("mail").get().toString()
            person.department = attrs.get("department").get().toString()
            // check if account is disabled according to LDAP (check if ACCOUNTDISABLE bit is set)
            person.accountDisabled = BigInteger(attrs.get("userAccountControl").get().toString()).testBit(1)
            return person
        }
    }
}
