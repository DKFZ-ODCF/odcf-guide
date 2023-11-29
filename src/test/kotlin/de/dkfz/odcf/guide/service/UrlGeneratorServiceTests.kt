package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.UrlGeneratorServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.core.env.Environment
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class UrlGeneratorServiceTests {

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
