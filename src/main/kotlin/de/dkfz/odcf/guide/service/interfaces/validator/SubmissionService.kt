package de.dkfz.odcf.guide.service.interfaces.validator

import de.dkfz.odcf.guide.entity.submissionData.Submission
import java.util.*

interface SubmissionService {

    /**
     * Changes the submission state and creates a log entry.
     *
     * @param submission The submission to change the state for.
     * @param status The new status for the submission.
     * @param username The responsible username for this change (uses "automatic" if not provided).
     * @param logComment Optional comment to save with the log entry.
     * @param stateComment Optional comment for the ON_HOLD state.
     */
    fun changeSubmissionState(
        submission: Submission,
        status: Submission.Status,
        username: String? = null,
        logComment: String? = null,
        stateComment: String? = null,
    )

    /**
     * Sets the submission state to FINISHED_EXTERNALLY.
     * No username associated.
     *
     * @param submission The submission to change the state for.
     */
    fun finishSubmissionExternally(submission: Submission)

    /**
     * Sets the [Submission.externalDataAvailableForMerging] property to the provided value.
     * Also sets the availability date either to the specified date or if not provided to now.
     *
     * @param submission The submission to change the availability for.
     * @param available Boolean for the desired availability
     * @param date Availability date
     */
    fun setExternalDataAvailableForMerging(submission: Submission, available: Boolean, date: Date?)

    /**
     * Post proceed with submission
     *
     * @param submission
     */
    fun postProceedWithSubmission(submission: Submission)
}
