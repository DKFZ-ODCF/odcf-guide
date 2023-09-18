package de.dkfz.odcf.guide.service.interfaces.external

import de.dkfz.odcf.guide.entity.cluster.ClusterJob
import de.dkfz.odcf.guide.entity.cluster.ClusterJobTemplate
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.exceptions.JobAlreadySubmittedException
import de.dkfz.odcf.guide.exceptions.RuntimeOptionsNotFoundException

interface LSFCommandService {

    /**
     * Create a cluster job for a submission with the meta information from the given ClusterJobTemplate and additional parameters.
     * If autostart is enabled it will also submit the job.
     *
     * @param clusterJobTemplate meta information for the job
     * @param additionParams additional query parameters for the command
     * @param submission submission for which the job is run
     * @param recursiveCall `true` if this is a recursive call, no mails should be sent and no jobs started - default: `false`
     * @throws RuntimeOptionsNotFoundException if required options not found
     * @throws GuideRuntimeException if the job could not be submitted
     * @throws JobAlreadySubmittedException if a job was already submitted for the given submission
     * @return created cluster job
     */
    @Throws(RuntimeOptionsNotFoundException::class, GuideRuntimeException::class, JobAlreadySubmittedException::class)
    fun submitClusterJob(clusterJobTemplate: ClusterJobTemplate, additionParams: Map<String, String>, submission: Submission, recursiveCall: Boolean = false): ClusterJob

    /**
     * Create a cluster job for a submission with the meta information from the given ClusterJobTemplate and additional parameters.
     * If autostart is enabled it will also submit the job.
     *
     * @param submission submission for which the job is run
     * @param name name of the cluster job
     * @param groupName name of the cluster job group
     * @param command command to be executed
     * @param outputPath log output path
     * @param mem expected memory usage - default: `1024`
     * @param startJob if the job should be tried to be started - default: `true`
     * @throws GuideRuntimeException if the job could not be submitted
     * @return created cluster job
     */
    @Throws(GuideRuntimeException::class)
    fun submitClusterJob(
        submission: Submission,
        name: String,
        groupName: String,
        command: String,
        visibleForUser: Boolean = true,
        outputPath: String,
        mem: String = "1024",
        maximumRuntime: Int = 1440,
        estimatedRuntimePerSample: Int = 0,
        startJob: Boolean = true
    ): ClusterJob

    /**
     * Try to send the command of the cluster job on the submission host and sends a status update mail.
     * Will only be successful if autostart is enabled or force start is `true`.
     *
     * @param clusterJob cluster job containing all information
     * @param forceStart option to start the job anyway - default: `false`
     * @return updated cluster job
     */
    fun tryToRunJob(clusterJob: ClusterJob, forceStart: Boolean = false): ClusterJob

    /**
     * Collects all jobs for the given submission.
     * These jobs are ordered starting with the latest root parent job and its following jobs.
     *
     * @param submission submission for which to get the jobs
     * @return list of ordered jobs
     */
    fun collectJobs(submission: Submission): List<ClusterJob>
}
