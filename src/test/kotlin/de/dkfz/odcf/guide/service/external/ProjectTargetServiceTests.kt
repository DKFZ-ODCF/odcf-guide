package de.dkfz.odcf.guide.service.external

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.exceptions.BlankJsonException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.service.implementation.external.ProjectTargetServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Spy
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProjectTargetServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    @Spy
    lateinit var projectTargetServiceMock: ProjectTargetServiceImpl

    @Mock
    lateinit var jsonApiService: JsonApiService

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(ProjectTargetServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `test send set of values`() {
        projectTargetServiceMock.sendValues("methodName")
        projectTargetServiceMock.sendValues("methodName", mapOf("a" to "b"))
        projectTargetServiceMock.sendValues("methodName", mapOf("c" to "d, e"))

        verify(jsonApiService, times(1)).sendJsonToApi("method-name", ApiType.PROJECT_TARGET_SERVICE)
        verify(jsonApiService, times(1)).sendJsonToApi("method-name?a=b", ApiType.PROJECT_TARGET_SERVICE)
        verify(jsonApiService, times(1)).sendJsonToApi("method-name?c=d%2C+e", ApiType.PROJECT_TARGET_SERVICE)
    }

    @Test
    fun `test get single value from set`() {
        val value = "singleValue"

        `when`(jsonApiService.getJsonFromApi("method-name?params=a&params=b", ApiType.PROJECT_TARGET_SERVICE)).thenReturn(value)

        val result = projectTargetServiceMock.getSingleValueFromSet("methodName", mapOf("params" to setOf("a", "b")))

        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `test get single value from set without params`() {
        val value = "singleValue"

        `when`(jsonApiService.getJsonFromApi("method-name", ApiType.PROJECT_TARGET_SERVICE)).thenReturn(value)

        val result = projectTargetServiceMock.getSingleValueFromSet("methodName")

        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `test get value set from set`() {
        val value = """["a","b","c"]"""

        `when`(jsonApiService.getJsonFromApi("method-name?params=a&params=b", ApiType.PROJECT_TARGET_SERVICE)).thenReturn(value)

        val result = projectTargetServiceMock.getSetOfValuesFromSet("methodName", mapOf("params" to setOf("a", "b")))

        assertThat(result).hasSize(3)
        assertThat(result).contains("a")
        assertThat(result).contains("b")
        assertThat(result).contains("c")
    }

    @Test
    fun `test get value set from set without params`() {
        val value = """["a","b","c"]"""

        `when`(jsonApiService.getJsonFromApi("method-name", ApiType.PROJECT_TARGET_SERVICE)).thenReturn(value)

        val result = projectTargetServiceMock.getSetOfValuesFromSet("methodName")

        assertThat(result).hasSize(3)
        assertThat(result).contains("a")
        assertThat(result).contains("b")
        assertThat(result).contains("c")
    }

    @Test
    fun `test functionality updateProjectsInTarget`() {
        val projects = setOf("project1", "project2")
        val pi1 = entityFactory.getPerson("pi1", "pi", "1")
        val pi2 = entityFactory.getPerson("pi2", "pi", "2")

        `when`(jsonApiService.sendJsonToApi(anyString(), anyOrNull())).thenReturn("1")
        `when`(externalMetadataSourceService.getValuesAsSet("projects-after-year", mapOf("year" to "2023"))).thenReturn(projects)
        `when`(externalMetadataSourceService.getPrincipalInvestigatorsAsPersonSet("project2")).thenReturn(setOf(pi1, pi2))
        `when`(jsonApiService.getValues<Set<String>>(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(setOf("project1"))

        projectTargetServiceMock.updateProjectsInTarget()

        verify(projectTargetServiceMock, times(1)).sendValues("insert-into-project-view", mapOf("project" to "project2", "description" to "pi 1, pi 2"))
        verify(jsonApiService, times(1)).sendJsonToApi("insert-into-project-view?project=project2&description=pi+1%2C+pi+2", ApiType.PROJECT_TARGET_SERVICE)
    }

    @TestFactory
    fun `test functionality updateProjectsInTarget with problems while updating`() = listOf(
        "0",
        "",
    ).map { affectedRows ->
        dynamicTest("affected rows: '$affectedRows'") {
            val listAppender = initListAppender()
            val projects = setOf("projectWithAffectedRows$affectedRows")
            val pi1 = entityFactory.getPerson("pi1", "pi", "1")
            val pi2 = entityFactory.getPerson("pi2", "pi", "2")

            `when`(jsonApiService.sendJsonToApi(anyString(), anyOrNull())).thenReturn(affectedRows)
            `when`(externalMetadataSourceService.getValuesAsSet("projects-after-year", mapOf("year" to "2023"))).thenReturn(projects)
            `when`(jsonApiService.getValues<Set<String>>(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(emptySet())
            `when`(externalMetadataSourceService.getPrincipalInvestigatorsAsPersonSet("projectWithAffectedRows$affectedRows")).thenReturn(setOf(pi1, pi2))

            projectTargetServiceMock.updateProjectsInTarget()
            val logsList = listAppender.list

            verify(projectTargetServiceMock, times(1)).sendValues("insert-into-project-view", mapOf("project" to "projectWithAffectedRows$affectedRows", "description" to "pi 1, pi 2"))
            verify(jsonApiService, times(1)).sendJsonToApi("insert-into-project-view?project=projectWithAffectedRows$affectedRows&description=pi+1%2C+pi+2", ApiType.PROJECT_TARGET_SERVICE)
            assertThat(logsList).hasSize(1)
            assertThat(logsList.first().level).isEqualTo(Level.WARN)
            assertThat(logsList.first().message).isEqualTo("There was a problem while sending project 'projectWithAffectedRows$affectedRows' to the projectTargetService.")
        }
    }

    @Test
    fun `test functionality updateProjectsInTarget with exception`() {
        val projects = setOf("project1", "project2")
        val listAppender = initListAppender()

        `when`(externalMetadataSourceService.getValuesAsSet("projects-after-year", mapOf("year" to "2023"))).thenReturn(projects)
        `when`(jsonApiService.getValues<Set<String>>(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenThrow(BlankJsonException("There was a problem when reading out the JSON ''"))

        projectTargetServiceMock.updateProjectsInTarget()
        val logsList = listAppender.list

        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There was a problem when reading out the JSON ''")
        verify(projectTargetServiceMock, times(0)).sendValues("insert-into-project-view", mapOf("project" to "project2", "description" to "pi1, pi2"))
        verify(jsonApiService, times(0)).sendJsonToApi("insert-into-project-view?project=project2&description=pi1%2C+pi2", ApiType.PROJECT_TARGET_SERVICE)
    }

    @Test
    fun `test functionality updateProjectsInTarget with no otpProjects`() {
        val listAppender = initListAppender()

        `when`(externalMetadataSourceService.getValuesAsSet("projects-after-year", mapOf("year" to "2023"))).thenReturn(emptySet())
        `when`(jsonApiService.getValues<Set<String>>(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(setOf("project1"))

        projectTargetServiceMock.updateProjectsInTarget()
        val logsList = listAppender.list

        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).isEqualTo("There was a problem while trying to get the project lists, did not update projects in target.")
        verify(projectTargetServiceMock, times(0)).sendValues("insert-into-project-view", mapOf("project" to "project2", "description" to "pi1, pi2"))
        verify(jsonApiService, times(0)).sendJsonToApi("insert-into-project-view?project=project2&description=pi1%2C+pi2", ApiType.PROJECT_TARGET_SERVICE)
    }
}
