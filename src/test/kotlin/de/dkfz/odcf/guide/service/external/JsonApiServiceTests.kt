package de.dkfz.odcf.guide.service.external

import de.dkfz.odcf.guide.exceptions.ApiType
import de.dkfz.odcf.guide.service.implementation.external.JsonApiServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Spy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClientRequestException

@SpringBootTest
class JsonApiServiceTests @Autowired constructor(val jsonApiService: JsonApiService) {

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
