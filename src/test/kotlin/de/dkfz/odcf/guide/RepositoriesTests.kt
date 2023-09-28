package de.dkfz.odcf.guide

import de.dkfz.odcf.guide.helper.EntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
open class RepositoriesTests @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val personRepository: PersonRepository,
    private val sampleRepository: SampleRepository,
    private val submissionRepository: SubmissionRepository,
    private val fileRepository: FileRepository,
    private val apiSubmissionRepository: ApiSubmissionRepository,
    private val uploadSubmissionRepository: UploadSubmissionRepository,
    private val parserRepository: ParserRepository,
    private val fieldRequestedValuesRepository: FieldRequestedValuesRepository,
) {

    private val entityFactory = EntityFactory()

    @Test
    fun `When findByUsername then return User`() {
        val person = entityFactory.getPerson()

        entityManager.persist(person)
        entityManager.flush()

        val user = personRepository.findByUsername(person.username)
        assertThat(user).isEqualTo(person)
    }

    @Test
    fun `When findBySubmission then return list of samples`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)

        entityManager.persist(submission)
        entityManager.persist(sample.seqType)
        entityManager.persist(sample)
        entityManager.flush()

        val samples = sampleRepository.findAllBySubmission(submission)
        assertThat(samples.size).isEqualTo(1)
        assertThat(samples[0].submission).isEqualTo(submission)
        assertThat(samples[0].pid).isEqualTo(sample.pid)
    }

    @Test
    fun `When findBySubmissionOrderByIdAsc then return ordered list of samples`() {
        val submission = entityFactory.getApiSubmission()
        val sample1 = entityFactory.getSample(submission)
        val sample2 = entityFactory.getSample(submission)

        entityManager.persist(submission)
        entityManager.persist(sample1.seqType)
        entityManager.persist(sample1)
        entityManager.persist(sample2.seqType)
        entityManager.persist(sample2)
        entityManager.flush()

        val samples = sampleRepository.findAllBySubmission(submission)
        assertThat(samples.size).isEqualTo(2)
        assertThat(samples[0].pid).isEqualTo(sample2.pid)
        assertThat(samples[1].pid).isEqualTo(sample1.pid)
    }

    @Test
    fun `When findByUuid then return submission`() {
        val submission = entityFactory.getApiSubmission()

        entityManager.persist(submission)
        entityManager.flush()

        val submissionR = submissionRepository.findByUuid(submission.uuid)
        assertThat(submissionR).isEqualTo(submission)
    }

    @Test
    fun `When findByIdentifier then return submission`() {
        val submission = entityFactory.getApiSubmission()

        entityManager.persist(submission)
        entityManager.flush()

        val submissionR = submissionRepository.findByIdentifier(submission.identifier)
        assertThat(submissionR).isEqualTo(submission)
    }

    @Test
    fun `When findAllByEmail then return submission`() {
        val submission = entityFactory.getApiSubmission()
        val person = entityFactory.getPerson()

        entityManager.persist(person)
        submission.submitter = person
        entityManager.persist(submission)
        entityManager.flush()

        val submissionR = submissionRepository.findAllBySubmitter(submission.submitter)
        assertThat(submissionR.size).isEqualTo(1)
        assertThat(submissionR[0]).isEqualTo(submission)
    }

    @Test
    fun `When findAllByStatus then return submission`() {
        val submission = entityFactory.getApiSubmission()

        entityManager.persist(submission)
        entityManager.flush()

        val submissionR = submissionRepository.findAllByStatus(submission.status)
        assertThat(submissionR.size).isEqualTo(1)
        assertThat(submissionR[0]).isEqualTo(submission)
    }

    @Test
    fun `When findTopByIdentifierStartsWithOrderByIdentifierDesc then return submission`() {
        val submission = entityFactory.getApiSubmission()

        entityManager.persist(submission)
        entityManager.flush()

        val submissionR = submissionRepository.findTopByIdentifierStartsWithOrderByIdentifierDesc("i")
        assertThat(submissionR).isEqualTo(submission)
    }

    @Test
    fun `When findAllByMd5 then return list of files`() {
        val file1 = entityFactory.getFile()
        val file2 = entityFactory.getFile()

        entityManager.persist(file1.sample.submission)
        entityManager.persist(file1.sample.seqType)
        entityManager.persist(file1.sample)
        entityManager.persist(file2.sample.submission)
        entityManager.persist(file2.sample.seqType)
        entityManager.persist(file2.sample)
        entityManager.persist(file1)
        entityManager.persist(file2)
        entityManager.flush()

        val files = fileRepository.findAllByMd5("md5")
        assertThat(files.size).isEqualTo(2)
        assertThat(files[0].fileName).isEqualTo("fileName_R1.fastq.gz")
    }

    fun `When findAllBySubmissionType then return of submissions`() {
        val apiSubmission = entityFactory.getApiSubmission()
        val uploadSubmission = entityFactory.getUploadSubmission()

        entityManager.persist(apiSubmission)
        entityManager.persist(uploadSubmission)
        entityManager.flush()

        val submissionApi = apiSubmissionRepository.findAll()[0]
        val submissionUpload = uploadSubmissionRepository.findAll()[0]
        assertThat(submissionApi).isEqualTo(apiSubmission)
        assertThat(submissionUpload).isEqualTo(uploadSubmission)
    }

    @Test
    fun `When findByProject then return parser`() {
        val parser = entityFactory.getParser()

        entityManager.persist(parser)
        entityManager.flush()

        val foundParser = parserRepository.findByProject("project")
        assertThat(foundParser).isEqualTo(parser)
    }

    @Test
    fun `When findAllByPidEndsWith then find other pid`() {
        val pseudonym = "pid1"
        val sample1 = entityFactory.getSample()
        sample1.pid = "prefix1_$pseudonym"
        val sample2 = entityFactory.getSample()
        sample2.pid = "prefix2_$pseudonym"
        sample2.seqType = sample1.seqType
        sample2.project += "_other"

        entityManager.persist(sample1.seqType)
        entityManager.persist(sample1.submission)
        entityManager.persist(sample1)
        entityManager.persist(sample2.seqType)
        entityManager.persist(sample2.submission)
        entityManager.persist(sample2)
        entityManager.flush()

        val foundSamples = sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(pseudonym, sample1.project, sample1.seqType!!.name)
        assertThat(foundSamples).hasSize(1)
        assertThat(foundSamples).contains(sample2)
    }

    @Test
    fun `When findAllByPidEndsWith then do not find other pid with wrong pseudonym`() {
        val pseudonym = "pid1"
        val sample1 = entityFactory.getSample()
        sample1.pid = "prefix1_$pseudonym"
        val sample2 = entityFactory.getSample()
        sample2.pid = "prefix2_${pseudonym}1"
        sample2.seqType = sample1.seqType
        sample2.project += "_other"

        entityManager.persist(sample1.seqType)
        entityManager.persist(sample1.submission)
        entityManager.persist(sample1)
        entityManager.persist(sample2.seqType)
        entityManager.persist(sample2.submission)
        entityManager.persist(sample2)
        entityManager.flush()

        val foundSamples = sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(pseudonym, sample1.project, sample1.seqType!!.name)
        assertThat(foundSamples).hasSize(0)
        assertThat(foundSamples).doesNotContain(sample2)
    }

    @Test
    fun `When findAllByPidEndsWith then do not find other pid with wrong seqType`() {
        val pseudonym = "pid1"
        val sample1 = entityFactory.getSample()
        sample1.pid = "prefix1_$pseudonym"
        val sample2 = entityFactory.getSample()
        sample2.pid = "prefix2_$pseudonym"
        sample2.project += "_other"

        entityManager.persist(sample1.seqType)
        entityManager.persist(sample1.submission)
        entityManager.persist(sample1)
        entityManager.persist(sample2.seqType)
        entityManager.persist(sample2.submission)
        entityManager.persist(sample2)
        entityManager.flush()

        val foundSamples = sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(pseudonym, sample1.project, sample1.seqType!!.name)
        assertThat(foundSamples).hasSize(0)
        assertThat(foundSamples).doesNotContain(sample2)
    }

    @Test
    fun `When findAllByPidEndsWith then do not find other pid in same project`() {
        val pseudonym = "pid1"
        val sample1 = entityFactory.getSample()
        sample1.pid = "prefix1_$pseudonym"
        val sample2 = entityFactory.getSample()
        sample2.pid = "prefix2_$pseudonym"
        sample2.project = sample1.project
        sample2.seqType = sample1.seqType

        entityManager.persist(sample1.seqType)
        entityManager.persist(sample1.submission)
        entityManager.persist(sample1)
        entityManager.persist(sample2.seqType)
        entityManager.persist(sample2.submission)
        entityManager.persist(sample2)
        entityManager.flush()

        val foundSamples = sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(pseudonym, sample1.project, sample1.seqType!!.name)
        assertThat(foundSamples).hasSize(0)
        assertThat(foundSamples).doesNotContain(sample2)
    }

    @Test
    fun `When findByOriginSubmissionOrRequester then return RequestedValue`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        person.username = "person"
        val requestedValue = entityFactory.getRequestedValue()
        val requestedValue2 = entityFactory.getRequestedValue(submission)
        requestedValue2.requester = person

        entityManager.persist(submission)
        entityManager.persist(person)
        entityManager.persist(requestedValue)
        entityManager.persist(requestedValue.requester)
        entityManager.persist(requestedValue.originSubmission)
        entityManager.persist(requestedValue2)
        entityManager.flush()

        val foundRequestedValue = fieldRequestedValuesRepository.findAllByUsedSubmissionsContainsOrRequester(submission, person)

        assertThat(foundRequestedValue.size).isEqualTo(1)
        assertThat(foundRequestedValue).contains(requestedValue2)
        assertThat(foundRequestedValue).doesNotContain(requestedValue)
    }
}
