package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.submissionData.Submission

interface RequestedValueService {

    fun acceptValue(requestedValue: RequestedValue)

    fun acceptAndCorrectValue(requestedValue: RequestedValue, newValue: String)

    fun rejectValue(requestedValue: RequestedValue)

    fun getRequestedValuesForUserAndFieldNameAndSubmission(fieldName: String, submission: Submission): Set<String>

    fun getRequestedSeqTypesForUserAndSubmission(submission: Submission): Set<SeqType>

    fun saveRequestedValue(fieldName: String, className: String, newValue: String, submission: Submission)

    fun saveSeqTypeRequestedValue(newSeqType: SeqType, submission: Submission)

    fun sendNewValueRequestMail(submissionIdentifier: String, newValue: String, fieldName: String)

    fun getSubmissionUsesRequestedValues(submission: Submission): Map<String, Set<String>>
}
