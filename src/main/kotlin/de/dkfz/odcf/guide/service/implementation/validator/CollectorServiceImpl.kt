package de.dkfz.odcf.guide.service.implementation.validator

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.Submission.Status.*
import de.dkfz.odcf.guide.helperObjects.mapDistinctAndNotNullOrBlank
import de.dkfz.odcf.guide.helperObjects.mapParallel
import de.dkfz.odcf.guide.helperObjects.toBool
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CollectorServiceImpl(
    private val submissionRepository: SubmissionRepository,
    private val apiSubmissionRepository: ApiSubmissionRepository,
    private val uploadSubmissionRepository: UploadSubmissionRepository,
    private val sampleRepository: SampleRepository,
    private val otpCachedProjectRepository: OtpCachedProjectRepository,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val ldapService: LdapService,
    private val runtimeOptionsRepository: RuntimeOptionsRepository
) : CollectorService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getFormattedIdentifier(identifier: String): String {
        return if (identifier.startsWith("i")) {
            "S#" + identifier.substring(1).toInt()
        } else identifier
    }

    override fun getAllSubmissionsPerUser(): Set<Submission> {
        val person = ldapService.getPerson()
        val submissions: MutableList<Submission> = submissionRepository.findAllBySubmitter(person).toMutableList()
        externalMetadataSourceService.getSetOfValues("projectsByPerson", mapOf("username" to person.username)).forEach {
            submissions.addAll(submissionRepository.findAllByOriginProjectsContains(it))
        }
        return submissions.toSortedSet(compareBy { it.identifier })
    }

    /**
     * @return All the uploaded submissions per user sorted by their identifier as well as the submissions
     * of the projects the user is involved in.
     */
    fun getUploadedSubmissionsPerUser(): Set<Submission> {
        val person = ldapService.getPerson()
        val submissions: MutableList<Submission> = uploadSubmissionRepository.findAllBySubmitter(person).toMutableList()
        externalMetadataSourceService.getSetOfValues("projectsByPerson", mapOf("username" to person.username)).forEach {
            submissions.addAll(submissionRepository.findAllByOriginProjectsContains(it))
        }
        return submissions.toSortedSet(compareBy { it.identifier })
    }

    override fun getUploadedSubmissionsPerStatusTypeForUser(): Map<String, List<Submission>> {
        val submissionPerStatusType = getSubmissionPerStatusType(getUploadedSubmissionsPerUser())
        submissionPerStatusType.remove("Terminated")
        submissionPerStatusType.remove("AutoClosed")
        submissionPerStatusType.remove("FinishedExternally")
        return submissionPerStatusType
    }

    override fun getApiSubmissionsPerStatusTypeForAdmin(): LinkedHashMap<String, ArrayList<Submission>> {
        return getSubmissionPerStatusType(getApiSubmissionsForOverview())
    }

    override fun getUploadedSubmissionsPerStatusTypeForAdmin(): Map<String, List<Submission>> {
        return getSubmissionPerStatusType(getUploadedSubmissionsForOverview())
    }

    override fun foundMergeableSamples(submission: Submission): Boolean {
        if (submission.ownTransfer) return false
        val samples = sampleRepository.findBySubmissionAndProceedNot(submission, Sample.Proceed.NO)
        return samples.map { it.getMergingFieldData.toString() }.toSet().size < samples.size || getSampleListEnrichedByMergingSamples(samples.toSet()).size > samples.size
    }

    override fun getImportableProjects(submission: Submission): Set<String> {
        return getProjectsForSubmissionAndUser(submission, submission.submitter)
    }

    override fun getProjectsForSubmissionAndUser(submission: Submission, user: Person): Set<String> {
        val projects = externalMetadataSourceService.getSetOfValues(
            "projects-by-person-or-organizational-unit",
            mapOf("username" to user.username, "organizationalUnit" to user.organizationalUnit)
        ).map {
            it.removeSuffix("(f)").replace("(t)", " (closed)")
        }.toMutableSet()
        projects.addAll(submission.originProjectsSet)
        sampleRepository.findAllBySubmission(submission).forEach {
            if (it.project.isNotEmpty()) {
                projects.add(it.project)
            }
        }
        val sortedProjects = emptySet<String>().toMutableSet()
        sortedProjects.addAll(projects)
        sortedProjects.sorted()
        return sortedProjects
    }

    override fun getProjectsForAdmins(): Set<String> {
        val sortedProjects = emptySet<String>().toMutableSet()
        sortedProjects.addAll(
            externalMetadataSourceService.getSetOfValues("projectsWithClosed").map {
                it.removeSuffix("(f)").replace("(t)", " (closed)")
            }
        )
        sortedProjects.sorted()
        return sortedProjects
    }

    override fun getUrlsByPerson(person: Person): Map<String, String> {
        val urls: MutableMap<String, String> = HashMap()
        for (s in submissionRepository.findAllBySubmitter(person)) {
            urls[getFormattedIdentifier(s.identifier)] = MetaValController.SIMPLE_TABLE_PAGE_USER + "?uuid=" + s.uuid
        }
        return urls
    }

    override fun getProjectPrefixesForSamplesInSubmission(submission: Submission, candidateProjects: Set<String>?): Map<String, String?> {
        val projectPrefixes = sampleRepository.findAllBySubmission(submission).mapDistinctAndNotNullOrBlank { it.project }.associateWith {
            externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to it))
        }.toMutableMap()

        candidateProjects?.forEach {
            val project = it.removeSuffix("(closed)").trim()
            if (!projectPrefixes.containsKey(project)) {
                projectPrefixes[project] = externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to project))
            }
        }
        return projectPrefixes.toMap()
    }

    override fun getSampleListEnrichedByMergingSamples(samples: Set<Sample>, findMergingSamples: Boolean): List<Sample> {
        val filteredSamples = samples.filter { !it.isStopped }
        if (!findMergingSamples) return filteredSamples.sortedBy { if (it.id > 0) it.id else Int.MAX_VALUE }
        val mergingSamples = runBlocking(Dispatchers.Default) {
            samples.mapParallel { sample ->
                val otpMergingSamples = externalMetadataSourceService.getSetOfMapOfValues("mergingCandidatesData", sample.getMergingFieldData).map {
                    Sample(if (it["file_withdrawn"]!!.toBool()) Sample.WITHDRAWN_SAMPLE_FROM_OTP else Sample.SAMPLE_FROM_OTP, sample)
                }
                val guideMergingSamples = getGuideMergingSamples(sample).map {
                    Sample("${Sample.SAMPLE_FROM_ANOTHER_SUBMISSION} ${it.submission.identifier}", sample)
                }
                otpMergingSamples + guideMergingSamples
            }.flatten()
        }
        return filteredSamples.plus(mergingSamples).sortedBy { if (it.id > 0) it.id else Int.MAX_VALUE }
    }

    private fun getGuideMergingSamples(sample: Sample): List<Sample> {
        return sampleRepository.findAllByProjectAndPidAndSampleTypeAndSeqTypeAndLibraryLayoutAndAntibodyTargetAndSubmissionNotAndSubmission_ImportedExternalIsFalse(
            sample.project, sample.pid, sample.sampleType, sample.seqType, sample.libraryLayout, sample.antibodyTarget, sample.submission
        ).filter { it.singleCellWellLabel == sample.singleCellWellLabel && it.submission.isDiscontinued.not() }
    }

    override fun getSampleListEnrichedByMergingSamplesGrouped(submission: Submission): Map<String, List<Sample>> {
        val samples = sampleRepository.findAllBySubmission(submission).toSet()
        val groupedSamples = if (submission.ownTransfer) {
            getSampleListEnrichedByMergingSamples(samples, false).groupBy { it.abstractSampleId }
        } else {
            getSampleListEnrichedByMergingSamples(samples).groupBy { it.getMergingFieldData.toString() }
        }
        return groupedSamples.mapValues { it.value.distinctBy { "${it.id}${it.name}" } }
    }

    override fun getPathsWithSampleList(samples: Map<String, List<Sample>>, submission: Submission): Map<String, List<Sample>> {
        val template = runtimeOptionsRepository.findByName("projectPathTemplateNonOtp".takeIf { submission.ownTransfer } ?: "projectPathTemplate")!!.value
        val samplesData = runBlocking(Dispatchers.Default) {
            samples.values.mapParallel { samples ->
                val sample = samples.first()
                val project = withContext(Dispatchers.IO) {
                    otpCachedProjectRepository.findByName(sample.project)
                }
                val seqTypeDirName = externalMetadataSourceService.getSingleValue("SeqTypeDirName", mapOf("seqType" to sample.seqType?.name.orEmpty()))
                Triple(samples, project, seqTypeDirName)
            }
        }
        return samplesData.associate { (samples, project, seqTypeDirName) ->
            val sample = samples.first()
            val key = template.replace("<PROJECT>", project?.pathProjectFolder.orEmpty())
                .replace("<SEQ_TYPE_EXT>", seqTypeDirName.takeIf { it.isNotBlank() } ?: "<SEQ_TYPE>")
                .replace("<SEQ_TYPE_INT>", sample.seqType?.name?.lowercase() ?: "<SEQ_TYPE>")
                .replace("<PID>", sample.pid)
                .replace("<SAMPLE_TYPE_EXT>", listOf(sample.sampleTypeReflectingXenograft.lowercase(), sample.antibodyTarget).filter { it.isNotEmpty() }.joinToString("-"))
                .replace("<SAMPLE_TYPE_INT>", sample.sampleType.lowercase())
                .replace("<WELL_LABEL>", sample.singleCellWellLabel.lowercase())
                .replace("<LIBRARY_LAYOUT>", sample.libraryLayout!!.name.lowercase())
                .replace("<RUN_NAME>", sample.technicalSample?.runId ?: "<RUN_NAME>")
                .replace("<ILSE_ID>", submission.identifier.filter { it.isDigit() })
                .replace("<ASID>", sample.abstractSampleId)
                .replace("//", "/")
            key to samples
        }
    }

    /**
     * @return All the ApiSubmission objects for the overview sorted by their identifier.
     * Excludes all submissions with the status REMOVED_BY_ADMIN.
     */
    private fun getApiSubmissionsForOverview(): Set<Submission> {
        return apiSubmissionRepository.findAll().filter { it.status != REMOVED_BY_ADMIN }.toSortedSet(compareBy { it.identifier })
    }

    /**
     * @return All the submissions uploaded from TSV files for the overview sorted by their identifier.
     * Excludes all submissions with the status REMOVED_BY_ADMIN.
     */
    private fun getUploadedSubmissionsForOverview(): Set<Submission> {
        return uploadSubmissionRepository.findAll().filter { it.status != REMOVED_BY_ADMIN }.toSortedSet(compareBy { it.identifier })
    }

    /**
     * Returns lists of all the submission sorted to their status type.
     *
     * @param submissions All the submissions to be sorted
     * @return LinkedHashMap of the status name mapped to a list of all the submissions currently under that status.
     */
    private fun getSubmissionPerStatusType(submissions: Set<Submission>): LinkedHashMap<String, ArrayList<Submission>> {
        val submissionPerStatusType: LinkedHashMap<String, ArrayList<Submission>> = object : LinkedHashMap<String, ArrayList<Submission>>() {
            init {
                put("Active", ArrayList())
                put("Closed", ArrayList())
                put("Terminated", ArrayList())
                put("AutoClosed", ArrayList())
                put("FinishedExternally", ArrayList())
                put("Exported", ArrayList())
            }
        }
        submissions.toSortedSet(compareBy { it.identifier }).forEach {
            when (it.status) {
                CLOSED -> submissionPerStatusType["Closed"]!!.add(it)
                AUTO_CLOSED -> submissionPerStatusType["AutoClosed"]!!.add(it)
                FINISHED_EXTERNALLY -> submissionPerStatusType["FinishedExternally"]!!.add(it)
                EXPORTED -> submissionPerStatusType["Exported"]!!.add(it)
                TERMINATED -> submissionPerStatusType["Terminated"]!!.add(it)
                else -> submissionPerStatusType["Active"]!!.add(it)
            }
        }
        return submissionPerStatusType
    }
}
