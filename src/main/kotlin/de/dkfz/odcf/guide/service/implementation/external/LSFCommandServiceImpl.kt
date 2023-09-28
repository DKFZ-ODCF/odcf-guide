package de.dkfz.odcf.guide.service.implementation.external

import com.fasterxml.jackson.databind.ObjectMapper
import de.dkfz.odcf.guide.ClusterJobRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.cluster.ClusterJob.State.*
import de.dkfz.odcf.guide.entity.cluster.ClusterJobTemplate
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.exceptions.JobAlreadySubmittedException
import de.dkfz.odcf.guide.exceptions.RuntimeOptionsNotFoundException
import de.dkfz.odcf.guide.service.interfaces.external.LSFCommandService
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Service
open class LSFCommandServiceImpl(
    private val clusterJobRepository: ClusterJobRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val remoteCommandsService: RemoteCommandsService,
    private val sampleRepository: SampleRepository,
) : LSFCommandService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Throws(RuntimeOptionsNotFoundException::class, GuideRuntimeException::class, JobAlreadySubmittedException::class)
    override fun submitClusterJob(clusterJobTemplate: ClusterJobTemplate, additionParams: Map<String, String>, submission: Submission, recursiveCall: Boolean): ClusterJob {
        if (clusterJobRepository.findAllBySubmissionAndJobNameStartsWith(submission, clusterJobTemplate.name).isNotEmpty()) {
            throw JobAlreadySubmittedException("Job for submission ${submission.identifier} already submitted")
        }
        val ilse = String.format("%06d", submission.identifier.filter { it.isDigit() }.toInt())
        val ilsePrefix = ilse.substring(0, 3)
        val command = "${clusterJobTemplate.command} ${additionParams.map { "${it.key} ${it.value}" }.joinToString(" ")}"
        val basePath = runtimeOptionsRepository.findByName("tsvBasePath")?.value ?: throw RuntimeOptionsNotFoundException("tsvBasePath")
        val subPath = runtimeOptionsRepository.findByName("tsvInternalSubpath")?.value ?: throw RuntimeOptionsNotFoundException("tsvInternalSubpath")
        val outputPath = basePath + subPath.replace("<ILSE_PREFIX>", ilsePrefix)
            .replace("<ILSE_ID>", ilse)
            .replace("<SUBMISSION_ID>", submission.identifier)
            .replace("meta.guide.tsv", "job_log")
        val clusterJob = submitClusterJob(
            submission = submission,
            name = "${clusterJobTemplate.name}_${submission.identifier}",
            groupName = clusterJobTemplate.group,
            maximumRuntime = clusterJobTemplate.maximumRuntime,
            estimatedRuntimePerSample = clusterJobTemplate.estimatedRuntimePerSample,
            visibleForUser = clusterJobTemplate.clusterJobVisibleForUser,
            command = command,
            outputPath = outputPath,
            startJob = false
        )
        if (clusterJobTemplate.subsequentJobTemplate != null) {
            val subsequentJob = submitClusterJob(clusterJobTemplate.subsequentJobTemplate!!, additionParams, submission, recursiveCall = true)
            subsequentJob.parentJob = clusterJob
            clusterJobRepository.save(subsequentJob)
        }
        if (!recursiveCall) {
            val subject = mailContentGeneratorService.getTicketSubject(clusterJob.submission, "lsfService.processingStatusUpdateSubject")
            mailSenderService.sendMailToTicketSystem(subject, mailContentGeneratorService.getProcessingStatusUpdateBody(collectJobs(submission)))
            clusterJobScheduler()
        }
        return clusterJob
    }

    @Throws(GuideRuntimeException::class)
    override fun submitClusterJob(
        submission: Submission,
        name: String,
        groupName: String,
        command: String,
        visibleForUser: Boolean,
        outputPath: String,
        mem: String,
        maximumRuntime: Int,
        estimatedRuntimePerSample: Int,
        startJob: Boolean
    ): ClusterJob {
        val jobGroup = "/odcf-guide/$groupName"
        if (remoteCommandsService.getFromRemote("bjgroup -s $jobGroup").contains("No job group found")) {
            logger.info(remoteCommandsService.getFromRemote("""bgadd -L "10" "$jobGroup""""))
        }

        val lsfCommand = """bsub -W $maximumRuntime -R rusage[mem=$mem]
            #${" -We ${sampleRepository.countAllBySubmission(submission) * estimatedRuntimePerSample}".takeIf { estimatedRuntimePerSample > 0 }.orEmpty()}
            # -J "$name"
            # -g "$jobGroup"
            # -o ${outputPath}_$name.out
            # -e ${outputPath}_$name.err
            # "$command"
            """.trimMargin("#").replace("\n", "")

        val clusterJob = ClusterJob(
            submission = submission,
            jobName = name,
            command = lsfCommand,
            state = PENDING,
            pathToLog = outputPath,
            visibleForUser = visibleForUser
        )
        clusterJobRepository.save(clusterJob)
        if (startJob) {
            val subject = mailContentGeneratorService.getTicketSubject(clusterJob.submission, "lsfService.processingStatusUpdateSubject")
            mailSenderService.sendMailToTicketSystem(subject, mailContentGeneratorService.getProcessingStatusUpdateBody(collectJobs(submission)))
            clusterJobScheduler()
        }
        return clusterJob
    }

    /**
     * Schedule job to start pending cluster jobs and check for status updates on running jobs.
     * It will run every 5 minutes and send mails on updates.
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    @Transactional(isolation = Isolation.SERIALIZABLE)
    open fun clusterJobScheduler() {
        clusterJobRepository.findAllByStateIn(setOf(PENDING)).forEach { tryToRunJob(it) }

        val bjobsProperties = listOf("id", "stat", "exec_host", "exit_code", "start_time", "finish_time")
        val bjobs = "bjobs -o \"${bjobsProperties.joinToString(": ")}\" -json "
        val activeJobs = clusterJobRepository.findAllByStateIn(setOf(RUNNING, SUBMITTED, UNKNOWN))
        if (activeJobs.isNotEmpty()) {
            val json = remoteCommandsService.getFromRemote(bjobs + activeJobs.map { it.remoteId }.joinToString(" "))
            val mapper = ObjectMapper()
            val map = mapper.readValue(json, MutableMap::class.java)
            val jobList = map["RECORDS"] as List<Map<String, String>>
            jobList.forEach {
                val job = clusterJobRepository.findByRemoteId(it["JOBID"]!!.toInt())
                val lsfState = if (it["ERROR"] != null) {
                    ""
                } else {
                    job.exitCode = (it["EXIT_CODE"]!!.takeIf { it.isNotBlank() } ?: "-1").toInt()
                    job.hostName = it["EXEC_HOST"]!!
                    job.startTime = SimpleDateFormat("MMM dd HH:mm:ss yyyy", Locale.US).parse(it["START_TIME"]!!)
                    job.endTime = SimpleDateFormat("MMM dd HH:mm:ss yyyy", Locale.US).parse(it["FINISH_TIME"]!!)
                    it["STAT"]!!
                }
                val jobState = ClusterJob.State.findByLSFState(lsfState)
                if (jobState != job.state) {
                    job.setState(lsfState)
                    clusterJobRepository.save(job)
                    val orderedJobList = collectJobs(job.submission)
                    when (jobState) {
                        ClusterJob.State.FAILED -> {
                            val subject = mailContentGeneratorService.getTicketSubject(job.submission, "lsfService.failedUpdateSubject")
                            mailSenderService.sendMailToTicketSystem(subject, mailContentGeneratorService.getProcessingStatusUpdateBody(orderedJobList))
                        }
                        ClusterJob.State.DONE -> {
                            job.exitCode = 0 // LSF do not set exit code to 0 if job is done
                            if (orderedJobList.all { it.state == ClusterJob.State.DONE }) {
                                val subject = mailContentGeneratorService.getTicketSubject(job.submission, "lsfService.finalProcessingStatusUpdateSubject")
                                mailSenderService.sendMailToAllSubmissionMembers(subject, mailContentGeneratorService.getFinalProcessingStatusUpdateBody(job), job.submission)
                            } else {
                                val subject = mailContentGeneratorService.getTicketSubject(job.submission, "lsfService.processingStatusUpdateSubject")
                                mailSenderService.sendMailToTicketSystem(subject, mailContentGeneratorService.getProcessingStatusUpdateBody(orderedJobList))
                                val subsequentJob = clusterJobRepository.findByParentJobAndRestartedJobIsNull(job)
                                if (subsequentJob != null) tryToRunJob(subsequentJob)
                            }
                        }
                        ClusterJob.State.UNKNOWN -> {
                            val subject = mailContentGeneratorService.getTicketSubject(job.submission, "lsfService.processingStatusUpdateSubject")
                            mailSenderService.sendMailToTicketSystem(subject, mailContentGeneratorService.getProcessingStatusUpdateBody(orderedJobList))
                        }
                        else -> {} // do nothing
                    }
                    clusterJobRepository.save(job)
                }
            }
        }
    }

    override fun tryToRunJob(clusterJob: ClusterJob, forceStart: Boolean): ClusterJob {
        val autoStart = runtimeOptionsRepository.findByName("autoStartJobs")?.value.toBoolean()
        if ((autoStart || forceStart) && clusterJob.startable) {
            try {
                val submitResult = remoteCommandsService.getFromRemote(clusterJob.command)
                logger.info(submitResult)
                clusterJob.remoteId = submitResult.filter { it.isDigit() }.toInt()
                clusterJob.setState("PEND")
                clusterJobRepository.saveAndFlush(clusterJob)
                val subject = mailContentGeneratorService.getTicketSubject(clusterJob.submission, "lsfService.processingStatusUpdateSubject")
                mailSenderService.sendMailToTicketSystem(subject, mailContentGeneratorService.getProcessingStatusUpdateBody(collectJobs(clusterJob.submission)))
            } catch (e: IOException) {
                throw GuideRuntimeException("could not execute:\n${clusterJob.command}\n\n${e.stackTrace}")
            }
        }
        clusterJobRepository.save(clusterJob)
        return clusterJob
    }

    override fun collectJobs(submission: Submission): List<ClusterJob> {
        val rootJob = clusterJobRepository.findAllBySubmissionAndParentJobIsNullAndRestartedJobIsNull(submission) ?: return emptyList()
        val clusterJobs = mutableListOf(rootJob)
        var subsequentJob = clusterJobRepository.findByParentJobAndRestartedJobIsNull(rootJob)
        while (subsequentJob != null) {
            clusterJobs.add(subsequentJob)
            subsequentJob = clusterJobRepository.findByParentJobAndRestartedJobIsNull(subsequentJob)
        }
        return clusterJobs
    }
}
