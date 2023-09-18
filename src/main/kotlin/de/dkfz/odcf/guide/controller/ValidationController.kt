package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.ValidationRepository
import de.dkfz.odcf.guide.entity.validation.Validation
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/admin/validation")
class ValidationController(
    private val authorizationService: AuthorizationService,
    private val validationRepository: ValidationRepository,
) {

    @GetMapping("/get-data")
    @ResponseBody
    fun getSingleValidation(@RequestParam("validationId") validationId: Int): Validation? {
        return validationRepository.findById(validationId).orElse(null)
    }

    @GetMapping("/get-all")
    fun getAllValidations(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }
        val validations = validationRepository.findAll().toSet()
        val result = validations.map { validation ->
            mapOf(
                "id" to validation.id,
                "validationName" to validation.field,
                "regex" to validation.regex,
                "required" to validation.required,
                "description" to validation.description
            )
        }
        return ResponseEntity(mapOf("data" to result), HttpHeaders(), HttpStatus.OK)
    }
}
