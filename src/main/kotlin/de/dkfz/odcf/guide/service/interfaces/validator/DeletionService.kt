package de.dkfz.odcf.guide.service.interfaces.validator

import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import org.springframework.dao.DataIntegrityViolationException

interface DeletionService {

    /**
     * Deletes a given submission and all its samples.
     *
     * @param submission Submission to delete
     * @return `true` if the submission no longer exists
     */
    fun deleteSubmission(submission: Submission, sendMail: Boolean = true): Boolean

    /**
     * Deletes all samples from a given submission.
     *
     * @param submission Submission which samples to delete
     * @return `true` if the samples no longer exist
     */
    fun deleteSamples(submission: Submission): Boolean

    /**
     * Deletes all samples from a given list of samples.
     *
     * @param samples List of samples to delete
     * @return `true` if the samples no longer exist
     */
    fun deleteSamples(samples: List<Sample>): Boolean

    /**
     * Deletes all files from a given sample as well as the sample itself.
     *
     * @param sample Sample to delete
     * @return `true` if the sample no longer exists
     */
    fun deleteSample(sample: Sample): Boolean

    /**
     * Deletes a given seqType, sets the seqType to `null` in all the samples that use the deleted seqTypes
     * and rejects all the requestedValues tied to that deleted seqType
     *
     * @param seqType SeqType to delete
     * @return `true` if the seqType no longer exists
     */
    @Throws(DataIntegrityViolationException::class)
    fun deleteSeqType(seqType: SeqType): Boolean

    /**
     * Deletes all references to a given submission from the requested values.
     *
     * When the origin submission of a requested value is the submission to be deleted:
     * - If it is used in other submissions too, assign the requested value to the dummy submission 'o0000000'.
     * - If it is only used in the submission to be deleted, the requested value is deleted along with the submission
     *
     * @param submission submission to be deleted
     * @return `true` if all the references to the submission have been deleted
     */
    fun deleteSubmissionFromRequestedValues(submission: Submission): Boolean
}
