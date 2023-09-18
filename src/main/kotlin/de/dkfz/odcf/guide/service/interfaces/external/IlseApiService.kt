package de.dkfz.odcf.guide.service.interfaces.external

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helperObjects.importObjects.ExternalIlseSubmissionImportObject

interface IlseApiService {

    /**
     * Retrieves information about multiple samples from ILSe API and converts it to a list of sample import objects.
     * If the runtime option `jsonToDB` is `true`, also saves the original import source data in the DB.
     *
     * @param ilseId The ILSe number of a submission as an Integer
     *
     * @throws ExternalApiReadException If the content from the ILSe API cannot be read
     * @throws JsonProcessingException If the JSON file containing the information about the samples
     *     retrieved from the ILSe API cannot be processed
     * @throws JsonMappingException If the content from JSON file retrieved from the ILSe API cannot be
     *     mapped correctly to the ExternalIlseSubmissionImportObject
     *
     * @return an `ExternalIlseSubmissionImportObject` which is a `List<IlseSampleImportObject>`
     *     containing all the information about the samples belonging to an ILSe submission
     */
    @Throws(
        JsonProcessingException::class,
        JsonMappingException::class,
        ExternalApiReadException::class
    )
    fun getSubmissionImportObjectFromApi(ilseId: Int, replaceExistingEntry: Boolean = false): ExternalIlseSubmissionImportObject
}
