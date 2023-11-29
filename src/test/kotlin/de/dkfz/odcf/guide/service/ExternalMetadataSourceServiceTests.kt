package de.dkfz.odcf.guide.service

import com.fasterxml.jackson.core.type.TypeReference
import de.dkfz.odcf.guide.service.implementation.external.ExternalMetadataSourceServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Spy
import org.springframework.core.env.Environment
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class ExternalMetadataSourceServiceTests {

    @InjectMocks
    @Spy
    lateinit var externalMetadataSourceServiceMock: ExternalMetadataSourceServiceImpl

    @Mock
    lateinit var jsonApiService: JsonApiService

    @Mock
    lateinit var env: Environment

    @Test
    fun `test get single value without params`() {
        val value = "singleValue"

        doReturn(value).`when`(externalMetadataSourceServiceMock).getJsonFromApi("method-name")

        val result = externalMetadataSourceServiceMock.getSingleValue("methodName")

        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `test get single value with params`() {
        val value = "singleValue"

        doReturn(value).`when`(externalMetadataSourceServiceMock).getJsonFromApi("method-name?a=b")

        val result = externalMetadataSourceServiceMock.getSingleValue("methodName", mapOf("a" to "b"))

        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `test get set of values`() {
        val value = """["a","b","c"]"""

        doReturn(value).`when`(externalMetadataSourceServiceMock).getJsonFromApi("method-name")

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

        doReturn(value).`when`(externalMetadataSourceServiceMock).getJsonFromApi("method-name")

        val result = externalMetadataSourceServiceMock.getValuesAsSetMap("methodName")

        assertThat(result).hasSize(1)
        assertThat(result.first()).hasSize(3)
        assertThat(result.first().entries.first().key).contains("a")
        assertThat(result.first().entries.first().value).contains("a1")
        assertThat(result.first().entries.last().key).contains("c")
        assertThat(result.first().entries.last().value).contains("c1")
    }

    @Test
    fun `test get values`() {
        val value = """{
            "a":"a1",
            "b":"b1",
            "c":"c1"
        }"""

        doReturn(value).`when`(externalMetadataSourceServiceMock).getJsonFromApi("method-name")

        val result = externalMetadataSourceServiceMock.getValues("methodName", typeReference = object : TypeReference<Map<String, String>>() {})

        assertThat(result).hasSize(3)
        assertThat(result::class.java).isEqualTo(LinkedHashMap::class.java)
        assertThat(result.entries.first().key).contains("a")
        assertThat(result.entries.first().value).contains("a1")
        assertThat(result.entries.last().key).contains("c")
        assertThat(result.entries.last().value).contains("c1")
    }
}
