package de.dkfz.odcf.guide.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.ProjectRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.storage.Project
import de.dkfz.odcf.guide.exceptions.UserNotFoundException
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.projectOverview.ProjectServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.external.SqlService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class ProjectServiceTests : AnyObject {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var projectServiceMock: ProjectServiceImpl

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var sqlService: SqlService

    @Mock
    lateinit var remoteCommandsService: RemoteCommandsService

    @Mock
    lateinit var ldapService: LdapService

    @Mock
    lateinit var projectRepository: ProjectRepository

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var env: Environment

    private fun mockDb() {
        `when`(env.getRequiredProperty("quota.datasource.url")).thenReturn("jdbc:h2:mem:testdb")
        `when`(env.getRequiredProperty("quota.datasource.username")).thenReturn("sa")
        `when`(env.getRequiredProperty("quota.datasource.password")).thenReturn("password")
        `when`(env.getRequiredProperty("quota.datasource.maximumPoolSize")).thenReturn("1")
    }

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(ProjectServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @Test
    fun `get project infos`() {
        val map = mapOf(
            "project" to "project",
            "closed" to "t",
            "unix" to "unix",
            "dir_project" to "dir_project",
            "dir_analysis" to "dir_analysis"
        )
        var counter = 0
        var project = Project()
        val pi1 = entityFactory.getPerson()
        pi1.username = "pi1"
        val pi2 = entityFactory.getPerson()
        pi2.username = "pi2"

        `when`(externalMetadataSourceService.getValuesAsSetMap("projectInfos")).thenReturn(setOf(map))
        `when`(externalMetadataSourceService.getValuesAsSet("seqTypesByProject", mapOf("project" to "project"))).thenReturn(setOf("WGS", "RNA"))
        `when`(externalMetadataSourceService.getSingleValue("lastDataRecdByProject", mapOf("project" to "project"))).thenReturn("last")
        `when`(externalMetadataSourceService.getValuesAsSet("pisByProject", mapOf("project" to "project"))).thenReturn(setOf("pi1", "pi2"))
        `when`(ldapService.getPersonByUsername("pi1")).thenReturn(pi1)
        `when`(ldapService.getPersonByUsername("pi2")).thenReturn(pi2)
        `when`(runtimeOptionsRepository.findByName("projectPathPrefix")).thenReturn(entityFactory.getRuntimeOption("/prefix/"))
        `when`(projectRepository.save(anyOtpCachedProject())).then {
            val argumentProject = it.arguments[0]
            counter++
            project = argumentProject as Project
            argumentProject
        }

        projectServiceMock.storeProjectInfosFromOtp()

        assertThat(counter).isEqualTo(1)
        assertThat(project.name).isEqualTo("project")
        assertThat(project.closed).isEqualTo(true)
        assertThat(project.unixGroup).isEqualTo("unix")
        assertThat(project.pis).isEqualTo(setOf(pi1, pi2))
        assertThat(project.seqTypes).isEqualTo("WGS, RNA")
        assertThat(project.lastDataReceived).isEqualTo("last")
        assertThat(project.pathProjectFolder).isEqualTo("/prefix/dir_project")
        assertThat(project.sizeProjectFolder).isEqualTo(0)
        assertThat(project.pathAnalysisFolder).isEqualTo("dir_analysis")
        assertThat(project.sizeAnalysisFolder).isEqualTo(0)
    }

    @Test
    fun `get project infos no project name`() {
        val map = mapOf(
            "closed" to "t",
            "unix" to "unix",
            "dir_project" to "dir_project",
            "dir_analysis" to "dir_analysis"
        )
        var counter = 0

        `when`(externalMetadataSourceService.getValuesAsSetMap("projectInfos")).thenReturn(setOf(map))
        `when`(runtimeOptionsRepository.findByName("projectPathPrefix")).thenReturn(entityFactory.getRuntimeOption())
        `when`(projectRepository.save(anyOtpCachedProject())).then {
            counter++
            it.arguments[0]
        }

        projectServiceMock.storeProjectInfosFromOtp()

        assertThat(counter).isEqualTo(0)
    }

    @Test
    fun `get project infos user not found`() {
        val map = mapOf("project" to "project")
        var project = Project()
        val pi1 = entityFactory.getPerson()
        pi1.username = "pi1"
        val pi2 = entityFactory.getPerson()
        pi2.username = "pi2"
        val listAppender = initListAppender()

        `when`(externalMetadataSourceService.getValuesAsSetMap("projectInfos")).thenReturn(setOf(map))
        `when`(externalMetadataSourceService.getValuesAsSet("seqTypesByProject", mapOf("project" to "project"))).thenReturn(setOf("WGS", "RNA"))
        `when`(externalMetadataSourceService.getSingleValue("lastDataRecdByProject", mapOf("project" to "project"))).thenReturn("last")
        `when`(externalMetadataSourceService.getValuesAsSet("pisByProject", mapOf("project" to "project"))).thenReturn(setOf("pi1", "pi2"))
        `when`(ldapService.getPersonByUsername("pi1")).thenReturn(pi1)
        `when`(ldapService.getPersonByUsername("pi2")).thenThrow(UserNotFoundException("not found"))
        `when`(runtimeOptionsRepository.findByName("projectPathPrefix")).thenReturn(entityFactory.getRuntimeOption("/prefix/"))
        `when`(projectRepository.save(anyOtpCachedProject())).then {
            val argumentProject = it.arguments[0]
            project = argumentProject as Project
            argumentProject
        }

        projectServiceMock.storeProjectInfosFromOtp()

        assertThat(project.pis).contains(pi1)
        assertThat(project.pis).doesNotContain(pi2)
        val logsList = listAppender.list
        assertThat(logsList).hasSize(1)
        assertThat(logsList.first().level).isEqualTo(Level.WARN)
        assertThat(logsList.first().message).startsWith("user '${pi2.username}' not added as PI to project ${project.name} with reason")
    }

    @Test
    fun `get project infos and delete one project`() {
        val map = mapOf(
            "project" to "project1",
            "closed" to "t",
            "unix" to "unix",
            "dir_project" to "dir_project",
            "dir_analysis" to "dir_analysis"
        )
        var counter = 0
        val project1 = entityFactory.getProject()
        val project2 = entityFactory.getProject()

        `when`(projectRepository.findAll()).thenReturn(listOf(project1, project2))
        `when`(projectRepository.delete(anyOtpCachedProject())).then {
            counter++
        }

        projectServiceMock.deleteProjectsNotRepresentedAnymore(setOf(map))

        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun `store project infos all valid`() {
        val project = entityFactory.getProject()

        mockDb()
        `when`(projectRepository.findAll()).thenReturn(listOf(project))
        `when`(sqlService.getFromRemote(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf("987654321"))

        projectServiceMock.storeProjectStorageInfos()

        assertThat(project.sizeProjectFolder).isEqualTo(987654321)
        assertThat(project.sizeAnalysisFolder).isEqualTo(987654321)
        assertThat(project.getProjectSize()).isEqualTo("1 GB")
        assertThat(project.getAnalysisSize()).isEqualTo("1 GB")
    }

    @Test
    fun `get OE analysis folder size`() {
        mockDb()
        `when`(sqlService.getFromRemote(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf("987654321"))

        val result = projectServiceMock.getOEAnalysisFolderSize("OE123")

        assertThat(result).isEqualTo("1 GB")
    }

    @Test
    fun `get OE analysis folder size NA`() {
        mockDb()
        `when`(sqlService.getFromRemote(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf("0"))

        val result = projectServiceMock.getOEAnalysisFolderSize("OE123")

        assertThat(result).isEqualTo("N/A")
    }

    @Test
    fun `test prepare map`() {
        val project = entityFactory.getProject()
        val startMap = mapOf("project" to project.name)

        `when`(projectRepository.findByName(project.name)).thenReturn(project)

        val result = projectServiceMock.prepareMap(startMap)

        assertThat(result["project"]).isEqualTo(project.name)
        assertThat(result["unix"]).isEqualTo(project.unixGroup)
        assertThat(result["pis"]).isEqualTo(project.getPiFullNames())
        assertThat(result["closed"]).isEqualTo(project.closed.toString().substring(0, 1))
        assertThat(result["projectSize"]).isEqualTo(project.getProjectSize())
        assertThat(result["analysisSize"]).isEqualTo(project.getAnalysisSize())
        assertThat(result["kindOfData"]).isEqualTo(project.seqTypes)
        assertThat(result["lastDataReceived"]).isEqualTo(project.lastDataReceived)
    }

    @Test
    fun `test prepare map null`() {
        val startMap = mapOf("project" to "project")

        `when`(projectRepository.findByName("project")).thenReturn(null)

        val result = projectServiceMock.prepareMap(startMap)

        assertThat(result["project"]).isEqualTo("project")
        assertThat(result["unix"]).isEqualTo("Not loaded yet")
        assertThat(result["pis"]).isEqualTo("Not loaded yet")
        assertThat(result["closed"]).isEqualTo("n")
        assertThat(result["projectSize"]).isEqualTo("Not loaded yet")
        assertThat(result["analysisSize"]).isEqualTo("Not loaded yet")
        assertThat(result["kindOfData"]).isEqualTo("Not loaded yet")
        assertThat(result["lastDataReceived"]).isEqualTo("Not loaded yet")
    }
}
