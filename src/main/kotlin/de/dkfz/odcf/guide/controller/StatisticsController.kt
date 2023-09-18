package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/statistics")
class StatisticsController(
    private val authorizationService: AuthorizationService,
    private val submissionRepository: SubmissionRepository,
) {

    @GetMapping("")
    fun getPage(): String {
        return "admin/statistics"
    }

    @GetMapping("/get-submission-size-by-state")
    fun getSubmissionSizeByState(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }
        val result = submissionRepository.getSubmissionAndSampleSizeByState()
        return ResponseEntity(result, HttpHeaders(), HttpStatus.OK)
    }

    @GetMapping("/get-submission-size-by-months")
    fun getSubmissionSizeByMonths(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }
        val result = submissionRepository.getSubmissionAndSampleSizeByDate()
        return ResponseEntity(result, HttpHeaders(), HttpStatus.OK)
    }
}
