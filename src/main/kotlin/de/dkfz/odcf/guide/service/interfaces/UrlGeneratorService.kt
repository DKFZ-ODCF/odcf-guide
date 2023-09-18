package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.submissionData.Submission

interface UrlGeneratorService {

    /**
     * Constructs the URL to the simple details page for a submission.
     *
     * @param submission Submission for which the URL is needed
     * @return String - Base-URL to the user page together with the UUID for the submission.
     */
    fun getURL(submission: Submission): String

    /**
     * Constructs the URL to the simple details page of a given submission for an admin.
     *
     * @param submission Submission for which the URL is needed
     * @return String - Base-URL to the admin page together with the submission identifier.
     */
    fun getAdminURL(submission: Submission): String
}
