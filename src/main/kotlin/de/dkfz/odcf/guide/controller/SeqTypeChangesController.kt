package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.SeqTypeRepository
import de.dkfz.odcf.guide.service.interfaces.security.AuthorizationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/admin/sequencing-type")
class SeqTypeChangesController(
    private val authorizationService: AuthorizationService,
    private val seqTypeRepository: SeqTypeRepository,
) {

    @GetMapping("/get-seq-types")
    fun getActiveRequests(@RequestHeader(value = "User-Token", required = false) token: String?): ResponseEntity<*> {
        authorizationService.checkAuthorization(token)?.let { return it }
        val seqTypes = seqTypeRepository.findAllByIsRequestedIsFalseOrderByNameAsc().toSet()
        val result = seqTypes.map { seqType ->
            mapOf(
                "id" to seqType.id,
                "name" to seqType.name,
                "importAliases" to seqType.importAliases,
                "basicSeqType" to seqType.basicSeqType,
                "seqTypeOptions" to seqType.seqTypeOptions
            )
        }
        return ResponseEntity(mapOf("data" to result), HttpHeaders(), HttpStatus.OK)
    }
}
