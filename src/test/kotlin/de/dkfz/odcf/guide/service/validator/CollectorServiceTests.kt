package de.dkfz.odcf.guide.service.validator

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.validator.CollectorServiceImpl
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class CollectorServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var collectorServiceMock: CollectorServiceImpl

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var apiSubmissionRepository: ApiSubmissionRepository

    @Mock
    lateinit var uploadSubmissionRepository: UploadSubmissionRepository

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var seqTypeMappingService: SeqTypeMappingService

    @Mock
    lateinit var projectRepository: ProjectRepository

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var ldapService: LdapService

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Test
    fun `check submission state per sample hashmap for admin`() {
        val submissionImported = entityFactory.getApiSubmission(Submission.Status.IMPORTED)
        val submissionEdited = entityFactory.getApiSubmission(Submission.Status.EDITED)
        val submissionClosed = entityFactory.getApiSubmission(Submission.Status.CLOSED)
        val submissionAutoClosed = entityFactory.getApiSubmission(Submission.Status.AUTO_CLOSED)
        val submissionFinishedExternally = entityFactory.getApiSubmission(Submission.Status.FINISHED_EXTERNALLY)
        val submissionExported = entityFactory.getApiSubmission(Submission.Status.EXPORTED)
        val submissionLocked = entityFactory.getApiSubmission(Submission.Status.LOCKED)
        val submissionTerminated = entityFactory.getApiSubmission(Submission.Status.TERMINATED)
        val submissions = listOf(
            submissionImported,
            submissionEdited,
            submissionClosed,
            submissionAutoClosed,
            submissionFinishedExternally,
            submissionExported,
            submissionLocked,
            submissionTerminated,
        )

        `when`(apiSubmissionRepository.findAll()).thenReturn(submissions)

        val map = collectorServiceMock.getApiSubmissionsPerStatusTypeForAdmin()

        assertThat(map.size).isEqualTo(6)
        assertThat(map["Active"]).containsAll(listOf(submissionImported, submissionEdited, submissionLocked))
        assertThat(map["Closed"]).contains(submissionClosed)
        assertThat(map["Terminated"]).contains(submissionTerminated)
        assertThat(map["AutoClosed"]).contains(submissionAutoClosed)
        assertThat(map["FinishedExternally"]).contains(submissionFinishedExternally)
        assertThat(map["Exported"]).contains(submissionExported)
    }

    @Test
    fun `test get submission state per state for admin`() {
        val submissionImported = entityFactory.getUploadSubmission(Submission.Status.IMPORTED)
        val submissionEdited = entityFactory.getUploadSubmission(Submission.Status.EDITED)
        val submissionClosed = entityFactory.getUploadSubmission(Submission.Status.CLOSED)
        val submissionAutoClosed = entityFactory.getUploadSubmission(Submission.Status.AUTO_CLOSED)
        val submissionFinishedExternally = entityFactory.getUploadSubmission(Submission.Status.FINISHED_EXTERNALLY)
        val submissionExported = entityFactory.getUploadSubmission(Submission.Status.EXPORTED)
        val submissionLocked = entityFactory.getUploadSubmission(Submission.Status.LOCKED)
        val submissionTerminated = entityFactory.getUploadSubmission(Submission.Status.TERMINATED)
        val submissions = listOf(
            submissionImported, submissionEdited, submissionClosed,
            submissionAutoClosed, submissionFinishedExternally, submissionExported,
            submissionLocked, submissionTerminated
        )

        `when`(uploadSubmissionRepository.findAll()).thenReturn(submissions)

        val map = collectorServiceMock.getUploadedSubmissionsPerStatusTypeForAdmin()

        assertThat(map.size).isEqualTo(6)
        assertThat(map["Active"]).containsAll(listOf(submissionImported, submissionEdited, submissionLocked))
        assertThat(map["Closed"]).contains(submissionClosed)
        assertThat(map["Terminated"]).contains(submissionTerminated)
        assertThat(map["AutoClosed"]).contains(submissionAutoClosed)
        assertThat(map["FinishedExternally"]).contains(submissionFinishedExternally)
        assertThat(map["Exported"]).contains(submissionExported)
    }

    @Test
    fun `check formatted internal identifier`() {
        val submission = entityFactory.getApiSubmission()
        submission.identifier = "i12345"

        val formattedIdentifier = collectorServiceMock.getFormattedIdentifier(submission.identifier)

        assertThat(formattedIdentifier).isEqualTo("S#12345")
    }

    @Test
    fun `check formatted external identifier`() {
        val id = "o000001"

        val formattedIdentifier = collectorServiceMock.getFormattedIdentifier(id)

        assertThat(formattedIdentifier).isEqualTo(id)
    }

    @Test
    fun `check getAllSubmissionsPerUser`() {
        val submission1 = entityFactory.getApiSubmission()
        submission1.originProjects = "project1"
        val submission2 = entityFactory.getApiSubmission()
        submission2.originProjects = "project2;project3"

        `when`(externalMetadataSourceService.getValuesAsSet(matches("projectsByPerson"), anyMap())).thenReturn(setOf("project1", "project3"))
        `when`(submissionRepository.findAllByOriginProjectsContains("project1")).thenReturn(listOf(submission1))
        `when`(submissionRepository.findAllByOriginProjectsContains("project2")).thenReturn(listOf(submission2))
        `when`(submissionRepository.findAllByOriginProjectsContains("project3")).thenReturn(listOf(submission2))
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())

        val submissions = collectorServiceMock.getAllSubmissionsPerUser()

        assertThat(submissions.size).isEqualTo(2)
        assertThat(submissions).contains(submission1)
        assertThat(submissions).contains(submission2)
    }

    @Test
    fun `check getUploadedSubmissionsPerUser`() {
        val submission1 = entityFactory.getUploadSubmission()
        submission1.originProjects = "project1"
        val submission2 = entityFactory.getUploadSubmission()
        submission2.originProjects = "project2;project3"

        `when`(externalMetadataSourceService.getValuesAsSet(matches("projectsByPerson"), anyMap())).thenReturn(setOf("project1", "project3"))
        `when`(submissionRepository.findAllByOriginProjectsContains("project1")).thenReturn(listOf(submission1))
        `when`(submissionRepository.findAllByOriginProjectsContains("project2")).thenReturn(listOf(submission2))
        `when`(submissionRepository.findAllByOriginProjectsContains("project3")).thenReturn(listOf(submission2))
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())

        val submissions = collectorServiceMock.getUploadedSubmissionsPerUser()

        assertThat(submissions.size).isEqualTo(2)
        assertThat(submissions).contains(submission1)
        assertThat(submissions).contains(submission2)
    }

    @Test
    fun `get samples enriched by merging samples only find external samples`() {
        val sample1 = entityFactory.getSample()
        val sample2 = entityFactory.getSample(sample1.submission)
        val samples = setOf(sample1, sample2)

        `when`(externalMetadataSourceService.getValuesAsSetMap(matches("mergingCandidatesData"), anyMap()))
            .thenReturn(setOf(mapOf("file_withdrawn" to "false")))

        val result = collectorServiceMock.getSampleListEnrichedByMergingSamples(samples)

        assertThat(result.size).isEqualTo(samples.size * 2)
        assertThat(result.map { it.name }).contains("SAMPLE FROM OTP")
    }

    @Test
    fun `get samples enriched by merging samples only find internal samples`() {
        val sample1 = entityFactory.getSample()
        val sample2 = entityFactory.getSample(sample1.submission)
        val samples = setOf(sample1, sample2)
        val otherSample = entityFactory.getSample()

        `when`(externalMetadataSourceService.getValuesAsSetMap(matches("mergingCandidatesData"), anyMap())).thenReturn(emptySet())
        `when`(
            sampleRepository.findAllByProjectAndPidAndSampleTypeAndSeqTypeAndLibraryLayoutAndAntibodyTargetAndSubmissionNotAndSubmission_ImportedExternalIsFalse(
                anyString(), anyString(), anyString(), anyOrNull(), anyOrNull(), anyString(), anyOrNull()
            )
        ).thenReturn(setOf(otherSample))

        val result = collectorServiceMock.getSampleListEnrichedByMergingSamples(samples)

        assertThat(result.size).isEqualTo(samples.size * 2)
        assertThat(result.map { it.name }).contains("SAMPLE(S) FROM SUBMISSION ${otherSample.submission.identifier}")
    }

    @Test
    fun `get samples enriched by merging samples only find one external and one internal sample`() {
        val sample1 = entityFactory.getSample()
        val sample2 = entityFactory.getSample(sample1.submission)
        val samples = setOf(sample1, sample2)
        val otherSample = entityFactory.getSample()

        `when`(externalMetadataSourceService.getValuesAsSetMap("mergingCandidatesData", sample1.getMergingFieldData))
            .thenReturn(setOf(mapOf("file_withdrawn" to "false")))
        `when`(
            sampleRepository.findAllByProjectAndPidAndSampleTypeAndSeqTypeAndLibraryLayoutAndAntibodyTargetAndSubmissionNotAndSubmission_ImportedExternalIsFalse(
                sample2.project, sample2.pid, sample2.sampleType, sample2.seqType, sample2.libraryLayout, sample2.antibodyTarget, sample2.submission
            )
        ).thenReturn(setOf(otherSample))

        val result = collectorServiceMock.getSampleListEnrichedByMergingSamples(samples)

        assertThat(result.size).isEqualTo(samples.size * 2)
        assertThat(result.map { it.name }).contains("SAMPLE(S) FROM SUBMISSION ${otherSample.submission.identifier}")
        assertThat(result.map { it.name }).contains("SAMPLE FROM OTP")
    }

    @Test
    fun `get samples enriched by merging samples, find merging samples false`() {
        val sample1 = entityFactory.getSample()
        val sample2 = entityFactory.getSample(sample1.submission)
        val samples = setOf(sample1, sample2)

        val result = collectorServiceMock.getSampleListEnrichedByMergingSamples(samples, false)

        assertThat(result.size).isEqualTo(samples.size)
    }

    @Test
    fun `get sample list enriched by merging samples grouped`() {
        val sample1 = entityFactory.getSample()
        sample1.name += "1"
        val sample2 = entityFactory.getSample(sample1.submission)
        sample2.name += "2"
        val samples = listOf(sample1, sample2)
        val submission = sample1.submission as ApiSubmission
        submission.sequencingTechnology = entityFactory.getSequencingTechnology()
        val otherSample1 = entityFactory.getSample()
        val otherSample2 = entityFactory.getSample(otherSample1.submission)

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(samples)
        `when`(externalMetadataSourceService.getValuesAsSetMap(matches("mergingCandidatesData"), anyMap()))
            .thenReturn(setOf(mapOf("file_withdrawn" to "true")))
        `when`(
            sampleRepository.findAllByProjectAndPidAndSampleTypeAndSeqTypeAndLibraryLayoutAndAntibodyTargetAndSubmissionNotAndSubmission_ImportedExternalIsFalse(
                sample2.project, sample2.pid, sample2.sampleType, sample2.seqType, sample2.libraryLayout, sample2.antibodyTarget, sample2.submission
            )
        ).thenReturn(setOf(otherSample1, otherSample2))

        val result = collectorServiceMock.getSampleListEnrichedByMergingSamplesGrouped(submission)

        assertThat(result.size).isEqualTo(2)
        assertThat(result[sample1.getMergingFieldData.toString()]!!.size).isEqualTo(2)
        assertThat(result[sample1.getMergingFieldData.toString()]!!.map { it.name }).contains("WITHDRAWN SAMPLE FROM OTP")
        assertThat(result[sample2.getMergingFieldData.toString()]!!.size).isEqualTo(3)
        assertThat(result[sample2.getMergingFieldData.toString()]!!.map { it.name }).contains("WITHDRAWN SAMPLE FROM OTP")
        assertThat(result[sample2.getMergingFieldData.toString()]!!.map { it.name }).contains("SAMPLE(S) FROM SUBMISSION ${otherSample1.submission.identifier}")
    }

    @Test
    fun `get sample list enriched by merging samples grouped without finding merging samples`() {
        val sample1 = entityFactory.getSample()
        sample1.abstractSampleId += "1"
        val sample2 = entityFactory.getSample(sample1.submission)
        sample2.abstractSampleId += "2"
        val sample3 = entityFactory.getSample(sample1.submission)
        sample3.setProceed("no")
        val samples = listOf(sample1, sample2, sample3)
        val submission = sample1.submission as ApiSubmission
        submission.sequencingTechnology = entityFactory.getSequencingTechnologyWithClusterJobTemplate()

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(samples)

        val result = collectorServiceMock.getSampleListEnrichedByMergingSamplesGrouped(submission)

        assertThat(result.size).isEqualTo(2)
        assertThat(result[sample1.abstractSampleId ]!!.size).isEqualTo(1)
        assertThat(result[sample2.abstractSampleId ]!!.size).isEqualTo(1)
    }

    @Test
    fun `check getUploadedSubmissionsPerStatusTypeForUser`() {
        val submissionActive = entityFactory.getUploadSubmission(Submission.Status.EDITED)
        val submissionClosed = entityFactory.getUploadSubmission(Submission.Status.CLOSED)
        val submissionExported = entityFactory.getUploadSubmission(Submission.Status.EXPORTED)
        val submissionTerminated = entityFactory.getUploadSubmission(Submission.Status.TERMINATED)
        val submissionAutoClosed = entityFactory.getUploadSubmission(Submission.Status.AUTO_CLOSED)
        val submissionFinishedExternally = entityFactory.getUploadSubmission(Submission.Status.FINISHED_EXTERNALLY)
        val allSubmissions = listOf(submissionActive, submissionClosed, submissionTerminated, submissionAutoClosed, submissionFinishedExternally, submissionExported)

        `when`(externalMetadataSourceService.getValuesAsSet(matches("projectsByPerson"), anyMap())).thenReturn(setOf("project1"))
        `when`(submissionRepository.findAllByOriginProjectsContains("project1")).thenReturn(allSubmissions)
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())

        val submissions = collectorServiceMock.getUploadedSubmissionsPerStatusTypeForUser()

        assertThat(submissions.size).isEqualTo(3)
        assertThat(submissions["Active"]).contains(submissionActive)
        assertThat(submissions["Closed"]).contains(submissionClosed)
        assertThat(submissions["Exported"]).contains(submissionExported)
    }

    @Test
    fun `check getProjectsForAdmins`() {
        `when`(externalMetadataSourceService.getValuesAsSet("projectsWithClosed")).thenReturn(setOf("projectA(f)", "projectB(t)"))

        val projects = collectorServiceMock.getProjectsForAdmins()

        assertThat(projects.size).isEqualTo(2)
        assertThat(projects.first()).isEqualTo("projectA")
        assertThat(projects.last()).isEqualTo("projectB (closed)")
    }

    @Test
    fun `check getProjectsForSubmissionAndUser`() {
        val submission = entityFactory.getApiSubmission()
        submission.originProjects = "originProject"
        val sample = entityFactory.getSample(submission)

        `when`(externalMetadataSourceService.getValuesAsSet(matches("projects-by-person-or-organizational-unit"), anyMap())).thenReturn(setOf("projectA(f)", "projectB(t)"))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        val projects = collectorServiceMock.getProjectsForSubmissionAndUser(submission, entityFactory.getPerson())

        assertThat(projects.size).isEqualTo(4)
        assertThat(projects).isEqualTo(setOf("projectA", "projectB (closed)", "originProject", "project"))
    }

    @Test
    fun `test getImportableProjects`() {
        val submission = entityFactory.getApiSubmission()
        submission.originProjects = "originProject"
        submission.submitter = entityFactory.getPerson()
        val sample = entityFactory.getSample(submission)

        `when`(externalMetadataSourceService.getValuesAsSet(matches("projects-by-person-or-organizational-unit"), anyMap())).thenReturn(setOf("projectA(f)", "projectB(t)"))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        val result = collectorServiceMock.getImportableProjects(submission)

        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(4)
    }

    @Test
    fun `check getUrlsByPerson`() {
        val submission1 = entityFactory.getApiSubmission()
        val submission2 = entityFactory.getApiSubmission()
        val person = entityFactory.getPerson()

        `when`(submissionRepository.findAllBySubmitter(person)).thenReturn(listOf(submission1, submission2))

        val urls = collectorServiceMock.getUrlsByPerson(person)

        assertThat(urls.size).isEqualTo(2)
        assertThat(urls.values).contains(MetaValController.SIMPLE_TABLE_PAGE_USER + "?uuid=" + submission1.uuid)
    }

    @Test
    fun `check getProjectPrefixesForSamplesInSubmission`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val candidateProjects = setOf("project1", "project2")

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to "project"))).thenReturn("prefix")
        `when`(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to "project1"))).thenReturn("prefix1")
        `when`(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to "project2"))).thenReturn("prefix2")

        val prefixes = collectorServiceMock.getProjectPrefixesForSamplesInSubmission(submission, candidateProjects)

        assertThat(prefixes.size).isEqualTo(3)
        assertThat(prefixes.values).contains("prefix")
    }

    @Test
    fun `check getPathsWithSampleList with different sample types`() {
        val submission = entityFactory.getApiSubmission()
        submission.sequencingTechnology = entityFactory.getSequencingTechnology()
        val sample = entityFactory.getSample(submission)
        val sample1 = entityFactory.getSample(submission)
        sample1.seqType = sample.seqType
        sample1.setXenograft("true")
        sample1.antibodyTarget = ""
        val sample2 = entityFactory.getSample(submission)
        sample2.seqType = sample.seqType
        sample2.setXenograft("true")
        val samples = mapOf(
            sample.getMergingFieldData.toString() to listOf(sample),
            sample1.getMergingFieldData.toString() to listOf(sample1),
            sample2.getMergingFieldData.toString() to listOf(sample2)
        )
        val cachedProject = entityFactory.getProject()
        val projectPathTemplate = entityFactory.getRuntimeOption("<PROJECT>/sequencing/<SEQ_TYPE_EXT>/view-by-pid/<PID>/<SAMPLE_TYPE_EXT>/<WELL_LABEL>/<LIBRARY_LAYOUT>/run<RUN_NAME>/sequence/<FASTQ_FILE_NAME>")

        `when`(runtimeOptionsRepository.findByName("projectPathTemplate")).thenReturn(projectPathTemplate)
        `when`(projectRepository.findByName(sample.project)).thenReturn(cachedProject)
        `when`(externalMetadataSourceService.getSingleValue("SeqTypeDirName", mapOf("seqType" to sample.seqType?.name.orEmpty()))).thenReturn("seqTypeDir")

        val pathsWithSamples = collectorServiceMock.getPathsWithSampleList(samples, submission)

        assertThat(pathsWithSamples.size).isEqualTo(3)
        assertThat(pathsWithSamples.keys).contains("/path/to/project/sequencing/seqTypeDir/view-by-pid/prefix_pid/sample-type01-antibodyTarget/singlecellplate-singlecellwellposition/paired/run<RUN_NAME>/sequence/<FASTQ_FILE_NAME>")
        assertThat(pathsWithSamples.keys).contains("/path/to/project/sequencing/seqTypeDir/view-by-pid/prefix_pid/sample-type01-x/singlecellplate-singlecellwellposition/paired/run<RUN_NAME>/sequence/<FASTQ_FILE_NAME>")
        assertThat(pathsWithSamples.keys).contains("/path/to/project/sequencing/seqTypeDir/view-by-pid/prefix_pid/sample-type01-x-antibodyTarget/singlecellplate-singlecellwellposition/paired/run<RUN_NAME>/sequence/<FASTQ_FILE_NAME>")
    }

    @Test
    fun `check getPathsWithSampleList for ownTransfer`() {
        val submission = entityFactory.getApiSubmission()
        submission.sequencingTechnology = entityFactory.getSequencingTechnology()
        submission.sequencingTechnology.checkExternalMetadataSource = false
        val sample = entityFactory.getSample(submission)
        val samples = mapOf(sample.getMergingFieldData.toString() to listOf(sample))
        val cachedProject = entityFactory.getProject()
        val projectPathTemplateNonOtp = entityFactory.getRuntimeOption("<PROJECT>/nonOTP/ont/view-by-pid/<PID>/<SAMPLE_TYPE_INT>/<SEQ_TYPE_INT>/<ASID>/")

        `when`(runtimeOptionsRepository.findByName("projectPathTemplateNonOtp")).thenReturn(projectPathTemplateNonOtp)
        `when`(projectRepository.findByName(sample.project)).thenReturn(cachedProject)
        `when`(externalMetadataSourceService.getSingleValue("SeqTypeDirName", mapOf("seqType" to sample.seqType?.name.orEmpty()))).thenReturn("seqTypeDir")

        val pathsWithSamples = collectorServiceMock.getPathsWithSampleList(samples, submission)

        assertThat(pathsWithSamples.size).isEqualTo(1)
        assertThat(pathsWithSamples.keys).contains("/path/to/project/nonOTP/ont/view-by-pid/${sample.pid}/${sample.sampleType.lowercase()}/${sample.seqType!!.name.lowercase()}/${sample.abstractSampleId}/")
    }
}
