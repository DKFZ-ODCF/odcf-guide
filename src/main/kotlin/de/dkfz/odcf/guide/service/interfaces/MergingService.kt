package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideMergerException
import de.dkfz.odcf.guide.exceptions.JsonExtractorException
import de.dkfz.odcf.guide.exceptions.SubmissionNotFinishedException

interface MergingService {

    /**
     * If the property for merging (`application.mergingService.doMerging`) is set to `true` in the YML file for the run configurations,
     * triggers the merging or optionally only the JSON extraction for a submission and sends a mail to the ticket system.
     *
     * Also sends mails if some Exceptions are caught in the process.
     *
     * @param submission Submission that should be merged.
     * @param onlyRunJsonExtractor `true` if only the JSON extraction should be run.
     */
    @Throws(
        JsonExtractorException::class,
        GuideMergerException::class,
        SubmissionNotFinishedException::class
    )
    fun doMerging(submission: Submission, onlyRunJsonExtractor: Boolean = false)

    /**
     * If the property for merging (`application.mergingService.doMerging`) is set to `true` in the YML file for the run configurations,
     * runs the JSON extractor script to retrieve the technical data of a given submission from midterm.
     *
     * @param submission Submission for which the technical data is retrieved
     *
     * @throws JsonExtractorException If the file was not found on midterm,
     *     if the path to the json extractor script can't be
     *     found or if something goes wrong during the json extraction.
     *
     * @return JSON content of the technical data file extracted from midterm.
     */
    @Throws(JsonExtractorException::class, IllegalArgumentException::class)
    fun runJsonExtractorScript(submission: Submission): String
}
