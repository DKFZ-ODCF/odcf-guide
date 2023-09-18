package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.OtpCachedProjectRepository
import de.dkfz.odcf.guide.PersonRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.projectOverview.ProjectService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/project")
class ProjectController(
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val projectService: ProjectService,
    private val ldapService: LdapService,
    private val otpCachedProjectRepository: OtpCachedProjectRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val personRepository: PersonRepository,
    private val env: Environment
) {

    @RequestMapping("/overview")
    fun showProjectOverview(): String {
        return if (ldapService.isCurrentUserAdmin()) {
            "redirect:/project/overview/admin"
        } else {
            "redirect:/project/overview/user"
        }
    }

    @RequestMapping("/overview/user")
    fun showProject(model: Model): String {
        val infosByPerson = emptyList<Any>().toMutableList()
        val user = ldapService.getPerson()

        externalMetadataSourceService.getSetOfMapOfValues("InfosByPerson", mapOf("username" to user.username)).forEach {
            infosByPerson.add(projectService.prepareMap(it))
        }

        val (infosByPublic, infosByGroup) = otpCachedProjectRepository.findAllByNameIn(
            externalMetadataSourceService.getSetOfValues(
                "projects-by-person-or-organizational-unit",
                mapOf("username" to user.username, "organizationalUnit" to user.organizationalUnit)
            ).map { it.removeSuffix("(f)").removeSuffix("(t)") }.toSet()
        ).sortedBy { it.name }.partition { it.unixGroup == runtimeOptionsRepository.findByName("publicGroup")?.value.orEmpty() }

        model["infosByPerson"] = infosByPerson
        model["infosByGroup"] = infosByGroup
        model["infosByPublic"] = infosByPublic
        model["otpUrlOverview"] = env.getRequiredProperty("otp.url.overview")
        model["otpUrlUserManagement"] = env.getRequiredProperty("otp.url.userManagement")
        model["otpProjectPath"] = runtimeOptionsRepository.findByName("otpProjectPath")?.value.orEmpty()
        model["analysisSize"] = projectService.getOEAnalysisFolderSize(user.organizationalUnit)
        model["organizationalUnit"] = user.organizationalUnit

        return "project/overview/user"
    }

    @RequestMapping("/overview/admin")
    fun showProjectAdmin(model: Model): String {
        if (!ldapService.isCurrentUserAdmin()) {
            return "redirect:/project/overview/user"
        }
        model["otpCachedProjects"] = otpCachedProjectRepository.findAll().sortedBy { it.name }
        model["otpUrlOverview"] = env.getRequiredProperty("otp.url.overview")
        model["otpUrlUserManagement"] = env.getRequiredProperty("otp.url.userManagement")
        model["otpProjectPath"] = runtimeOptionsRepository.findByName("otpProjectPath")?.value.orEmpty()

        return "project/overview/admin"
    }

    @GetMapping("/get-quota-settings", produces = [MediaType.TEXT_PLAIN_VALUE])
    @ResponseBody
    fun getQuotaSettings(@RequestParam token: String): ResponseEntity<*> {
        val isAuthorized = personRepository.findByApiToken(token)?.isAdmin ?: false
        if (!isAuthorized) {
            return ResponseEntity("You are not allowed, to access this page!\n", HttpHeaders(), HttpStatus.FORBIDDEN)
        }

        val otpCachedProjects = otpCachedProjectRepository.findAll().sortedBy { it.name }
        var quotaSettings = ""

        val defaultProjectQuota = runtimeOptionsRepository.findByName("projectFolderQuota")?.value
        val defaultAnalysisQuota = runtimeOptionsRepository.findByName("analysisFolderQuota")?.value

        for (project in otpCachedProjects) {
            val quotaProject =
                if (project.quotaProjectFolder != (-1).toLong()) project.getProjectQuotaSize()
                else defaultProjectQuota
            val quotaAnalysis =
                if (project.quotaAnalysisFolder != (-1).toLong()) project.getAnalysisQuotaSize()
                else defaultAnalysisQuota

            quotaSettings += project.pathProjectFolder + "\t" + quotaProject + "\n"
            quotaSettings +=
                if (project.pathAnalysisFolder != "") project.pathAnalysisFolder + "\t" + quotaAnalysis + "\n"
                else ""
        }
        return ResponseEntity(quotaSettings, HttpHeaders(), HttpStatus.OK)
    }
}
