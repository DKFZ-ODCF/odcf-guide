package de.dkfz.odcf.guide.service.implementation.validator

import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.validator.ModificationService
import org.springframework.stereotype.Service

@Service
class ModificationServiceImpl(
    private val sampleRepository: SampleRepository,
    private val externalMetadataSourceService: ExternalMetadataSourceService
) : ModificationService {

    override fun updateProjectPrefixesMap(sample: Sample, projectPrefixes: MutableMap<String, String?>) {
        if (projectPrefixes[sample.project] == null) {
            externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to sample.project)).let { projectPrefixes[sample.project] = it }
        }
    }

    override fun removeProjectPrefixFromPids(submission: Submission, projectPrefixMapping: Map<String, String?>) {
        val mutableProjectPrefixMapping = projectPrefixMapping.toMutableMap()
        sampleRepository.findAllBySubmission(submission).forEach {
            removeProjectPrefixFromPid(it, mutableProjectPrefixMapping)
        }
    }

    override fun removeProjectPrefixFromPid(sample: Sample, projectPrefixMapping: MutableMap<String, String?>) {
        var projectPrefix: String = getProjectPrefix(sample, projectPrefixMapping)
        if (projectPrefix.isBlank()) {
            return
        }
        if (projectPrefix.endsWith("-") || projectPrefix.endsWith("_")) {
            projectPrefix = projectPrefix.substring(0, projectPrefix.length - 1)
        }
        sample.pid = sample.pid.replace("$projectPrefix[_-]?".toRegex(), "")
    }

    /**
     * Returns the project prefix for a given sample and updates the project prefixes map if needed.
     *
     * @param sample Sample object for which the project prefix is needed
     * @param projectPrefixMapping Map of all existing project prefixes
     * @return The project prefix, returns an empty string if the project prefix was not found
     */
    private fun getProjectPrefix(sample: Sample, projectPrefixMapping: MutableMap<String, String?>): String { // can this be included in removeProjectPrefixFromPid?
        updateProjectPrefixesMap(sample, projectPrefixMapping)
        return projectPrefixMapping[sample.project] ?: ""
    }
}
