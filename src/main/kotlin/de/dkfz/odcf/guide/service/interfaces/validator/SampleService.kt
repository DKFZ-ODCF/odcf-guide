package de.dkfz.odcf.guide.service.interfaces.validator

import de.dkfz.odcf.guide.dtoObjects.FileGuiDto
import de.dkfz.odcf.guide.dtoObjects.SampleGuiDto
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.validation.ValidationLevel
import de.dkfz.odcf.guide.helperObjects.SampleForm

interface SampleService {

    /**
     * Validates samples as provided by the [form]. Keys of the returned Map consist of the Sample Identifier
     * for which validation errors have been identified and the value consists of an inner map
     * with names of the affected fields as key and `true` as value. The inner map data structure
     * has been chosen in order to have direct access to the respective fields in thymeleaf.
     *
     * @param form [SampleForm] as provided by the user gui
     * @return Map with validation errors
     */
    fun validateSamples(form: SampleForm, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>>

    /**
     * Validates files as provided by the [form]. Keys of the returned Map consist of the Sample Identifier
     * for which validation errors have been identified and the value consists of an inner map
     * with names of the affected fields as key and `true` as value. The inner map data structure
     * has been chosen in order to have direct access to the respective fields in thymeleaf.
     *
     * @param form [FileForm] as provided by the user gui
     * @return Map with validation errors
     */
    fun validateFiles(form: SampleForm, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>>

    /**
     * Validates samples. Keys of the returned Map consist of the Sample Identifier
     * for which validation errors have been identified and the value consists of an inner map
     * with names of the affected fields as key and `true` as value. The inner map data structure
     * has been chosen in order to have direct access to the respective fields in thymeleaf.
     *
     * @param samples List of [SampleGuiDto] objects to be validated
     * @return Map with validation errors
     */
    fun validateSamples(samples: List<SampleGuiDto>, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>>

    /**
     * Validates files. Keys of the returned Map consist of the Sample Identifier
     * for which validation errors have been identified and the value consists of an inner map
     * with names of the affected fields as key and `true` as value. The inner map data structure
     * has been chosen in order to have direct access to the respective fields in thymeleaf.
     *
     * @param files List of [FileGuiDto] objects to be validated
     * @return Map with validation errors
     */
    fun validateFiles(files: List<FileGuiDto>, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>>

    /**
     * Validates a [sample]. Keys of the returned Map are the names of invalid fields and the values are always `true`.
     * The map data structure has been chosen in order to have direct access to the respective fields in thymeleaf.
     *
     * @param sample [SampleGuiDto] object to be validated
     * @return Map with validation errors
     */
    fun validateSample(sample: SampleGuiDto, validationLevel: ValidationLevel): Map<String, Boolean>

    /**
     * Validates a [file]. Keys of the returned Map are the names of invalid fields and the values are always `true`.
     * The map data structure has been chosen in order to have direct access to the respective fields in thymeleaf.
     *
     * @param file [FileGuiDto] object to be validated
     * @return Map with validation errors
     */
    fun validateFile(file: FileGuiDto, validationLevel: ValidationLevel): Map<String, Boolean>

    /**
     * Triggers the updating of all the samples belonging to a submission.
     * Weeds out empty sample objects out of the SampleForm and locks the submission during the process of updating.
     *
     * @param submission Submission to be updated
     * @param form Form containing the new information about the samples
     */
    fun updateSamples(submission: Submission, form: SampleForm)

    /**
     * Updates all samples of a submission and saves the new sample information.
     *
     * @param submission Submission containing the samples to be updated
     * @param samples List of sample objects containing new information that will replace the submission's current samples
     */
    fun updateSamples(submission: Submission, samples: List<Sample>)

    /**
     * Updates all samples and files of a submission and saves the new information.
     * Also triggers [deletedFilesAndSamples] to clean up the submission.
     *
     * @param submission Extended submission object containing the samples to be updated
     * @param samples List of sample objects containing new information that will replace the submission's current samples
     */
    fun updateFilesAndSamples(submission: Submission, samples: List<Sample>)

    /**
     * Finds old files connected to the samples of the submission that are no longer represented and deletes them.
     * Also finds all old samples of the submission that are no longer represented and deletes them.
     *
     * @param submission Submission with new information
     * @param samples Samples with new information
     */
    fun deletedFilesAndSamples(submission: Submission, samples: List<Sample>)

    /**
     * Converts SampleGuiDto object to Sample entity. If the `id` of the
     * SampleGuiDto object is `0`, a new Sample is created, otherwise the
     * Sample object with the corresponding `id` is used.
     *
     * @param sampleGuiDto Object to be converted
     * @return Sample entity
     */
    fun convertToEntity(sampleGuiDto: SampleGuiDto): Sample
}
