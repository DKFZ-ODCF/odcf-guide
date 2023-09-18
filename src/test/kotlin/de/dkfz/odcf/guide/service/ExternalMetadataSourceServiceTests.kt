package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.service.implementation.external.ExternalMetadataSourceServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Spy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment

@SpringBootTest
class ExternalMetadataSourceServiceTests @Autowired constructor(private val externalMetadataSourceService: ExternalMetadataSourceService) {

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

        val result = externalMetadataSourceServiceMock.getSetOfValues("methodName")

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

        val result = externalMetadataSourceServiceMock.getSetOfMapOfValues("methodName")

        assertThat(result).hasSize(1)
        assertThat(result.first()).hasSize(3)
        assertThat(result.first().entries.first().key).contains("a")
        assertThat(result.first().entries.first().value).contains("a1")
        assertThat(result.first().entries.last().key).contains("c")
        assertThat(result.first().entries.last().value).contains("c1")
    }
}
