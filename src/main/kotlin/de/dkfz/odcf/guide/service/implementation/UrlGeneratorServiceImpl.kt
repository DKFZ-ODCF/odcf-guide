package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_TABLE_PAGE_ADMIN
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_TABLE_PAGE_USER
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.service.interfaces.UrlGeneratorService
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class UrlGeneratorServiceImpl(
    private val env: Environment
) : UrlGeneratorService {

    override fun getURL(submission: Submission): String {
        return env.getRequiredProperty("application.serverUrl") + SIMPLE_TABLE_PAGE_USER + "?uuid=" + submission.uuid
    }

    override fun getAdminURL(submission: Submission): String {
        return env.getRequiredProperty("application.serverUrl") + SIMPLE_TABLE_PAGE_ADMIN + "?identifier=" + submission.identifier
    }
}
