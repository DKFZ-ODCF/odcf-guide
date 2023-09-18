package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.OtpCachedProjectRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.otpCached.OtpCachedProject
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.projectOverview.ProjectServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.external.SqlService
import de.dkfz.odcf.guide.service.interfaces.projectOverview.ProjectService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment

@SpringBootTest
class ProjectServiceTests @Autowired constructor(private val projectService: ProjectService) : AnyObject {

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
    lateinit var otpCachedProjectRepository: OtpCachedProjectRepository

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
        var project = OtpCachedProject()

        `when`(externalMetadataSourceService.getSetOfMapOfValues("projectInfos")).thenReturn(setOf(map))
        `when`(externalMetadataSourceService.getSetOfValues("seqTypesByProject", mapOf("project" to "project"))).thenReturn(setOf("WGS", "RNA"))
        `when`(externalMetadataSourceService.getSingleValue("lastDataRecdByProject", mapOf("project" to "project"))).thenReturn("last")
        `when`(externalMetadataSourceService.getSetOfValues("pisByProject", mapOf("project" to "project"))).thenReturn(setOf("pi1", "pi2"))
        `when`(runtimeOptionsRepository.findByName("projectPathPrefix")).thenReturn(entityFactory.getRuntimeOption("/prefix/"))

        `when`(otpCachedProjectRepository.save(anyOtpCachedProject())).then {
            val argumentProject = it.arguments[0]
            counter++
            project = argumentProject as OtpCachedProject
            argumentProject
        }
        projectServiceMock.storeProjectInfosFromOtp()

        assertThat(counter).isEqualTo(1)
        assertThat(project.name).isEqualTo("project")
        assertThat(project.closed).isEqualTo(true)
        assertThat(project.unixGroup).isEqualTo("unix")
        assertThat(project.pis).isEqualTo("pi1, pi2")
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

        `when`(externalMetadataSourceService.getSetOfMapOfValues("projectInfos")).thenReturn(setOf(map))
        `when`(runtimeOptionsRepository.findByName("projectPathPrefix")).thenReturn(entityFactory.getRuntimeOption())

        `when`(otpCachedProjectRepository.save(anyOtpCachedProject())).then {
            counter++
            it.arguments[0]
        }
        projectServiceMock.storeProjectInfosFromOtp()

        assertThat(counter).isEqualTo(0)
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
        val project1 = entityFactory.getOtpCachedProject()
        val project2 = entityFactory.getOtpCachedProject()

        `when`(otpCachedProjectRepository.findAll()).thenReturn(listOf(project1, project2))
        `when`(otpCachedProjectRepository.delete(anyOtpCachedProject())).then {
            counter++
        }

        projectServiceMock.deleteProjectsNotRepresentedAnymore(setOf(map))

        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun `store project infos all valid`() {
        val otpCachedProject = entityFactory.getOtpCachedProject()

        mockDb()
        `when`(otpCachedProjectRepository.findAll()).thenReturn(listOf(otpCachedProject))
        `when`(sqlService.getFromRemote(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf("987654321"))

        projectServiceMock.storeProjectStorageInfos()

        assertThat(otpCachedProject.sizeProjectFolder).isEqualTo(987654321)
        assertThat(otpCachedProject.sizeAnalysisFolder).isEqualTo(987654321)
        assertThat(otpCachedProject.getProjectSize()).isEqualTo("1 GB")
        assertThat(otpCachedProject.getAnalysisSize()).isEqualTo("1 GB")
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
        val otpCachedProject = entityFactory.getOtpCachedProject()
        val startMap = mapOf("project" to otpCachedProject.name)

        `when`(otpCachedProjectRepository.findByName(otpCachedProject.name)).thenReturn(otpCachedProject)

        val result = projectServiceMock.prepareMap(startMap)

        assertThat(result["project"]).isEqualTo(otpCachedProject.name)
        assertThat(result["unix"]).isEqualTo(otpCachedProject.unixGroup)
        assertThat(result["pis"]).isEqualTo(otpCachedProject.pis)
        assertThat(result["closed"]).isEqualTo(otpCachedProject.closed.toString().substring(0, 1))
        assertThat(result["projectSize"]).isEqualTo(otpCachedProject.getProjectSize())
        assertThat(result["analysisSize"]).isEqualTo(otpCachedProject.getAnalysisSize())
        assertThat(result["kindOfData"]).isEqualTo(otpCachedProject.seqTypes)
        assertThat(result["lastDataReceived"]).isEqualTo(otpCachedProject.lastDataReceived)
    }

    @Test
    fun `test prepare map null`() {
        val startMap = mapOf("project" to "project")

        `when`(otpCachedProjectRepository.findByName("project")).thenReturn(null)

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
