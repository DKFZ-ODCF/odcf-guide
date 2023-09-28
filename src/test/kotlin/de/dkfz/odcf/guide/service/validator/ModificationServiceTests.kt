package de.dkfz.odcf.guide.service.validator

import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.validator.ModificationServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.validator.ModificationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ModificationServiceTests @Autowired constructor(private val modificationService: ModificationService) {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var modificationServiceMock: ModificationServiceImpl

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Test
    fun `check remove prefix from pids for samples in submission`() {
        val submission = entityFactory.getApiSubmission()
        val sample1 = entityFactory.getSample(submission)
        val sample2 = entityFactory.getSample(submission)
        sample2.pid = "prefix-pid2"
        val projectPrefixMapping = emptyMap<String, String?>().toMutableMap()
        val prefix = "prefix_"

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample1, sample2))
        `when`(externalMetadataSourceService.getSingleValue(matches("projectPrefixByProject"), anyMap())).thenReturn(prefix)

        modificationServiceMock.removeProjectPrefixFromPids(submission, projectPrefixMapping)

        assertThat(sample1.pid).doesNotContain(prefix)
        assertThat(sample2.pid).isEqualTo("pid2")
    }

    @Test
    fun `check remove project prefix and prefix delimiter is equal`() {
        val sample = entityFactory.getSample()
        val projectPrefixMapping = emptyMap<String, String?>().toMutableMap()
        val prefix = "prefix_"

        `when`(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to sample.project))).thenReturn(prefix)

        modificationServiceMock.removeProjectPrefixFromPid(sample, projectPrefixMapping)

        assertThat(projectPrefixMapping.size).isEqualTo(1)
        assertThat(sample.pid).doesNotContain(prefix)
    }

    @Test
    fun `check remove project prefix and prefix delimiter is different`() {
        val sample = entityFactory.getSample()
        val projectPrefixMapping = emptyMap<String, String?>().toMutableMap()
        val prefix = "prefix-"

        `when`(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to sample.project))).thenReturn(prefix)

        modificationServiceMock.removeProjectPrefixFromPid(sample, projectPrefixMapping)

        assertThat(projectPrefixMapping.size).isEqualTo(1)
        assertThat(sample.pid).doesNotContain(prefix)
    }

    @Test
    fun `check remove project prefix and prefix has no`() {
        val sample = entityFactory.getSample()
        val projectPrefixMapping = emptyMap<String, String?>().toMutableMap()
        val prefix = "prefix"

        `when`(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to sample.project))).thenReturn(prefix)

        modificationServiceMock.removeProjectPrefixFromPid(sample, projectPrefixMapping)

        assertThat(projectPrefixMapping.size).isEqualTo(1)
        assertThat(sample.pid).doesNotContain(prefix)
    }
}
