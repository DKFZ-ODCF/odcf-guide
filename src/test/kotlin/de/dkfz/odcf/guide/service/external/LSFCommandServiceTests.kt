package de.dkfz.odcf.guide.service.external

import de.dkfz.odcf.guide.ClusterJobRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.cluster.ClusterJob.State.*
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.exceptions.JobAlreadySubmittedException
import de.dkfz.odcf.guide.exceptions.RuntimeOptionsNotFoundException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.external.LSFCommandServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.IOException
import java.util.*

@SpringBootTest
class LSFCommandServiceTests @Autowired constructor() {

    @InjectMocks
    lateinit var lsfCommandServiceMock: LSFCommandServiceImpl

    @Mock
    lateinit var clusterJobRepository: ClusterJobRepository

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var remoteCommandsService: RemoteCommandsService

    private val entityFactory = EntityFactory()
    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @Test
    fun `test submit cluster job without params`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()
        val basePath = "base/path/"
        val subPath = "internal/core/<ILSE_PREFIX>/<ILSE_ID>/<SUBMISSION_ID>.meta.guide.tsv"
        val identifier = submission.identifier
        val logPath = basePath + subPath.replace("<ILSE_PREFIX>", identifier.substring(2, 5))
            .replace("<ILSE_ID>", identifier.replace("i0", ""))
            .replace("<SUBMISSION_ID>", identifier)
            .replace("meta.guide.tsv", "job_log")
        val subject = mailBundle.getString("lsfService.processingStatusUpdateSubject")
        val body = mailBundle.getString("lsfService.processingStatusUpdateBody")

        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption(basePath))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption(subPath))
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("false"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("found")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "lsfService.processingStatusUpdateSubject")).thenReturn(subject)
        `when`(mailContentGeneratorService.getProcessingStatusUpdateBody(any())).thenReturn(body)

        val result = lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, emptyMap(), submission)

        verify(mailSenderService, times(1)).sendMailToTicketSystem(subject, body)
        assertThat(result.jobName).isEqualTo("${clusterJobTemplate.name}_$identifier")
        assertThat(result.remoteId).isEqualTo(-1)
        assertThat(result.pathToLog).isEqualTo(logPath)
        assertThat(result.command).isEqualTo(
            "bsub -W 1440 -R rusage[mem=1024] " +
                "-J \"${clusterJobTemplate.name}_$identifier\" " +
                "-g \"/odcf-guide/${clusterJobTemplate.group}\" " +
                "-o ${logPath}_${clusterJobTemplate.name}_$identifier.out " +
                "-e ${logPath}_${clusterJobTemplate.name}_$identifier.err " +
                "\"${clusterJobTemplate.command} \""
        )
        assertThat(result.state).isEqualTo(PENDING)
    }

    @Test
    fun `test submit cluster job with params`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()
        val additionParams = mapOf("-additional" to "param")
        val basePath = "base/path/"
        val subPath = "internal/core/<ILSE_PREFIX>/<ILSE_ID>/<SUBMISSION_ID>.meta.guide.tsv"
        val identifier = submission.identifier
        val logPath = basePath + subPath.replace("<ILSE_PREFIX>", identifier.substring(2, 5))
            .replace("<ILSE_ID>", identifier.replace("i0", ""))
            .replace("<SUBMISSION_ID>", identifier)
            .replace("meta.guide.tsv", "job_log")
        val subject = mailBundle.getString("lsfService.processingStatusUpdateSubject")
        val body = mailBundle.getString("lsfService.processingStatusUpdateBody")

        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption(basePath))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption(subPath))
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("false"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("found")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "lsfService.processingStatusUpdateSubject")).thenReturn(subject)
        `when`(mailContentGeneratorService.getProcessingStatusUpdateBody(any())).thenReturn(body)

        val result = lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, additionParams, submission)

        verify(mailSenderService, times(1)).sendMailToTicketSystem(subject, body)
        assertThat(result.jobName).isEqualTo("${clusterJobTemplate.name}_$identifier")
        assertThat(result.remoteId).isEqualTo(-1)
        assertThat(result.pathToLog).isEqualTo(logPath)
        assertThat(result.command).isEqualTo(
            "bsub -W 1440 -R rusage[mem=1024] " +
                "-J \"${clusterJobTemplate.name}_$identifier\" " +
                "-g \"/odcf-guide/${clusterJobTemplate.group}\" " +
                "-o ${logPath}_${clusterJobTemplate.name}_$identifier.out " +
                "-e ${logPath}_${clusterJobTemplate.name}_$identifier.err " +
                "\"${clusterJobTemplate.command} ${additionParams.map { "${it.key} ${it.value}" }.joinToString(" ")}\""
        )
        assertThat(result.state).isEqualTo(PENDING)
    }

    @Test
    fun `test submit dependent cluster jobs`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val subClusterJobTemplate = entityFactory.getClusterJobTemplate()
        clusterJobTemplate.subsequentJobTemplate = subClusterJobTemplate
        val submission = entityFactory.getApiSubmission()
        val basePath = "base/path/"
        val subPath = "internal/core/<ILSE_PREFIX>/<ILSE_ID>/<SUBMISSION_ID>.meta.guide.tsv"

        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption(basePath))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption(subPath))
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("false"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("found")

        val result = lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, emptyMap(), submission)

        assertThat(result).isNotNull
        verify(mailSenderService, times(1)).sendMailToTicketSystem(anyOrNull(), anyOrNull())
        val jobCaptor = ArgumentCaptor.forClass(ClusterJob::class.java)
        verify(clusterJobRepository, times(3)).save(jobCaptor.capture())
        assertThat(jobCaptor.allValues.distinct().size).isEqualTo(2)
        assertThat(jobCaptor.allValues.distinct()[1].parentJob).isEqualTo(jobCaptor.allValues.distinct()[0])
    }

    @Test
    fun `test submit cluster job without cluster job template`() {
        val submission = entityFactory.getApiSubmission()
        val subject = mailBundle.getString("lsfService.processingStatusUpdateSubject")
        val body = mailBundle.getString("lsfService.processingStatusUpdateBody")

        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("false"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/group")).thenReturn("found")
        `when`(mailContentGeneratorService.getTicketSubject(submission, "lsfService.processingStatusUpdateSubject")).thenReturn(subject)
        `when`(mailContentGeneratorService.getProcessingStatusUpdateBody(any())).thenReturn(body)

        val result = lsfCommandServiceMock.submitClusterJob(submission = submission, name = "name", groupName = "group", command = "command", outputPath = "output")

        verify(mailSenderService, times(1)).sendMailToTicketSystem(subject, body)
        assertThat(result.jobName).isEqualTo("name")
        assertThat(result.remoteId).isEqualTo(-1)
        assertThat(result.command).isEqualTo(
            "bsub -W 1440 -R rusage[mem=1024] " +
                "-J \"name\" " +
                "-g \"/odcf-guide/group\" " +
                "-o output_name.out " +
                "-e output_name.err " +
                "\"command\""
        )
        assertThat(result.state).isEqualTo(PENDING)
    }

    @Test
    fun `test submit cluster job with exception base path`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()
        val additionParams = mapOf("-additional" to "param")

        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("false"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("found")

        assertThatExceptionOfType(RuntimeOptionsNotFoundException::class.java).isThrownBy {
            lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, additionParams, submission)
        }.withMessage("runtime options 'tsvBasePath' was not found in DB")
    }

    @Test
    fun `test submit cluster job with exception sub path`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()
        val additionParams = mapOf("-additional" to "param")

        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption("basePath"))
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("false"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("found")

        assertThatExceptionOfType(RuntimeOptionsNotFoundException::class.java).isThrownBy {
            lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, additionParams, submission)
        }.withMessage("runtime options 'tsvInternalSubpath' was not found in DB")
    }

    @Test
    fun `test submit cluster job and create job group`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()
        val basePath = "base/path/"
        val subPath = "internal/core/<ILSE_PREFIX>/<ILSE_ID>/<SUBMISSION_ID>.meta.guide.tsv"

        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption(basePath))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption(subPath))
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("false"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("No job group found")

        lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, emptyMap(), submission)

        verify(remoteCommandsService, times(1)).getFromRemote("""bgadd -L "10" "/odcf-guide/${clusterJobTemplate.group}"""")
    }

    @Test
    fun `test submit cluster job and start it`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()
        val basePath = "base/path/"
        val subPath = "internal/core/<ILSE_PREFIX>/<ILSE_ID>/<SUBMISSION_ID>.meta.guide.tsv"

        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption(basePath))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption(subPath))
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("true"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("No job group found")
        `when`(remoteCommandsService.getFromRemote(startsWith("bsub"))).thenReturn("123")
        `when`(clusterJobRepository.findAllByStateIn(setOf(PENDING))).thenReturn(setOf(entityFactory.getClusterJob()))

        lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, emptyMap(), submission)

        verify(remoteCommandsService, times(1)).getFromRemote(startsWith("bsub"))
        verify(mailSenderService, times(2)).sendMailToTicketSystem(anyOrNull(), anyOrNull())
    }

    @Test
    fun `test submit cluster job and start it but io exception`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()
        val basePath = "base/path/"
        val subPath = "internal/core/<ILSE_PREFIX>/<ILSE_ID>/<SUBMISSION_ID>.meta.guide.tsv"

        `when`(runtimeOptionsRepository.findByName("tsvBasePath")).thenReturn(entityFactory.getRuntimeOption(basePath))
        `when`(runtimeOptionsRepository.findByName("tsvInternalSubpath")).thenReturn(entityFactory.getRuntimeOption(subPath))
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("true"))
        `when`(remoteCommandsService.getFromRemote("bjgroup -s /odcf-guide/${clusterJobTemplate.group}")).thenReturn("No job group found")
        `when`(remoteCommandsService.getFromRemote(startsWith("bsub"))).thenThrow(IOException("fake Exeption"))
        `when`(clusterJobRepository.findAllByStateIn(setOf(PENDING))).thenReturn(setOf(entityFactory.getClusterJob()))

        assertThatExceptionOfType(GuideRuntimeException::class.java).isThrownBy {
            lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, emptyMap(), submission)
        }.withMessageStartingWith("could not execute:")

        verify(mailSenderService, times(0)).sendMailToTicketSystem("job created and started", "job created and started")
    }

    @Test
    fun `test submit cluster job with already submitted job`() {
        val clusterJobTemplate = entityFactory.getClusterJobTemplate()
        val submission = entityFactory.getApiSubmission()

        `when`(clusterJobRepository.findAllBySubmissionAndJobNameStartsWith(submission, clusterJobTemplate.name))
            .thenReturn(setOf(entityFactory.getClusterJob()))

        assertThatExceptionOfType(JobAlreadySubmittedException::class.java).isThrownBy {
            lsfCommandServiceMock.submitClusterJob(clusterJobTemplate, emptyMap(), submission)
        }.withMessageStartingWith("Job for submission ${submission.identifier} already submitted")

        verify(mailSenderService, times(0)).sendMailToTicketSystem("job created and started", "job created and started")
    }

    @Test
    fun `test cluster job scheduler start job`() {
        val clusterJob = entityFactory.getClusterJob()
        val subject = mailBundle.getString("lsfService.processingStatusUpdateSubject")
        val body = mailBundle.getString("lsfService.processingStatusUpdateBody")

        `when`(clusterJobRepository.findAllByStateIn(setOf(PENDING))).thenReturn(setOf(clusterJob))
        `when`(clusterJobRepository.findAllBySubmissionAndParentJobIsNullAndRestartedJobIsNull(clusterJob.submission)).thenReturn(clusterJob)
        `when`(remoteCommandsService.getFromRemote(startsWith("bsub"))).thenReturn("123")
        `when`(runtimeOptionsRepository.findByName("autoStartJobs")).thenReturn(entityFactory.getRuntimeOption("true"))
        `when`(mailContentGeneratorService.getTicketSubject(clusterJob.submission, "lsfService.processingStatusUpdateSubject")).thenReturn(subject)
        `when`(mailContentGeneratorService.getProcessingStatusUpdateBody(listOf(clusterJob))).thenReturn(body)

        lsfCommandServiceMock.clusterJobScheduler()

        verify(remoteCommandsService, times(1)).getFromRemote(startsWith("bsub"))
        verify(mailSenderService, times(1)).sendMailToTicketSystem(subject, body)
    }

    @Test
    fun `test cluster job scheduler check job with error`() {
        val clusterJob = entityFactory.getClusterJob()
        clusterJob.remoteId = 123456789
        val json = "{\n" +
            "  \"COMMAND\":\"bjobs\",\n" +
            "  \"JOBS\":1,\n" +
            "  \"RECORDS\":[\n" +
            "    {\n" +
            "      \"JOBID\":\"${clusterJob.remoteId}\",\n" +
            "      \"ERROR\":\"some Error\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n"

        `when`(clusterJobRepository.findAllByStateIn(setOf(RUNNING, SUBMITTED))).thenReturn(setOf(clusterJob))
        `when`(remoteCommandsService.getFromRemote(startsWith("bjobs"))).thenReturn(json)
        `when`(clusterJobRepository.findByRemoteId(clusterJob.remoteId)).thenReturn(clusterJob)

        lsfCommandServiceMock.clusterJobScheduler()

        assertThat(clusterJob.state).isEqualTo(UNKNOWN)
    }

    @TestFactory
    fun `test cluster job scheduler`() = listOf(
        FAILED to "lsfService.failedUpdateSubject",
        UNKNOWN to "lsfService.processingStatusUpdateSubject",
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("test cluster job scheduler check job with $input") {
            val clusterJob = entityFactory.getClusterJob()
            clusterJob.setState("RUN")
            clusterJob.remoteId = 123456789
            val host = "odcf-cn12u34s56"
            val json = "{\n" +
                "  \"COMMAND\":\"bjobs\",\n" +
                "  \"JOBS\":1,\n" +
                "  \"RECORDS\":[\n" +
                "    {\n" +
                "      \"JOBID\":\"${clusterJob.remoteId}\",\n" +
                "      \"STAT\":\"${input.lsfState}\",\n" +
                "      \"EXEC_HOST\":\"$host\",\n" +
                "      \"EXIT_CODE\":\"1\",\n" +
                "      \"START_TIME\":\"Aug  2 14:18:45 2022\",\n" +
                "      \"FINISH_TIME\":\"Aug  2 14:18:51 2022 L\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
            val subject = mailBundle.getString(expected)
            val body = mailBundle.getString("lsfService.processingStatusUpdateBody")

            `when`(clusterJobRepository.findAllByStateIn(setOf(RUNNING, SUBMITTED, UNKNOWN))).thenReturn(setOf(clusterJob))
            `when`(remoteCommandsService.getFromRemote(startsWith("bjobs"))).thenReturn(json)
            `when`(clusterJobRepository.findByRemoteId(clusterJob.remoteId)).thenReturn(clusterJob)
            `when`(mailContentGeneratorService.getTicketSubject(clusterJob.submission, expected)).thenReturn(subject)
            `when`(mailContentGeneratorService.getProcessingStatusUpdateBody(any())).thenReturn(body)

            lsfCommandServiceMock.clusterJobScheduler()

            assertThat(clusterJob.hostName).isEqualTo(host)
            assertThat(clusterJob.state).isEqualTo(input)
            assertThat(clusterJob.exitCode).isEqualTo(1)
            verify(mailSenderService, times(1)).sendMailToTicketSystem(subject, body)
        }
    }

    @Test
    fun `test cluster job scheduler check job with DONE`() {
        val clusterJob = entityFactory.getClusterJob()
        clusterJob.setState("RUN")
        clusterJob.remoteId = 123456789
        val host = "odcf-cn12u34s56"
        val json = "{\n" +
            "  \"COMMAND\":\"bjobs\",\n" +
            "  \"JOBS\":1,\n" +
            "  \"RECORDS\":[\n" +
            "    {\n" +
            "      \"JOBID\":\"${clusterJob.remoteId}\",\n" +
            "      \"STAT\":\"${DONE.lsfState}\",\n" +
            "      \"EXEC_HOST\":\"$host\",\n" +
            "      \"EXIT_CODE\":\"\",\n" +
            "      \"START_TIME\":\"Aug  2 14:18:45 2022\",\n" +
            "      \"FINISH_TIME\":\"Aug  2 14:18:51 2022 L\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n"
        val subject = mailBundle.getString("lsfService.finalProcessingStatusUpdateSubject")
        val body = mailBundle.getString("lsfService.finalProcessingStatusUpdateBody")

        `when`(clusterJobRepository.findAllByStateIn(setOf(RUNNING, SUBMITTED, UNKNOWN))).thenReturn(setOf(clusterJob))
        `when`(remoteCommandsService.getFromRemote(startsWith("bjobs"))).thenReturn(json)
        `when`(clusterJobRepository.findByRemoteId(clusterJob.remoteId)).thenReturn(clusterJob)
        `when`(mailContentGeneratorService.getTicketSubject(clusterJob.submission, "lsfService.finalProcessingStatusUpdateSubject")).thenReturn(subject)
        `when`(mailContentGeneratorService.getFinalProcessingStatusUpdateBody(any())).thenReturn(body)

        lsfCommandServiceMock.clusterJobScheduler()

        assertThat(clusterJob.hostName).isEqualTo(host)
        assertThat(clusterJob.state).isEqualTo(DONE)
        assertThat(clusterJob.exitCode).isEqualTo(0)
        verify(mailSenderService, times(1)).sendMailToAllSubmissionMembers(subject, body, clusterJob.submission)
    }

    @Test
    fun `test cluster job scheduler check jobs with DONE`() {
        val clusterJob = entityFactory.getClusterJob()
        clusterJob.setState("RUN")
        clusterJob.remoteId = 123456789
        val clusterJob2 = entityFactory.getClusterJob()
        clusterJob2.parentJob = clusterJob
        val host = "odcf-cn12u34s56"
        val json = "{\n" +
            "  \"COMMAND\":\"bjobs\",\n" +
            "  \"JOBS\":1,\n" +
            "  \"RECORDS\":[\n" +
            "    {\n" +
            "      \"JOBID\":\"${clusterJob.remoteId}\",\n" +
            "      \"STAT\":\"${DONE.lsfState}\",\n" +
            "      \"EXEC_HOST\":\"$host\",\n" +
            "      \"EXIT_CODE\":\"\",\n" +
            "      \"START_TIME\":\"Aug  2 14:18:45 2022\",\n" +
            "      \"FINISH_TIME\":\"Aug  2 14:18:51 2022 L\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n"

        `when`(clusterJobRepository.findAllByStateIn(setOf(RUNNING, SUBMITTED, UNKNOWN))).thenReturn(setOf(clusterJob))
        `when`(remoteCommandsService.getFromRemote(startsWith("bjobs"))).thenReturn(json)
        `when`(clusterJobRepository.findByRemoteId(clusterJob.remoteId)).thenReturn(clusterJob)
        `when`(clusterJobRepository.findAllBySubmissionAndParentJobIsNullAndRestartedJobIsNull(clusterJob.submission)).thenReturn(clusterJob)
        `when`(clusterJobRepository.findByParentJobAndRestartedJobIsNull(clusterJob)).thenReturn(clusterJob2)

        lsfCommandServiceMock.clusterJobScheduler()

        assertThat(clusterJob.hostName).isEqualTo(host)
        assertThat(clusterJob.state).isEqualTo(DONE)
        assertThat(clusterJob.exitCode).isEqualTo(0)
        assertThat(clusterJob2.state).isEqualTo(UNKNOWN)
        assertThat(clusterJob2.exitCode).isEqualTo(-1)
        verify(mailSenderService, times(1)).sendMailToTicketSystem(anyOrNull(), anyOrNull())
    }

    @Test
    fun `test cluster job scheduler running job`() {
        val clusterJob = entityFactory.getClusterJob()
        clusterJob.setState("RUN")
        clusterJob.remoteId = 123456789
        val host = "odcf-cn12u34s56"
        val json = "{\n" +
            "  \"COMMAND\":\"bjobs\",\n" +
            "  \"JOBS\":1,\n" +
            "  \"RECORDS\":[\n" +
            "    {\n" +
            "      \"JOBID\":\"${clusterJob.remoteId}\",\n" +
            "      \"STAT\":\"${RUNNING.lsfState}\",\n" +
            "      \"EXEC_HOST\":\"$host\",\n" +
            "      \"EXIT_CODE\":\"0\",\n" +
            "      \"START_TIME\":\"Aug  2 14:18:45 2022\",\n" +
            "      \"FINISH_TIME\":\"Aug  2 14:18:51 2022 L\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n"

        `when`(clusterJobRepository.findAllByStateIn(setOf(RUNNING, SUBMITTED, UNKNOWN))).thenReturn(setOf(clusterJob))
        `when`(remoteCommandsService.getFromRemote(startsWith("bjobs"))).thenReturn(json)
        `when`(clusterJobRepository.findByRemoteId(clusterJob.remoteId)).thenReturn(clusterJob)

        lsfCommandServiceMock.clusterJobScheduler()

        assertThat(clusterJob.hostName).isEqualTo(host)
        assertThat(clusterJob.state).isEqualTo(RUNNING)
        assertThat(clusterJob.exitCode).isEqualTo(0)
        verify(mailSenderService, times(0)).sendMailToTicketSystem(anyString(), anyString())
    }
}
