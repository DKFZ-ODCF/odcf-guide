package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.MissingPropertyException

interface PseudonymService {

    /**
     * Triggers the check for similar PIDs and the sending of the result mail.
     *
     * @param submission Submission containing the PIDs to be checked.
     */
    fun checkSimilarPidsAndSendMail(submission: Submission)

    /**
     * Calls [getSimilarPids] to get the similar PIDs for all the samples of a submission.
     *
     * @param submission Submission containing the PIDs to be checked.
     * @return A set of relevant information about samples in different submissions which have a similar PID
     */
    fun getSimilarPids(submission: Submission): Set<Map<String, String>>

    /**
     * Removes the prefix of the PID of a sample and
     * gets the information about samples containing similar PIDs from the GUIDE and OTP.
     *
     * @param sample Sample for which the PID should be checked
     * @return A set of relevant information about samples in different samples which have a similar PID
     */
    fun getSimilarPids(sample: Sample): Set<Map<String, String>>

    /**
     * Sends a mail to the ticket system containing the information about samples containing
     * similar PIDs as in the given submission.
     *
     * @param matchResults Results of the checking for similar PIDs.
     * @param submission Submission containing the PIDs that have been checked.
     */
    @Throws(MissingPropertyException::class)
    fun sendMail(matchResults: Set<Map<String, String>>, submission: Submission)
}
