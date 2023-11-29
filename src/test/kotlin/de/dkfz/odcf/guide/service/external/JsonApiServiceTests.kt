package de.dkfz.odcf.guide.service.external

import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.service.implementation.external.JsonApiServiceImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Spy
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClientRequestException

@ExtendWith(SpringExtension::class)
class JsonApiServiceTests {

    @InjectMocks
    @Spy
    lateinit var jsonApiServiceImplMock: JsonApiServiceImpl

    @Test
    fun `Test getJsonFromApi via invalid url throws exception`() {
        val url = "some-fake-url"

        Assertions.assertThatExceptionOfType(WebClientRequestException::class.java).isThrownBy {
            jsonApiServiceImplMock.getJsonFromApi("https://$url", emptyMap(), ApiType.ILSe)
        }.withMessageContaining("failed to resolve '$url")
    }
}
