package de.dkfz.odcf.guide.helperObjects

import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.service.interfaces.security.LdapService

class ControllerHelper(
    private val ldapService: LdapService
) {

    fun getRedirectPage(submission: Submission): String {
        return if (ldapService.getPerson().isAdmin) {
            "redirect:" + (if (submission.isExtended) MetaValController.EXTENDED_TABLE_PAGE_ADMIN else MetaValController.SIMPLE_TABLE_PAGE_ADMIN) + "?identifier=" + submission.identifier
        } else {
            "redirect:" + (if (submission.isExtended) MetaValController.EXTENDED_TABLE_PAGE_USER else MetaValController.SIMPLE_TABLE_PAGE_USER) + "?uuid=" + submission.uuid
        }
    }
}
