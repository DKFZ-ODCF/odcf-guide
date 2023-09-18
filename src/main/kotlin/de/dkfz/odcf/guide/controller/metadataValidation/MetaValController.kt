package de.dkfz.odcf.guide.controller.metadataValidation

import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/metadata-validator")
class MetaValController(
    private val ldapService: LdapService
) {

    companion object {
        const val SIMPLE_TABLE_PAGE_USER = "/metadata-validator/submission/simple/user"
        const val EXTENDED_TABLE_PAGE_USER = "/metadata-validator/submission/extended/user"
        const val SIMPLE_TABLE_PAGE_ADMIN = "/metadata-validator/submission/simple/admin"
        const val EXTENDED_TABLE_PAGE_ADMIN = "/metadata-validator/submission/extended/admin"
        const val SIMPLE_READ_ONLY = "/metadata-validator/submission/simple/read-only"
        const val EXTENDED_READ_ONLY = "/metadata-validator/submission/extended/read-only"
        const val LOCKED_TIMEOUT_IN_MIN = 30
    }

    @GetMapping("")
    fun redirectToOverview(): String {
        if (ldapService.getPerson().isAdmin) {
            return "redirect:/metadata-validator/overview/admin"
        }
        return "redirect:/metadata-validator/overview/user"
    }

    @GetMapping("/details")
    fun redirectToSimpleMetadataValidator(@RequestParam("uuid") uuid: String): String {
        return "redirect:$SIMPLE_TABLE_PAGE_USER?uuid=$uuid"
    }

    @GetMapping("/admin")
    fun redirectToAdminSimpleMetadataValidator(@RequestParam("identifier") identifier: String): String {
        return "redirect:$SIMPLE_TABLE_PAGE_ADMIN?identifier=$identifier"
    }
}
