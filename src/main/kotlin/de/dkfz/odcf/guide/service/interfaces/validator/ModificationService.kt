package de.dkfz.odcf.guide.service.interfaces.validator

import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission

interface ModificationService {

    /**
     * Checks whether the project prefixes map contains the prefix for the project of a given sample,
     * and if not adds the project prefix to the map
     *
     * @param sample Sample object containing a project prefix to be checked
     * @param projectPrefixes Map of the prefixes of all projects
     */
    fun updateProjectPrefixesMap(sample: Sample, projectPrefixes: MutableMap<String, String?>)

    /**
     * Removes the project prefixes from multiple PIDs of a given submission.
     *
     * @param submission Submission object containing the samples for which the project prefixes should be removed
     * @param projectPrefixMapping Map of all existing project prefixes
     */
    fun removeProjectPrefixFromPids(submission: Submission, projectPrefixMapping: Map<String, String?>)

    /**
     * Removes a project prefix from a PID of a given sample.
     *
     * @param sample Sample object for which the project prefix should be removed
     * @param projectPrefixMapping Map of all existing project prefixes
     */
    fun removeProjectPrefixFromPid(sample: Sample, projectPrefixMapping: MutableMap<String, String?>)
}
