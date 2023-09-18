package de.dkfz.odcf.guide.service.implementation.validator

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
open class DeletionServiceImpl(
    private val sampleRepository: SampleRepository,
    private val submissionRepository: SubmissionRepository,
    private val fileRepository: FileRepository,
    private val seqTypeRepository: SeqTypeRepository,
    private val seqTypeRequestedValuesRepository: SeqTypeRequestedValuesRepository,
    private val fieldRequestedValuesRepository: FieldRequestedValuesRepository,
    private val submissionService: SubmissionService,
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val collectorService: CollectorService,
) : DeletionService {

    override fun deleteSubmission(submission: Submission, sendMail: Boolean): Boolean {
        deleteSamples(submission)
        deleteSubmissionFromRequestedValues(submission)

        if (sendMail) {
            val subject = mailContentGeneratorService.getTicketSubject(submission, "deletionService.submission.subject")
            val body = mailContentGeneratorService.getMailBody(
                "deletionService.submission.body",
                mapOf(
                    "{0}" to collectorService.getFormattedIdentifier(submission.identifier)
                )
            )
            mailSenderService.sendMailToTicketSystem(subject, body)
        }

        submissionRepository.delete(submission)
        return !submissionRepository.existsById(submission.identifier)
    }

    override fun deleteSamples(submission: Submission): Boolean {
        return deleteSamples(sampleRepository.findBySubmission(submission))
    }

    override fun deleteSamples(samples: List<Sample>): Boolean {
        return samples.map { deleteSample(it) }.any { !it }
    }

    override fun deleteSample(sample: Sample): Boolean {
        fileRepository.deleteAll(fileRepository.findAllBySample(sample))
        sampleRepository.delete(sample)
        return !sampleRepository.existsById(sample.id)
    }

    @Transactional(rollbackFor = [Exception::class])
    @Throws(DataIntegrityViolationException::class)
    override fun deleteSeqType(seqType: SeqType): Boolean {
        val requestedSeqTypes = seqTypeRequestedValuesRepository.findAllByRequestedSeqType(seqType)
        if (requestedSeqTypes.isNotEmpty()) {
            val validatedSubmissions = sampleRepository.findAllBySeqTypeAndSubmission_StatusIn(seqType, listOf(Submission.Status.VALIDATED)).map { it.submission }
            validatedSubmissions.forEach { submissionService.changeSubmissionState(it, Submission.Status.UNLOCKED) }
            submissionRepository.saveAll(validatedSubmissions)
        }

        val samples = sampleRepository.findAllBySeqTypeAndSubmission_StatusIn(seqType, Submission.Status.filterBySampleIsCorrectable()).toMutableList()
        val samplesWithDeletedSeqType = mutableSetOf<Sample>()
        samples.forEach { sample ->
            sample.seqType = null
            samplesWithDeletedSeqType.add(sample)
        }
        sampleRepository.saveAll(samplesWithDeletedSeqType)
        requestedSeqTypes.forEach {
            it.requestedSeqType = null
            it.state = RequestedValue.State.REJECTED
        }
        seqTypeRequestedValuesRepository.saveAll(requestedSeqTypes)
        seqTypeRepository.delete(seqType)
        return !seqTypeRepository.existsById(seqType.id)
    }

    override fun deleteSubmissionFromRequestedValues(submission: Submission): Boolean {
        val requestedValues = fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)
        val dummySubmission = submissionRepository.findByIdentifier("o0000000")!!

        requestedValues.forEach {
            if (it.usedSubmissions.size > 1) {
                it.usedSubmissions.remove(submission)
                if (it.originSubmission == submission) {
                    it.originSubmission = dummySubmission
                }
                fieldRequestedValuesRepository.save(it)
            } else {
                fieldRequestedValuesRepository.delete(it)
            }
        }
        return fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission).isEmpty()
    }
}
