package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.UrlGeneratorServiceImpl
import de.dkfz.odcf.guide.service.interfaces.UrlGeneratorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment

@SpringBootTest
class UrlGeneratorServiceTests @Autowired constructor(private val urlGeneratorService: UrlGeneratorService) {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var urlGeneratorServiceMock: UrlGeneratorServiceImpl

    @Mock
    lateinit var env: Environment

    @Test
    fun `get url`() {
        val submission = entityFactory.getApiSubmission()

        `when`(env.getRequiredProperty("application.serverUrl")).thenReturn("http://url.de")

        val url = urlGeneratorServiceMock.getURL(submission)

        assertThat(url).isEqualTo("http://url.de/metadata-validator/submission/simple/user?uuid=${submission.uuid}")
    }

    @Test
    fun `get admin url`() {
        val submission = entityFactory.getApiSubmission()

        `when`(env.getRequiredProperty("application.serverUrl")).thenReturn("http://url.de")

        val url = urlGeneratorServiceMock.getAdminURL(submission)

        assertThat(url).isEqualTo("http://url.de/metadata-validator/submission/simple/admin?identifier=${submission.identifier}")
    }
}
