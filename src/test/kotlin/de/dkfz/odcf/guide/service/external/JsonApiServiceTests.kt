package de.dkfz.odcf.guide.service.external

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.core.type.TypeReference
import de.dkfz.odcf.guide.exceptions.BlankJsonException
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.service.implementation.external.JsonApiServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.kotlin.anyOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClientRequestException

@ExtendWith(SpringExtension::class)
class JsonApiServiceTests {

    @InjectMocks
    @Spy
    lateinit var jsonApiServiceImplMock: JsonApiServiceImpl

    @Mock
    lateinit var env: Environment

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(JsonApiServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `Test getJsonFromApi via invalid url throws exception`() {
        val url = "some-fake-url"

        assertThatExceptionOfType(WebClientRequestException::class.java).isThrownBy {
            jsonApiServiceImplMock.getJsonFromApi("https://$url", emptyMap(), ApiType.PROJECT_TARGET_SERVICE)
        }.withMessageContaining("failed to resolve '$url")
    }

    @TestFactory
    fun `test getJsonFromApi without params for`() = listOf(
        "otp" to ApiType.OTP,
        "ilse" to ApiType.PROJECT_TARGET_SERVICE,
    ).map { (api, apiType) ->
        DynamicTest.dynamicTest(" '$apiType'") {
            val value = "singleValue"

            `when`(env.getRequiredProperty("externalMetadataSourceService.adapter.url")).thenReturn("otp-url")
            `when`(env.getRequiredProperty("externalMetadataSourceService.adapter.urlAddition")).thenReturn("/rest/otp/")
            `when`(env.getRequiredProperty("projectTargetService.adapter.url")).thenReturn("ilse-url")
            `when`(env.getRequiredProperty("projectTargetService.adapter.urlAddition")).thenReturn("/rest/ilse/")
            doReturn(value)
                .`when`(jsonApiServiceImplMock).getJsonFromApi(matches("$api-url/rest/$api/method-name"), anyOrNull(), anyOrNull())

            val result = jsonApiServiceImplMock.getJsonFromApi("method-name", apiType = apiType)

            assertThat(result).isEqualTo(value)
        }
    }

    @Test
    fun `test getJsonFromApi with exception`() {
        `when`(env.getRequiredProperty("externalMetadataSourceService.adapter.url")).thenReturn("otp-url")
        `when`(env.getRequiredProperty("externalMetadataSourceService.adapter.urlAddition")).thenReturn("/rest/otp/")
        doThrow(ExternalApiReadException::class.java)
            .`when`(jsonApiServiceImplMock).getJsonFromApi(matches("otp-url/rest/otp/method-name"), anyOrNull(), anyOrNull())

        val result = jsonApiServiceImplMock.getJsonFromApi("method-name", ApiType.OTP)

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `test getJsonFromApi with no adapterService`() {
        val listAppender = initListAppender()

        val result = jsonApiServiceImplMock.getJsonFromApi("method-name", ApiType.OTRS)
        val logsList = listAppender.list

        assertThat(result).isEqualTo("")
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There is no adapterService configured for OTRS")
    }

    @Test
    fun `test getJsonFromApi with no adapterService url`() {
        val listAppender = initListAppender()

        doThrow(IllegalStateException::class.java).`when`(env).getRequiredProperty("externalMetadataSourceService.adapter.url")

        val result = jsonApiServiceImplMock.getJsonFromApi("method-name", ApiType.OTP)
        val logsList = listAppender.list

        assertThat(result).isEqualTo("")
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There is no adapterService url configured for OTP")
    }

    @TestFactory
    fun `test sendJsonToApi to`() = listOf(
        "otp" to ApiType.OTP,
        "ilse" to ApiType.PROJECT_TARGET_SERVICE,
    ).map { (api, apiType) ->
        DynamicTest.dynamicTest(" $apiType") {
            val value = "singleValue"

            doReturn(value).`when`(jsonApiServiceImplMock).postJsonToApi(anyOrNull(), anyOrNull(), anyString(), anyOrNull())
            `when`(env.getRequiredProperty("${apiType.adapterService}.adapter.url")).thenReturn("$api-url")
            `when`(env.getRequiredProperty("${apiType.adapterService}.adapter.urlAddition")).thenReturn("/rest/$api/")
            `when`(env.getRequiredProperty("${apiType.adapterService}.adapter.token")).thenReturn("token")

            jsonApiServiceImplMock.sendJsonToApi("method-name?params=a", apiType)

            verify(jsonApiServiceImplMock, times(1)).postJsonToApi(
                "$api-url/rest/$api/method-name?params=a",
                mapOf("User-Token" to "token"),
                "",
                apiType
            )
        }
    }

    @Test
    fun `test sendJsonToApi with exception`() {
        doThrow(ExternalApiReadException::class.java).`when`(jsonApiServiceImplMock).postJsonToApi(anyOrNull(), anyOrNull(), anyString(), anyOrNull())

        val result = jsonApiServiceImplMock.sendJsonToApi("method-name?a=b", ApiType.PROJECT_TARGET_SERVICE)

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `test sendJsonToApi with no adapterService`() {
        val listAppender = initListAppender()

        val result = jsonApiServiceImplMock.sendJsonToApi("method-name", ApiType.OTRS)
        val logsList = listAppender.list

        assertThat(result).isEqualTo("")
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There is no adapterService configured for OTRS")
    }

    @Test
    fun `test sendJsonToApi with no adapterService url`() {
        val listAppender = initListAppender()

        doThrow(IllegalStateException::class.java).`when`(env).getRequiredProperty("externalMetadataSourceService.adapter.url")

        val result = jsonApiServiceImplMock.sendJsonToApi("method-name", ApiType.OTP)
        val logsList = listAppender.list

        assertThat(result).isEqualTo("")
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There is no adapterService url configured for OTP")
    }

    @Test
    fun `test get list of values with params`() {
        val value = """["a","b","c"]"""

        doReturn(value).`when`(jsonApiServiceImplMock).getJsonFromApi("method-name?a=b", ApiType.OTP)

        val result = jsonApiServiceImplMock.getValues("methodName", mapOf("a" to "b"), ApiType.OTP, object : TypeReference<List<String>>() {})

        assertThat(result).hasSize(3)
        assertThat(result).contains("a")
        assertThat(result).contains("b")
        assertThat(result).contains("c")
    }

    @Test
    fun `test get values`() {
        val value = """{
            "a":"a1",
            "b":"b1",
            "c":"c1"
        }"""

        doReturn(value).`when`(jsonApiServiceImplMock).getJsonFromApi("method-name", ApiType.OTP)

        val result = jsonApiServiceImplMock.getValues("methodName", apiType = ApiType.OTP, typeReference = object : TypeReference<Map<String, String>>() {})

        assertThat(result).hasSize(3)
        assertThat(result::class.java).isEqualTo(LinkedHashMap::class.java)
        assertThat(result.entries.first().key).contains("a")
        assertThat(result.entries.first().value).contains("a1")
        assertThat(result.entries.last().key).contains("c")
        assertThat(result.entries.last().value).contains("c1")
    }

    @Test
    fun `test empty object when JSON is empty`() {
        val value = ""

        doReturn(value).`when`(jsonApiServiceImplMock).getJsonFromApi("method-name?a=b", ApiType.OTP)

        assertThatExceptionOfType(BlankJsonException::class.java).isThrownBy {
            jsonApiServiceImplMock.getValues("methodName", mapOf("a" to "b"), ApiType.OTP, object : TypeReference<Map<String, String>>() {})
        }.withMessageContaining("There was a problem when reading out the JSON ''")
    }
}
