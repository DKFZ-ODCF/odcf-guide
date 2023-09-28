package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.MissingPropertyException
import de.dkfz.odcf.guide.service.interfaces.PseudonymService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PseudonymServiceImpl(
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val sampleRepository: SampleRepository,
) : PseudonymService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    override fun checkSimilarPidsAndSendMail(submission: Submission) {
        val matchResults = getSimilarPids(submission)
        if (matchResults.isNotEmpty()) {
            try {
                sendMail(matchResults, submission)
            } catch (e: MissingPropertyException) {
                logger.error(e.stackTraceToString())
                mailSenderService.sendMailToTicketSystem(
                    subject = "Error while sending similar patients mail",
                    body = "For developer:\n\n${e.stackTraceToString()}"
                )
            }
        }
    }

    override fun getSimilarPids(submission: Submission): Set<Map<String, String>> {
        return sampleRepository.findAllBySubmission(submission).flatMap { getSimilarPids(it) }.toSet()
    }

    override fun getSimilarPids(sample: Sample): Set<Map<String, String>> {
        val pseudonym = sample.pid.removePrefix(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to sample.project)))
        return if (pseudonym.isBlank() || sample.seqType == null) {
            emptySet()
        } else {
            setOf(checkAgainstGuide(pseudonym, sample), checkAgainstOtp(pseudonym, sample)).flatten().toSet()
        }
    }

    /**
     * Checks against the GUIDE DB whether samples from different projects exist that have the same seqType and a similar PID.
     *
     * @param pseudonym PID of the sample with its prefix removed
     * @param sample Sample containing the PID to be tested
     * @return The information about the samples for which the criteria apply
     */
    private fun checkAgainstGuide(pseudonym: String, sample: Sample): Set<Map<String, String>> {
        val samples = sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(pseudonym, sample.project, sample.seqType!!.name).minus(sample)
        return samples.map { innerSample ->
            mapOf(
                "checked_pid" to sample.pid,
                "pid" to innerSample.pid,
                "target_entity" to innerSample.submission.identifier,
                "seq_type" to (innerSample.seqType?.name ?: "no seqType given"),
                "sample_type" to (innerSample.sampleType.takeIf { it.isNotBlank() } ?: "no sampleType given"),
                "scope" to "GUIDE",
            )
        }.toSet()
    }

    /**
     * Checks against the OTP DB whether samples from different projects exist that have the same seqType and a similar PID.
     *
     * @param pseudonym PID of the sample with its prefix removed
     * @param sample Sample containing the PID to be tested
     * @return The information about the samples for which the criteria apply
     */
    private fun checkAgainstOtp(pseudonym: String, sample: Sample): Set<Map<String, String>> {
        return externalMetadataSourceService.getSetOfMapOfValues(
            "pidsByPseudonym",
            mapOf(
                "pseudonym" to pseudonym,
                "project" to sample.project,
                "seqType" to sample.seqType!!.name
            )
        ).filter { it.isNotEmpty() }.map { it + ("scope" to "OTP") + ("checked_pid" to sample.pid) }.toSet()
    }

    @Throws(MissingPropertyException::class)
    override fun sendMail(matchResults: Set<Map<String, String>>, submission: Submission) {
        val subject = mailBundle.getString("pseudonymService.subject")
            .replace("{0}", mailContentGeneratorService.getTicketSubjectPrefix(submission))
        val result = matchResults.groupBy { it["checked_pid"] }.map { groupItem ->
            "'${groupItem.key}' has following similar PIDs:\n" +
                groupItem.value.joinToString(",\n") {
                    val scope = it["scope"]?.lowercase() ?: throw MissingPropertyException("scope")
                    "&nbsp;&nbsp;&nbsp;- " + mailBundle.getString("pseudonymService.$scope")
                        .replace("{0}", it["pid"] ?: throw MissingPropertyException("pid"))
                        .replace("{1}", it["target_entity"] ?: throw MissingPropertyException("target_entity"))
                        .replace("{2}", it["sample_type"] ?: throw MissingPropertyException("sample_type"))
                        .replace("{3}", it["seq_type"] ?: throw MissingPropertyException("seq_type"))
                }
        }.joinToString("\n") { "\n$it" }
        val body = mailBundle.getString("pseudonymService.body")
            .replace("{0}", submission.identifier)
            .replace("{1}", result)
        mailSenderService.sendMailToTicketSystem(subject, body)
    }
}
