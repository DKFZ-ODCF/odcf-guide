package de.dkfz.odcf.guide.service.deprecated

import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helperObjects.importObjects.SubmissionImportObject
import org.springframework.dao.DuplicateKeyException

@Deprecated("This importer not used anymore")
interface JsonImportService {

    @Throws(DuplicateKeyException::class)
    fun import(json: SubmissionImportObject, identifier: String): Submission

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun reimport(submission: Submission)

    fun saveSubmission(identifier: String, json: SubmissionImportObject): Submission
}
