package de.dkfz.odcf.guide.service.external

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.exceptions.UserNotFoundException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.service.implementation.external.ExternalMetadataSourceServiceImpl
import de.dkfz.odcf.guide.service.implementation.external.JsonApiServiceImpl
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ExternalMetadataSourceServiceTests {

    @InjectMocks
    @Spy
    lateinit var externalMetadataSourceServiceMock: ExternalMetadataSourceServiceImpl

    @Mock
    lateinit var jsonApiService: JsonApiServiceImpl

    @Mock
    lateinit var ldapService: LdapService

    private val entityFactory = EntityFactory()

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(ExternalMetadataSourceServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `test get single value without params`() {
        val value = "singleValue"

        doReturn(value).`when`(jsonApiService).getJsonFromApi("method-name", ApiType.OTP)

        val result = externalMetadataSourceServiceMock.getSingleValue("methodName")

        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `test get single value with params`() {
        val value = "singleValue"

        doReturn(value).`when`(jsonApiService).getJsonFromApi("method-name?a=b", ApiType.OTP)

        val result = externalMetadataSourceServiceMock.getSingleValue("methodName", mapOf("a" to "b"))

        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `test get set of values`() {
        val value = """["a","b","c"]"""

        `when`(jsonApiService.getValues<List<String>>(anyString(), anyOrNull(), anyOrNull(), anyOrNull())).thenCallRealMethod()
        `when`(jsonApiService.getJsonFromApi("method-name", ApiType.OTP)).thenReturn(value)

        val result = externalMetadataSourceServiceMock.getValuesAsSet("methodName")

        assertThat(result).hasSize(3)
        assertThat(result).contains("a")
        assertThat(result).contains("b")
        assertThat(result).contains("c")
    }

    @Test
    fun `test get set of map of values`() {
        val value = """[
            {
                "a":"a1",
                "b":"b1",
                "c":"c1"
            }
        ]"""

        `when`(jsonApiService.getValues<List<String>>(anyString(), anyOrNull(), anyOrNull(), anyOrNull())).thenCallRealMethod()
        `when`(jsonApiService.getJsonFromApi("method-name", ApiType.OTP)).thenReturn(value)

        val result = externalMetadataSourceServiceMock.getValuesAsSetMap("methodName")

        assertThat(result).hasSize(1)
        assertThat(result.first()).hasSize(3)
        assertThat(result.first().entries.first().key).contains("a")
        assertThat(result.first().entries.first().value).contains("a1")
        assertThat(result.first().entries.last().key).contains("c")
        assertThat(result.first().entries.last().value).contains("c1")
    }

    @Test
    fun `test get set of values with exception`() {
        val listAppender = initListAppender()

        `when`(jsonApiService.getValues<List<String>>(anyString(), anyOrNull(), anyOrNull(), anyOrNull())).thenCallRealMethod()
        `when`(jsonApiService.getJsonFromApi("method-name", ApiType.OTP)).thenReturn("")
        val logsList = listAppender.list

        val result = externalMetadataSourceServiceMock.getValuesAsSet("methodName")

        assertThat(result).isEqualTo(emptySet<String>())
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There was a problem when reading out the JSON ''")
    }

    @Test
    fun `test get set of map of values with exception`() {
        val listAppender = initListAppender()

        `when`(jsonApiService.getValues<List<String>>(anyString(), anyOrNull(), anyOrNull(), anyOrNull())).thenCallRealMethod()
        `when`(jsonApiService.getJsonFromApi("method-name", ApiType.OTP)).thenReturn("")

        val result = externalMetadataSourceServiceMock.getValuesAsSetMap("methodName")
        val logsList = listAppender.list

        assertThat(result).isEqualTo(emptySet<Map<String, String>>())
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There was a problem when reading out the JSON ''")
    }

    @Test
    fun `test get set of PIs`() {
        val listAppender = initListAppender()
        val pi1 = entityFactory.getPerson("pi1", "pi", "1")
        val pi2 = entityFactory.getPerson("pi2", "pi", "2")

        doReturn(setOf("pi1", "pi2")).`when`(externalMetadataSourceServiceMock).getValuesAsSet("pis-by-project", mapOf("project" to "projectName"))
        `when`(ldapService.getPersonByUsername("pi1")).thenReturn(pi1)
        `when`(ldapService.getPersonByUsername("pi2")).thenReturn(pi2)

        val result = externalMetadataSourceServiceMock.getPrincipalInvestigatorsAsPersonSet("projectName")
        val logsList = listAppender.list

        assertThat(result).isEqualTo(setOf(pi1, pi2))
        assertThat(logsList).hasSize(0)
    }

    @Test
    fun `test get set of PIs with exception`() {
        val listAppender = initListAppender()
        val pi1 = entityFactory.getPerson("pi1", "pi", "1")

        doReturn(setOf("pi1", "pi2")).`when`(externalMetadataSourceServiceMock).getValuesAsSet("pis-by-project", mapOf("project" to "projectName"))
        `when`(ldapService.getPersonByUsername("pi1")).thenReturn(pi1)
        `when`(ldapService.getPersonByUsername("pi2")).doThrow(UserNotFoundException("This user was not found via LDAP."))

        val result = externalMetadataSourceServiceMock.getPrincipalInvestigatorsAsPersonSet("projectName")
        val logsList = listAppender.list

        assertThat(result).isEqualTo(setOf(pi1))
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("user 'pi2' not added as PI to project 'projectName' with reason:\nThis user was not found via LDAP.")
    }
}
