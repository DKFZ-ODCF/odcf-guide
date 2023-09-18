package de.dkfz.odcf.guide.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.PersonRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.exceptions.UserNotFoundException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.security.LdapServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest
@WithMockUser("mocki")
class LdapServiceTests @Autowired constructor(private val ldapService: LdapService) {

    private val USERNAME = "mocki"
    private val MAIL = "mocki@mail.de"
    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var ldapServiceMock: LdapServiceImpl

    @Mock
    lateinit var jsonApiService: JsonApiService

    @Mock
    lateinit var ldapTemplate: LdapTemplate

    @Mock
    lateinit var personRepository: PersonRepository

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var env: Environment

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(LdapServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `test get user by usernamer use db`() {
        val person = entityFactory.getPerson()

        `when`(personRepository.findByUsername(USERNAME)).thenReturn(person)

        val person1 = ldapServiceMock.getPersonByUsername(USERNAME)

        assertThat(person1).isEqualTo(person)
    }

    @Test
    fun `test get user by usernamer use mapper`() {
        val person = entityFactory.getPerson()

        `when`(ldapTemplate.search(anyString(), eq("cn=$USERNAME"), any(AttributesMapper::class.java))).thenReturn(listOf(person))

        val person1 = ldapServiceMock.getPersonByUsername(USERNAME)

        assertThat(person1).isEqualTo(person)
    }

    @Test
    fun `test get user by mail use db`() {
        val person = entityFactory.getPerson()

        `when`(personRepository.findByMail(MAIL)).thenReturn(person)

        val person1 = ldapServiceMock.getPersonByMail(MAIL)

        assertThat(person1).isEqualTo(person)
    }

    @Test
    fun `test get user by mail use mapper`() {
        val person = entityFactory.getPerson()

        `when`(ldapTemplate.search(anyString(), eq("mail=$MAIL"), any(AttributesMapper::class.java))).thenReturn(listOf(person))

        val person1 = ldapServiceMock.getPersonByMail(MAIL)

        assertThat(person1).isEqualTo(person)
    }

    @Test
    fun `test get user with mock user`() {
        val person = entityFactory.getPerson()

        `when`(env.getRequiredProperty("backdoor.user")).thenReturn("false")
        `when`(ldapTemplate.search(anyString(), eq("cn=$USERNAME"), any(AttributesMapper::class.java))).thenReturn(listOf(person))

        val person1 = ldapServiceMock.getPerson()

        assertThat(person1).isEqualTo(person)
    }

    @Test
    fun `test get user with backdoor user`() {
        val person = entityFactory.getPerson()

        `when`(env.getRequiredProperty("backdoor.user")).thenReturn("true")
        `when`(env.getRequiredProperty("backdoor.user.mail")).thenReturn(MAIL)
        `when`(ldapTemplate.search(anyString(), eq("mail=$MAIL"), any(AttributesMapper::class.java))).thenReturn(listOf(person))

        val person1 = ldapServiceMock.getPerson()

        assertThat(person1).isEqualTo(person)
    }

    @Test
    fun `test if user is admin`() {
        val person = entityFactory.getPerson()

        `when`(env.getRequiredProperty("backdoor.user")).thenReturn("false")
        `when`(personRepository.findByUsername(USERNAME)).thenReturn(person)

        val isAdmin = ldapServiceMock.isCurrentUserAdmin()

        assertThat(isAdmin).isEqualTo(false)
    }

    @Test
    fun `When getPersonByUsername then return user`() {
        val person = entityFactory.getPerson()

        `when`(personRepository.findByUsername(person.username)).thenReturn(person)

        val user = ldapServiceMock.getPersonByUsername(person.username)
        assertThat(user).isEqualTo(person)
    }

    @Test
    fun `When getPersonByUsername but username unknown then throw Exception`() {
        Assertions.assertThatExceptionOfType(UserNotFoundException::class.java).isThrownBy {
            ldapServiceMock.getPersonByUsername("name")
        }.withMessage("This user was not found via LDAP.")
    }

    @Test
    fun `When getPersonByMail then return user`() {
        val person = entityFactory.getPerson()

        `when`(personRepository.findByMail(person.mail)).thenReturn(person)

        val user = ldapServiceMock.getPersonByMail(person.mail)
        assertThat(user).isEqualTo(person)
    }

    @Test
    fun `When getPersonByMail but mail unknown then throw Exception`() {
        Assertions.assertThatExceptionOfType(UserNotFoundException::class.java).isThrownBy {
            ldapServiceMock.getPersonByMail("mail")
        }.withMessage("This user was not found via LDAP.")
    }

    @Test
    fun `When updatePersonsFromLdap test user not found exception`() {
        val listAppender = initListAppender()
        val person = entityFactory.getPerson()

        `when`(personRepository.findAllByAccountDisabled(false)).thenReturn(setOf(person))

        ldapServiceMock.updatePersonsFromLdap()
        val logsList = listAppender.list

        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("The user ${person.username} was not found via LDAP.")
    }

    @Test
    fun `When updatePersonsFromLdap test changes`() {
        val person = entityFactory.getPerson()
        person.username = "user1"
        val person2 = entityFactory.getPerson()
        person2.username = "user2"
        person2.firstName = "otherFN"
        person2.lastName = "otherLN"
        val someUrl = "url"
        val oe = "OE1234"

        `when`(personRepository.findAllByAccountDisabled(false)).thenReturn(setOf(person))
        `when`(ldapTemplate.search(anyString(), anyString(), any(AttributesMapper::class.java))).thenReturn(listOf(person2))
        `when`(runtimeOptionsRepository.findByName("organizationalUnitsPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + person.department, emptyMap(), ApiType.ITCF)).thenReturn("[{\"OENummer\":\"$oe\",\"Kst\":\"${person.department}\"}]")
        `when`(runtimeOptionsRepository.findByName("organizationalUnitLeaderPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + oe, emptyMap(), ApiType.ITCF)).thenReturn("[\"${person.username}\"]")

        ldapServiceMock.updatePersonsFromLdap()

        assertThat(person.organizationalUnit).isEqualTo(oe)
        assertThat(person.isOrganizationalUnitLeader).isTrue
        assertThat(person.firstName).isEqualTo(person2.firstName)
        assertThat(person.lastName).isEqualTo(person2.lastName)
    }

    @Test
    fun `When updatePersonsFromLdap test organizational unit and leader empty`() {
        val person = entityFactory.getPerson()
        val someUrl = "url"
        val oe = "N/A"

        `when`(personRepository.findAllByAccountDisabled(false)).thenReturn(setOf(person))
        `when`(ldapTemplate.search(anyString(), anyString(), any(AttributesMapper::class.java))).thenReturn(listOf(entityFactory.getPerson()))
        `when`(runtimeOptionsRepository.findByName("organizationalUnitsPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + person.department, emptyMap(), ApiType.ITCF)).thenReturn("[]")
        `when`(runtimeOptionsRepository.findByName("organizationalUnitLeaderPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + oe, emptyMap(), ApiType.ITCF)).thenReturn("[]")

        ldapServiceMock.updatePersonsFromLdap()

        assertThat(person.organizationalUnit).isEqualTo(oe)
        assertThat(person.isOrganizationalUnitLeader).isFalse
    }

    @Test
    fun `When inactive user account becomes active again`() {
        val person = entityFactory.getPerson()
        person.accountDisabled = true
        val someUrl = "url"
        val oe = "N/A"

        `when`(personRepository.findAllByAccountDisabled(true)).thenReturn(setOf(person))
        `when`(ldapTemplate.search(anyString(), anyString(), any(AttributesMapper::class.java))).thenReturn(listOf(entityFactory.getPerson()))
        `when`(runtimeOptionsRepository.findByName("organizationalUnitsPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + person.department, emptyMap(), ApiType.ITCF)).thenReturn("[{\"OENummer\":\"$oe\",\"Kst\":\"${person.department}\"}]")
        `when`(runtimeOptionsRepository.findByName("organizationalUnitLeaderPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + oe, emptyMap(), ApiType.ITCF)).thenReturn("[\"${person.username}\"]")

        ldapServiceMock.updatePersonsFromLdap()

        assertThat(person.accountDisabled).isFalse()
    }

    @Test
    fun `When active user account becomes inactive`() {
        val person = entityFactory.getPerson()
        val someUrl = "url"
        val oe = "N/A"
        val person2 = entityFactory.getPerson()
        person2.accountDisabled = true

        `when`(personRepository.findAllByAccountDisabled(false)).thenReturn(setOf(person))
        `when`(ldapTemplate.search(anyString(), anyString(), any(AttributesMapper::class.java))).thenReturn(listOf(person2))
        `when`(runtimeOptionsRepository.findByName("organizationalUnitsPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + person.department, emptyMap(), ApiType.ITCF)).thenReturn("[{\"OENummer\":\"$oe\",\"Kst\":\"${person.department}\"}]")
        `when`(runtimeOptionsRepository.findByName("organizationalUnitLeaderPath")).thenReturn(entityFactory.getRuntimeOption(someUrl))
        `when`(jsonApiService.getJsonFromApi(someUrl + oe, emptyMap(), ApiType.ITCF)).thenReturn("[\"${person.username}\"]")

        ldapServiceMock.updatePersonsFromLdap()

        assertThat(person.accountDisabled).isTrue()
    }
}
