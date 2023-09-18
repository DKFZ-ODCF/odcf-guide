package de.dkfz.odcf.guide.service.interfaces.validator

import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import java.util.*

interface CollectorService {

    /**
     * Formats a given submission identifier.
     * If the given identifier starts with an "i", replaces the "i" with "S#".
     *
     * @param identifier Identifier to format
     * @return The formatted identifier
     */
    fun getFormattedIdentifier(identifier: String): String

    /**
     * Returns all submissions submitted by the user or with a project from the user.
     *
     * @return A set of submission objects
     */
    fun getAllSubmissionsPerUser(): Set<Submission>

    /**
     * Returns all the uploaded submissions sorted by status type for a user.
     * Excludes all submissions with the status Terminated, AutoClosed and FinishedExternally.
     *
     * @return The name of the status mapped to a list of all submissions under that status.
     */
    fun getUploadedSubmissionsPerStatusTypeForUser(): Map<String, List<Submission>>

    /**
     * Returns all the API submissions sorted by status type for an admin.
     *
     * @return The name of the status mapped to a list of all API submissions under that status.
     */
    fun getApiSubmissionsPerStatusTypeForAdmin(): LinkedHashMap<String, ArrayList<Submission>>

    /**
     * Returns all the uploaded submissions sorted by status type for an admin.
     *
     * @return The name of the status mapped to a list of all uploaded submissions under that status.
     */
    fun getUploadedSubmissionsPerStatusTypeForAdmin(): Map<String, List<Submission>>

    /**
     * @param submission Submission object containing samples that might be mergeable
     * @return `true` if samples were found that could be merged with other already existing samples.
     */
    fun foundMergeableSamples(submission: Submission): Boolean

    /**
     * @param submission The submission object that contains projects that are available for the current user
     * @return All the project names that are available for a user depending on whether the current user is an admin or not.
     */
    fun getImportableProjects(submission: Submission): Set<String>

    /**
     * Returns all projects sorted by name for admins.
     * Adds "(closed)" for projects that are closed.
     *
     * @return List of the project names
     */
    fun getProjectsForAdmins(): Set<String>

    /**
     * Returns all projects sorted by name for the given submission and the current user.
     * Adds "(closed)" for projects that are closed.
     *
     * @param submission The submission object that contains projects that are available for the current user
     * @return List of the project names
     */
    fun getProjectsForSubmissionAndUser(submission: Submission, user: Person): Set<String>

    /**
     * Returns all URLs leading to API submissions in which the person is a submitter.
     *
     * @param person Person object for the submitter
     * @return The formatted identifier of a submission mapped to the URL leading to its details page
     */
    fun getUrlsByPerson(person: Person): Map<String, String>

    /**
     * Returns a map of the project prefixes for the samples in a given submission
     * as well as the prefixes for the candidateProjects.
     *
     * @param submission Submission object containing the samples for which project prefixes are being searched
     * @param candidateProjects Additional list of projects
     * @return A map connecting a project to its prefix
     */
    fun getProjectPrefixesForSamplesInSubmission(submission: Submission, candidateProjects: Set<String>?): Map<String, String?>

    /**
     * Returns a list of samples enriched by their merging samples as sample objects.
     * Merging samples are samples in OTP that have the same PID, sampleType and seqType (and more).
     * Excludes all stopped samples.
     *
     * @param samples List of sample objects for which merging samples should be found
     * @param findMergingSamples If false, no merging samples are searched
     * @return List of sample objects enriched by their merge candidates
     */
    fun getSampleListEnrichedByMergingSamples(samples: Set<Sample>, findMergingSamples: Boolean = true): List<Sample>

    /**
     * Returns a map of samples enriched by their merging samples grouped by their merged name.
     * Merging samples are samples in OTP that have the same PID, sampleType and seqType (and more).
     * Excludes all stopped samples. If a submission has a cluster job template, no merge candidates will be searched
     * and they are grouped by the abstract sample id.
     *
     * @param submission Submission containing the samples for which merging samples should be found
     * @return Map of samples enriched by their merge candidates and grouped by merged name
     */
    fun getSampleListEnrichedByMergingSamplesGrouped(submission: Submission): Map<String, List<Sample>>

    /**
     * Returns the project paths for a list of samples.
     * These paths are where the information about the samples will be saved as files after the
     * submission the samples belong to is finished in the GUIDE.
     *
     * @param samples samples for which to get the project paths
     * @param submission submission the samples belong to
     * @return List of samples mapped to their project paths
     */
    fun getPathsWithSampleList(samples: Map<String, List<Sample>>, submission: Submission): Map<String, List<Sample>>
}
