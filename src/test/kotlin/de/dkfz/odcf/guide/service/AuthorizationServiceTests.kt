package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.PersonRepository
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.security.AuthorizationServiceImpl
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class AuthorizationServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var authorizationServiceImpl: AuthorizationServiceImpl

    @Mock
    lateinit var ldapService: LdapService
    @Mock
    lateinit var personRepository: PersonRepository

    @TestFactory
    fun `test functionality of checkAuthorization for unauthorized users`() = listOf(
        "tokenUser" to "You are not authorized to use this page",
        "tokenUnknown" to "Invalid token",
        "" to "No token was found",
    ).map { (token, expected) ->
        DynamicTest.dynamicTest("get authorization for token: '$token'") {
            var user: Person? = entityFactory.getPerson()
            user?.apiToken = token
            if (token == "tokenUnknown") user = null

            `when`(personRepository.findByApiToken(token)).thenReturn(user)

            val result = authorizationServiceImpl.checkAuthorization(token)

            assertThat(result).isNotNull
            assertThat(result!!.body).isEqualTo(expected)
        }
    }

    @Test
    fun `test functionality of checkAuthorization for token 'null'`() {

        val result = authorizationServiceImpl.checkAuthorization(null)

        assertThat(result).isNotNull
        assertThat(result!!.body).isEqualTo("No token was found")
    }

    @Test
    fun `test functionality of checkAuthorization for admin`() {
        val user = entityFactory.getPerson()
        user.isAdmin = true
        user.apiToken = "tokenAdmin"

        `when`(personRepository.findByApiToken("tokenAdmin")).thenReturn(user)

        val result = authorizationServiceImpl.checkAuthorization("tokenAdmin")

        assertThat(result).isNull()
    }

    @Test
    fun `test functionality of checkAuthorization for admin with no token`() {

        `when`(ldapService.isCurrentUserAdmin()).thenReturn(true)

        val result = authorizationServiceImpl.checkAuthorization("")

        assertThat(result).isNull()
    }
}
