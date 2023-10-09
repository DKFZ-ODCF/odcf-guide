package de.dkfz.odcf.guide

import de.dkfz.odcf.guide.entity.Feedback
import de.dkfz.odcf.guide.entity.MetaDataColumn
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.Role
import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.metadata.SequencingTechnology
import de.dkfz.odcf.guide.entity.options.RuntimeOptions
import de.dkfz.odcf.guide.entity.otpCached.OtpCachedProject
import de.dkfz.odcf.guide.entity.parser.Parser
import de.dkfz.odcf.guide.entity.parser.ParserComponent
import de.dkfz.odcf.guide.entity.parser.ParserField
import de.dkfz.odcf.guide.entity.requestedValues.FieldRequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.SeqTypeRequestedValue
import de.dkfz.odcf.guide.entity.submissionData.*
import de.dkfz.odcf.guide.entity.validation.Validation
import de.dkfz.odcf.guide.entity.validation.ValidationLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ImportSourceDataRepository : JpaRepository<ImportSourceData, String> {
    fun findBySubmissionIdentifier(submissionIdentifier: String): ImportSourceData?
}

interface PersonRepository : JpaRepository<Person, Int> {
    fun findByUsername(username: String): Person?
    fun findByMail(mail: String): Person?
    fun findByApiToken(token: String): Person?
    fun findAllByAccountDisabled(flag: Boolean): Set<Person>
}

interface SampleRepository : JpaRepository<Sample, Int> {
    fun findAllBySubmission(submission: Submission): List<Sample>
    fun findFirstBySubmission(submission: Submission): Sample
    fun existsBySubmission(submission: Submission): Boolean
    fun findAllBySeqTypeAndSubmission_StatusIn(seqType: SeqType, status: List<Submission.Status>): List<Sample>
    fun findAllBySubmissionIn(submission: Set<Submission>): Set<Sample>
    fun findBySubmissionAndProceedNot(submission: Submission, proceed: Sample.Proceed): List<Sample>
    fun findBySubmissionOrderByIdAsc(submission: Submission): List<Sample>
    fun findAllBySubmissionOrderById(submission: Submission): List<Sample>
    fun findBySubmissionAndName(submission: Submission, name: String): Sample?
    fun findAllByPidEndsWithAndProjectNotAndSeqType_Name(pseudonym: String, project: String, SeqTypeName: String): List<Sample>

    fun findAllByProjectAndPidAndSampleTypeAndSeqTypeAndLibraryLayoutAndAntibodyTargetAndSubmissionNotAndSubmission_ImportedExternalIsFalse(
        project: String,
        pid: String,
        sampleType: String,
        seqType: SeqType?,
        libraryLayout: Sample.LibraryLayout?,
        antibodyTarget: String,
        submission: Submission
    ): Set<Sample>
    fun countAllBySubmission(submission: Submission): Int
}

interface TechnicalSampleRepository : JpaRepository<TechnicalSample, Int>

interface SubmissionRepository : JpaRepository<Submission, String> {
    fun findByUuid(uuid: UUID): Submission?
    fun findByIdentifier(identifier: String): Submission?
    fun findAllBySubmitter(submitter: Person): List<Submission>
    fun findAllByStatus(status: Submission.Status): List<Submission>
    fun findAllByStatusIn(status: List<Submission.Status>): List<Submission>
    fun findTop100ByStatusInOrderByImportDateDesc(status: List<Submission.Status>): Set<Submission>
    fun findTopByIdentifierStartsWithOrderByIdentifierDesc(string: String): Submission?
    fun findAllByOriginProjectsContains(project: String): List<Submission>
    fun findAllByStartTerminationPeriodIsBeforeAndStatusInAndTerminationStateNotIn(
        date: Date,
        status: List<Submission.Status>,
        terminationState: List<Submission.TerminationState>
    ): Set<Submission>

    @Query(
        "SELECT status AS state, " +
            "COUNT(status) AS total_submissions, " +
            "COALESCE(SUM(sample_count.count), 0) AS total_samples " +
            "FROM submission " +
            "LEFT JOIN (SELECT submission_identifier, COUNT(id) FROM sample GROUP BY submission_identifier) AS sample_count " +
            "ON submission.identifier = sample_count.submission_identifier " +
            "GROUP BY state ORDER BY state",
        nativeQuery = true
    )
    fun getSubmissionAndSampleSizeByState(): List<Map<String, String>>

    @Query(
        "SELECT TO_CHAR(submission.import_date, 'yyyy.mm') AS date, " +
            "TO_CHAR(submission.import_date, 'Month yyyy') AS month, " +
            "COUNT(identifier) AS total_submissions, " +
            "COUNT(identifier) FILTER ( WHERE status != 'AUTO_CLOSED' ) AS submissions_without_auto_closed,  " +
            "COALESCE(SUM(sample_count.count), 0) AS total_samples, " +
            "COALESCE(SUM(sample_count.count) FILTER ( WHERE status != 'AUTO_CLOSED' ), 0) AS samples_without_auto_closed " +
            "FROM submission " +
            "LEFT JOIN (SELECT submission_identifier, COUNT(id) FROM sample GROUP BY submission_identifier) AS sample_count ON submission.identifier = sample_count.submission_identifier " +
            "GROUP BY date, month ORDER BY date",
        nativeQuery = true
    )
    fun getSubmissionAndSampleSizeByDate(): List<Map<String, String>>
}

