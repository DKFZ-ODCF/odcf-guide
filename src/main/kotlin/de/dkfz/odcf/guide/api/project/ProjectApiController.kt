package de.dkfz.odcf.guide.api.project

import de.dkfz.odcf.guide.ProjectRepository
import de.dkfz.odcf.guide.RoleRepository
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectApiController(
    private val authorizationService: AuthorizationService,
    private val projectRepository: ProjectRepository,
    private val roleRepository: RoleRepository
) : ProjectApiInterface {

    override fun getOverview(token: String): ResponseEntity<*> {
        val role = roleRepository.findByName("PROJECT_OVERVIEW")
        authorizationService.checkIfTokenIsAuthorized(token, role)?.let { return it }
        return ResponseEntity(projectRepository.findAllByOrderByNameAsc(ProjectApiView::class.java), HttpHeaders(), HttpStatus.OK)
    }
}
