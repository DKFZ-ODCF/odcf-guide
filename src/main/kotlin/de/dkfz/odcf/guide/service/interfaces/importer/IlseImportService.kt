package de.dkfz.odcf.guide.service.interfaces.importer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helperObjects.importObjects.ExternalIlseSubmissionImportObject
import de.dkfz.odcf.guide.helperObjects.importObjects.IlseSampleImportObject
import org.springframework.dao.DuplicateKeyException

interface IlseImportService {

    /**
     * Imports data from ILSe and triggers the saving of the submission and its samples.
     *
     * @param ilse The ILSe number of a submission as an Integer, not yet formatted into a submission identifier
     * @param ticketNumber OTRS ticket number
     *
     * @throws DuplicateKeyException If a submission with the same identifier already exists.
     * @throws ExternalApiReadException If the content from the ILSe API cannot be read
     * @throws JsonProcessingException If the JSON file containing the information about the samples
     *     retrieved from the ILSe API cannot be processed
     * @throws JsonMappingException If the content from JSON file retrieved from the ILSe API cannot be
     *     mapped correctly to a ExternalIlseSubmissionImportObject
     *
     * @return Newly imported submission object
     */
    @Throws(
        DuplicateKeyException::class,
        ExternalApiReadException::class,
        JsonProcessingException::class,
        JsonMappingException::class
    )
    fun import(ilse: Int, ticketNumber: String): Submission?

    /**
     * If the submission is resettable, deletes all saved data for a submission and re-imports it from the ILSe API.
     *
     * @param submission Submission object to be re-imported from the ILSe API
     */
    @Throws(IllegalStateException::class)
    fun reimport(submission: Submission)

    /**
     * Creates a new ApiSubmission and calls [saveSamples] to save the samples to the submission.
     * If the submission is able to be auto closed, triggers [checkSimilarPidsAndSendMail].
     *
     * @param identifier Submission identifier of the ApiSubmission object to be created
     * @param importObject List of multiple sample import objects containing all information about the imported samples to be saved
     * @param ticketNumber OTRS ticket number associated with the submission
     * @return Newly saved submission object
     */
    fun saveSubmission(identifier: String, importObject: ExternalIlseSubmissionImportObject, ticketNumber: String): Submission

    /**
     * Detects attributes in the sample import object that are not represented in sample objects
     * and therefore unknown to the GUIDE (unknown fields) and sends a mail to the ticket system
     * containing information about the unknown fields.
     *
     * @param values List of multiple sample import objects
     * @param ilseId The ILSe number of a submission as an Integer
     */
    fun detectUnknownFields(values: ExternalIlseSubmissionImportObject, ilseId: Int)

    fun detectUnknownValues(sampleImportObject: IlseSampleImportObject, unknown: MutableMap<String, MutableSet<String>>)

    fun summariesAfterImport(submission: ApiSubmission)
}
