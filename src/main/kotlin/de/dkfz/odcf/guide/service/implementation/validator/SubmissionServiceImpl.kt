package de.dkfz.odcf.guide.service.implementation.validator

import de.dkfz.odcf.guide.ApiSubmissionRepository
import de.dkfz.odcf.guide.ClusterJobRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.exceptions.JobAlreadySubmittedException
import de.dkfz.odcf.guide.helperObjects.toBool
import de.dkfz.odcf.guide.service.interfaces.MergingService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.LSFCommandService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

@Service
open class SubmissionServiceImpl(
    private val submissionRepository: SubmissionRepository,
    private val apiSubmissionRepository: ApiSubmissionRepository,
    private val clusterJobRepository: ClusterJobRepository,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val lsfCommandService: LSFCommandService,
    private val mergingService: MergingService,
    private val env: Environment
) : SubmissionService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun changeSubmissionState(submission: Submission, status: Submission.Status, username: String?, logComment: String?, stateComment: String?) {
        val processedUsername = username ?: "automatic"
        logger.info(
            listOfNotNull(
                "Submission [${submission.identifier}] has been changed from [${submission.status.name}] to [${status.name}] by [$processedUsername]",
                "Comment: [$logComment]".takeIf { !logComment.isNullOrBlank() }
            ).joinToString("\n")
        )
        submission.status = status
        when (status) {
            Submission.Status.RESET -> {
                submission.lockDate = null
                submission.lockUser = null
                submission.closedDate = null
                submission.closedUser = null
                submission.removalDate = null
            }
            Submission.Status.LOCKED -> {
                submission.lockDate = Date()
                submission.lockUser = processedUsername
            }
            Submission.Status.ON_HOLD -> {
                submission.onHoldComment = stateComment ?: "no comment"
            }
            Submission.Status.CLOSED -> {
                submission.closedDate = Date()
                submission.closedUser = processedUsername
            }
            Submission.Status.REMOVED_BY_ADMIN -> {
                submission.removalDate = Date()
                submission.removalUser = processedUsername
            }
            Submission.Status.EXPORTED -> submission.exportDate = Date()
            Submission.Status.TERMINATED -> submission.terminateDate = Date()
            Submission.Status.FINISHED_EXTERNALLY -> submission.finishedExternallyDate = Date()
            else -> logger.debug("state '{}' has no further processing.", status)
        }
        submissionRepository.saveAndFlush(submission)
    }

    override fun finishSubmissionExternally(submission: Submission) {
        changeSubmissionState(submission, Submission.Status.FINISHED_EXTERNALLY, null)
    }

    override fun setExternalDataAvailableForMerging(submission: Submission, available: Boolean, date: Date?) {
        logger.info("GPCF Data availability has been set to " + available.toString() + " for Submission [${submission.identifier}].")
        submission.externalDataAvailableForMerging = available
        submission.externalDataAvailabilityDate = date ?: Date()
        submission.startTerminationPeriod = date ?: Date()
        submissionRepository.saveAndFlush(submission)
    }

    override fun postProceedWithSubmission(submission: Submission) {
        try {
            if (submission.ownTransfer) {
                submission as ApiSubmission
                val job = lsfCommandService.submitClusterJob(submission.sequencingTechnology.clusterJobTemplate!!, mapOf("-i" to "${submission.identifier.filter { it.isDigit() }.toInt()}"), submission)
                job.submission = submission
                clusterJobRepository.save(job)
            } else {
                mergingService.doMerging(submission)
            }
        } catch (e: JobAlreadySubmittedException) {
            logger.info(e.message)
        } catch (e: GuideRuntimeException) {
            logger.warn(e.message)
        }
    }

    @Scheduled(fixedDelay = 1 * 60 * 1000)
    open fun setUnlockState() {
        val timeout = env.getProperty("application.timeout", "${MetaValController.LOCKED_TIMEOUT_IN_MIN}").toInt()
        submissionRepository.findAllByStatus(Submission.Status.LOCKED).forEach {
            val diffTime = (Date().time - it.lockDate!!.time) / (60 * 1000)
            if (diffTime > timeout) {
                changeSubmissionState(it, Submission.Status.UNLOCKED, stateComment = "lock timeout")
            }
        }
    }

    @Scheduled(fixedDelay = 1 * 60 * 60 * 1000)
    open fun setCheckIfSubmissionIsImportedExternal() {
        apiSubmissionRepository.findAllByStatusInAndImportedExternalIsFalse(Submission.Status.values().filter { it.group == "finished" }).forEach {
            val ilse = it.identifier.filter { char -> char.isDigit() }.toInt().toString()
            val imported = externalMetadataSourceService.getSingleValue("checkIlseNumber", mapOf("ilse" to ilse)).toBool()
            it.importedExternal = imported
            submissionRepository.saveAndFlush(it)
        }
    }
}
