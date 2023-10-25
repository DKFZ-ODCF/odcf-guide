package de.dkfz.odcf.guide.service.implementation.projectOverview

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.dkfz.odcf.guide.ProjectRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.storage.Project
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.SqlService
import de.dkfz.odcf.guide.service.interfaces.projectOverview.ProjectService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.sql.Connection
import java.text.DecimalFormat
import java.util.*
import kotlin.math.pow

@Service
open class ProjectServiceImpl(
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val sqlService: SqlService,
    private val ldapService: LdapService,
    private val projectRepository: ProjectRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val env: Environment
) : ProjectService {

    /**
     * Connection pool to quota database
     */
    private var quotaDataSource: HikariDataSource? = null

    /**
     * @return a connection from [quotaDataSource] and initialize it if needed
     */
    private val poolConnection: Connection
        @Synchronized get() {
            if (quotaDataSource == null) {
                val config = HikariConfig()
                config.jdbcUrl = env.getRequiredProperty("quota.datasource.url")
                config.username = env.getRequiredProperty("quota.datasource.username")
                config.password = env.getRequiredProperty("quota.datasource.password")
                config.maximumPoolSize = 10
                config.poolName = "quota-db-pool"
                config.addDataSourceProperty("cachePrepStmts", "true")
                config.addDataSourceProperty("prepStmtCacheSize", "250")
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                quotaDataSource = HikariDataSource(config)
            }
            return quotaDataSource!!.connection
        }

    private val logger = LoggerFactory.getLogger(this::class.java)

    /** Regularly caches project infos from OTP in the GUIDE. */
    @Scheduled(cron = "\${application.projectOverview.cron.otp}")
    open fun storeProjectInfosFromOtp() {
        val otpProjects = externalMetadataSourceService.getSetOfMapOfValues("projectInfos")
        val projectPathPrefix = runtimeOptionsRepository.findByName("projectPathPrefix")!!.value
        otpProjects.forEach {
            val projectName = it["project"]
            if (projectName != null) {
                val project = projectRepository.findByName(projectName) ?: Project()
                project.name = projectName
                project.latestUpdate = Date()
                project.unixGroup = it["unix"].orEmpty()
                project.pis = externalMetadataSourceService.getSetOfValues("pisByProject", mapOf("project" to projectName))
                    .mapNotNull { username ->
                        try {
                            ldapService.getPersonByUsername(username)
                        } catch (e: Exception) {
                            logger.warn("user '$username' not added as PI to project $projectName with reason:\n${e.localizedMessage}")
                            null
                        }
                    }.toSet()
                project.closed = it["closed"]?.equals("t") ?: false
                project.pathProjectFolder = if (it["dir_project"] != null) "${projectPathPrefix}${it["dir_project"]}" else ""
                project.pathAnalysisFolder = it["dir_analysis"].orEmpty()
                project.seqTypes = externalMetadataSourceService.getSetOfValues("seqTypesByProject", mapOf("project" to projectName)).joinToString()
                project.lastDataReceived = externalMetadataSourceService.getSingleValue("lastDataRecdByProject", mapOf("project" to projectName))
                projectRepository.save(project)
            }
        }
        deleteProjectsNotRepresentedAnymore(otpProjects)
    }

    /** Regularly updates the information about the size of the project storage folders. */
    @Scheduled(cron = "\${application.projectOverview.cron.filesystem}")
    open fun storeProjectStorageInfos() {
        projectRepository.findAll().forEach { project ->
            val sql = "SELECT size_phys FROM usage\n" +
                "JOIN folders f ON f.id = usage.path_id\n" +
                "WHERE mount_path = '<PATH>'\n" +
                "ORDER BY date DESC LIMIT 1"
            project.sizeProjectFolder = (
                sqlService.getFromRemote(
                    poolConnection,
                    "size_phys",
                    sql.replace("<PATH>", project.pathProjectFolder)
                ).firstOrNull() ?: "-1"
                ).toLong()
            project.sizeAnalysisFolder = (
                sqlService.getFromRemote(
                    poolConnection,
                    "size_phys",
                    sql.replace("<PATH>", project.pathAnalysisFolder)
                ).firstOrNull() ?: "-1"
                ).toLong()
            projectRepository.save(project)
        }
    }

    override fun prepareMap(it: Map<String, String>): Map<String, String?> {
        val notLoaded = "Not loaded yet"
        val map = emptyMap<String, String?>().toMutableMap()
        map.putAll(it)
        val project = projectRepository.findByName(it["project"]!!)
        map["unix"] = project?.unixGroup ?: notLoaded
        map["pis"] = project?.getPiFullNames() ?: notLoaded
        map["closed"] = project?.closed.toString().substring(0, 1)
        map["projectSize"] = project?.getProjectSize() ?: notLoaded
        map["analysisSize"] = project?.getAnalysisSize() ?: notLoaded
        map["kindOfData"] = project?.seqTypes ?: notLoaded
        map["lastDataReceived"] = project?.lastDataReceived ?: notLoaded
        return map
    }

    /**
     * Deletes projects from the projects cached in the GUIDE database that are no longer represented in OTP.
     *
     * @param otpProjects List of project information.
     *     The project names in that list are used to check whether the given projects are still represented in OTP.
     */
    fun deleteProjectsNotRepresentedAnymore(otpProjects: Set<Map<String, String>>) {
        val otpProjectNames = otpProjects.map { it["project"] }
        projectRepository.findAll().forEach {
            if (!otpProjectNames.contains(it.name)) {
                projectRepository.delete(it)
            }
        }
    }

    override fun getOEAnalysisFolderSize(organizationalUnit: String): String {
        val sql = "SELECT usage.size_phys FROM folders\n" +
            "JOIN usage on folders.id = usage.path_id\n" +
            "WHERE folders.path LIKE '%analysis/$organizationalUnit'\n" +
            "ORDER BY usage.date desc limit 1"
        val size = (
            sqlService.getFromRemote(
                connection = poolConnection,
                propertyToGet = "size_phys",
                sql = sql
            ).firstOrNull() ?: "-1"
            ).toLong()
        val dec = DecimalFormat("###,###,##0")
        return if (size > 0) {
            // from B (2^0) to GB (2^30)
            "${dec.format(size / 2.0.pow(30))} GB"
        } else {
            "N/A"
        }
    }
}