interface ApiSubmissionRepository : JpaRepository<ApiSubmission, String> {
    fun findByIdentifier(identifier: String): ApiSubmission?
    fun findAllByStatusInAndImportedExternalIsFalse(status: List<Submission.Status>): Set<ApiSubmission>
}

interface UploadSubmissionRepository : JpaRepository<UploadSubmission, String> {
    fun findByIdentifier(identifier: String): UploadSubmission?
    fun findAllBySubmitter(submitter: Person): List<UploadSubmission>
}

interface FileRepository : JpaRepository<File, Int> {
    fun findByFileNameLikeIgnoreCaseAndSample_Submission(filename: String, submission: Submission): List<File>
    fun findAllBySample(sample: Sample): List<File>
    fun findAllBySampleIn(samples: List<Sample>): List<File>
    fun findByUuid(uuid: UUID): File?
    fun findAllByMd5(md5: String): List<File>
    fun findAllByMd5In(md5sums: List<String>): List<File>
}

interface SeqTypeRepository : JpaRepository<SeqType, Int> {
    fun findByName(name: String): SeqType?
    fun findAllByNeedAntibodyTargetIsTrue(): Set<SeqType>
    fun findAllByIsRequestedIsTrue(): Set<SeqType>
    fun findAllByIsRequestedIsFalseOrderByNameAsc(): Set<SeqType>
    fun findAllByOrderByNameAsc(): Set<SeqType>
}

interface FeedbackRepository : JpaRepository<Feedback, Int> {
    fun findAllByOrderByDateDesc(): List<Feedback>
}

interface ValidationRepository : JpaRepository<Validation, Int> {
    fun findByField(field: String): Validation
}

interface OtpCachedProjectRepository : JpaRepository<OtpCachedProject, Int> {
    fun findByName(name: String): OtpCachedProject?
    fun findAllByNameIn(names: Set<String>): Set<OtpCachedProject>
}

interface MetaDataColumnRepository : JpaRepository<MetaDataColumn, Int> {
    fun findByColumnNameOrImportAliasesContains(columnName: String, importAliases: String): MetaDataColumn?
}

interface ParserRepository : JpaRepository<Parser, Int> {
    fun findByProject(project: String): Parser?
}

interface ParserFieldRepository : JpaRepository<ParserField, Int>

interface ParserComponentRepository : JpaRepository<ParserComponent, Int> {
    fun findAllByParserField(parserField: ParserField): List<ParserComponent>
}

interface RuntimeOptionsRepository : JpaRepository<RuntimeOptions, Int> {
    fun findByName(name: String): RuntimeOptions?
}

interface ValidationLevelRepository : JpaRepository<ValidationLevel, Int> {
    fun findByName(name: String): ValidationLevel?
    fun findByDefaultObjectIsTrue(): Set<ValidationLevel>
}

interface SequencingTechnologyRepository : JpaRepository<SequencingTechnology, Int> {
    fun findByName(name: String): SequencingTechnology?
    fun findByDefaultObjectIsTrue(): Set<SequencingTechnology>
}

interface RequestedValuesRepository : JpaRepository<RequestedValue, Int>

interface FieldRequestedValuesRepository : JpaRepository<FieldRequestedValue, Int> {
    fun findByFieldName(fieldName: String): Set<FieldRequestedValue>
    fun findAllByUsedSubmissionsContainsOrRequester(submission: Submission, requester: Person): Set<FieldRequestedValue>
    fun findAllByFieldNameAndRequestedValue(fieldName: String, requestedValue: String): Set<FieldRequestedValue>
    fun findAllByUsedSubmissionsContains(submission: Submission): Set<FieldRequestedValue>
    fun findAllByState(state: RequestedValue.State): Set<FieldRequestedValue>
    fun findAllByStateAndFieldName(state: RequestedValue.State, fieldName: String): Set<FieldRequestedValue>
    fun findAllByStateIn(states: List<RequestedValue.State>): Set<FieldRequestedValue>
}

interface SeqTypeRequestedValuesRepository : JpaRepository<SeqTypeRequestedValue, Int> {
    fun findByRequestedValue(requestedValue: String): FieldRequestedValue?
    fun findAllByState(state: RequestedValue.State): Set<SeqTypeRequestedValue>
    fun findAllByUsedSubmissionsContains(submission: Submission): Set<SeqTypeRequestedValue>
    fun findAllByUsedSubmissionsContainsOrRequester(submission: Submission, requester: Person): Set<SeqTypeRequestedValue>
    fun findAllByRequestedValue(requestedValue: String): Set<SeqTypeRequestedValue>
    fun findAllByRequestedSeqType(requestedSeqType: SeqType): Set<SeqTypeRequestedValue>
    fun findAllByRequestedSeqType_IsRequestedIsTrue(): Set<SeqTypeRequestedValue>
}

interface ClusterJobRepository : JpaRepository<ClusterJob, Int> {
    fun findByRemoteId(id: Int): ClusterJob
    fun findAllByStateIn(state: Set<ClusterJob.State>): Set<ClusterJob>
    fun findAllBySubmission(submission: Submission): Set<ClusterJob>
    fun findAllBySubmissionAndParentJobIsNullAndRestartedJobIsNull(submission: Submission): ClusterJob?
    fun findAllBySubmissionAndJobNameStartsWith(submission: Submission, jobName: String): Set<ClusterJob>
    fun findByParentJobAndRestartedJobIsNull(job: ClusterJob): ClusterJob?
}

interface RoleRepository : JpaRepository<Role, Int> {
    fun findByName(name: String): Role
}
