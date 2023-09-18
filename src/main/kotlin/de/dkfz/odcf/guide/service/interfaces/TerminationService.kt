package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.submissionData.Submission

/**
 * Termination service - Service to terminate submissions
 */
interface TerminationService {

    /**
     * Terminate actions for submissions
     *
     *  - **Internal submission** - terminate it by changing the Status to `TERMINATED` and sending a mail to the submitter and ticket system.
     *
     * - **External submission** - terminate it by changing the Status to `REMOVED_BY_ADMIN` and sending a mail to the submitter
     * and ticket system with the content of the submission to be terminated attached as a TSV File
     *
     * @param submission Submission to be terminated
     */
    fun terminateSubmissionAfterReminder(submission: Submission)

    /**
     * Sets the submission state to TERMINATED and sends a mail to the ticket system.
     * No username associated.
     *
     * @param submission The submission to change the state for.
     * @param subjectKey The message key for the matching subject
     */
    fun terminateSubmission(submission: Submission, subjectKey: String)

    /**
     * Sets the submission's `startTerminationPeriod` date to now and sends a mail to the ticket system.
     * No username or change in submission status associated.
     *
     * @param submission The submission reset the termination period for.
     */
    fun resetSubmissionTerminationPeriod(submission: Submission)
}
