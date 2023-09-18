package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.GuideMergerException
import de.dkfz.odcf.guide.exceptions.JsonExtractorException
import de.dkfz.odcf.guide.exceptions.SubmissionNotFinishedException
import de.dkfz.odcf.guide.service.interfaces.MergingService
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class MergingServiceImpl(
    private val rcs: RemoteCommandsService,
    private val mailSenderService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val ldapService: LdapService,
    private val collectorService: CollectorService,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val env: Environment
) : MergingService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Extracts the submission number from the ILSe identifier string.
     *
     * @param identifier style "i0123456"
     */
    @Throws(IllegalArgumentException::class)
    private fun getILSeFromIdentifier(identifier: String): Int {
        if (identifier.startsWith("i")) {
            return Integer.parseInt(identifier.substring(1))
        }
        throw IllegalArgumentException("Identifier does not start with 'i'")
    }

    @Throws(JsonExtractorException::class, IllegalArgumentException::class)
    override fun runJsonExtractorScript(submission: Submission): String {
        if (!env.getProperty("application.mergingService.doMerging", "false").toBoolean()) {
            val message = "Did not trigger json extraction for submission [${submission.identifier}] since merging has been turned off."
            logger.info(message)
            return message
        }
        val jsonExtractorScript = runtimeOptionsRepository.findByName("jsonExtractorScript")?.value
            ?: throw JsonExtractorException("Path to json extractor script not found.")

        val ilse = getILSeFromIdentifier(submission.identifier)
        val ilseLeadingZero = String.format("%06d", ilse)
        val metadataFile = runtimeOptionsRepository.findByName("metadataFilePath")?.value
            ?.replace("{1}", ilseLeadingZero)
            ?.replace("{2}", ilse.toString())
            ?: throw JsonExtractorException("No metadata file path found.")
        val command = "$jsonExtractorScript $metadataFile"

        val response: String
        try {
            response = rcs.getFromRemote(command)
        } catch (e: IOException) {
            throw JsonExtractorException(e.message.orEmpty())
        }

        val regex = """ERROR occured during execution of metadata json extractor script""".toRegex()
        val errorOccurred = regex.containsMatchIn(response)
        if (errorOccurred) {
            throw JsonExtractorException(response)
        }

        return response.trim()
    }

    /**
     * Runs the guide joiner script for submissions to merge the metadata from the GUIDE with the technical data from midterm.
     *
     * @param submission Submission for which the metadata should be merged.
     * @param jsonExtractFile JSON content of the technical data file extracted from midterm.
     * @throws GuideMergerException if the path to the guide joiner script can't be found or
     *                              if something goes wrong during the merging.
     * @return Text for the mail to be sent to the ticket system with a link for the manual import into OTP.
     */
    private fun runGuideMergeScript(submission: Submission, jsonExtractFile: String): String {
        val guideJoinerScript = runtimeOptionsRepository.findByName("guideJoinerScript")?.value
            ?: throw GuideMergerException("Path to guide joiner script not found.")
        val otpRoot = runtimeOptionsRepository.findByName("otpRoot")?.value.orEmpty()
        val command = "$guideJoinerScript $jsonExtractFile"
        var responseBody = ""
        var paths = ""
        val formattedId = collectorService.getFormattedIdentifier(submission.identifier)

        val response: String
        try {
            response = rcs.getFromRemote(command)
        } catch (e: IOException) {
            throw GuideMergerException(e.message.orEmpty())
        }
        val regexRscriptExitCode = """RscriptExitCode:(\d+)""".toRegex()
        val regexErrorOccurred = "ERROR occured while executing GuideJoiner script".toRegex()
        val errorOccurred = regexRscriptExitCode.containsMatchIn(response) || regexErrorOccurred.containsMatchIn(response)
        if (errorOccurred) {
            responseBody += "\nAutomated merging for submission with ILSe ID [$formattedId] has been tried, " +
                "but the following error occurred:\n\n" +
                "################## ERROR REPORT ##################\n" +
                response +
                "############## END OF ERROR REPORT ###############\n" +
                "\n\nPlease fix the reported issues and retrigger merging manually or run the following command:\n\n" +
                command + "\n\n"

            throw GuideMergerException(responseBody)
        } else {

            val regex = """Output written to file:(.+)""".toRegex()
            val matchResult = regex.find(response)
            val resultFile = matchResult!!.destructured.component1()
            paths += "&paths=${resultFile.trim()}"

            responseBody += "Automated merging of GPCF and GUIDE table was performed successfully for submission with ILSe ID [$formattedId]." +
                "\n\nPlease click here for manual import:\n\n" +
                otpRoot + "metadataImport?ticketNumber=" + submission.ticketNumber +
                paths + "&directoryStructure=ABSOLUTE_PATH\n\n\n\n" +
                "################## MERGING REPORT ##################\n" +
                response +
                "############## END OF MERGING REPORT ###############\n"
        }

        return responseBody
    }

    @Throws(
        JsonExtractorException::class,
        GuideMergerException::class,
        SubmissionNotFinishedException::class
    )
    override fun doMerging(submission: Submission, onlyRunJsonExtractor: Boolean) {
        if (!env.getProperty("application.mergingService.doMerging", "false").toBoolean()) {
            logger.info("Did not trigger merging for submission [${submission.identifier}] since merging has been turned off.")
            return
        }
        if (!submission.isFinished && !onlyRunJsonExtractor) {
            throw SubmissionNotFinishedException("Submission is not finished! Merging not possible!")
        }

        var subject: String = mailContentGeneratorService.getTicketSubjectPrefix(submission) + " Successfully ran " + if (onlyRunJsonExtractor) "json extraction" else "merging" + " for submission " + submission.identifier

        // JSON EXTRACTION
        var scriptResponse: String
        try {
            scriptResponse = runJsonExtractorScript(submission)
        } catch (e: JsonExtractorException) {
            subject = mailContentGeneratorService.getTicketSubjectPrefix(submission) + " Could not run JsonExtract script for submission " + submission.identifier
            mailSenderService.sendMailToTicketSystem(subject, e.message!!)
            throw e
        } catch (ie: IllegalArgumentException) {
            subject = mailContentGeneratorService.getTicketSubjectPrefix(submission) + " Could not run JsonExtract script for submission " + submission.identifier
            mailSenderService.sendMailToTicketSystem(subject, ie.message!! + " Cannot run json extraction on non-GPCF submission.")
            throw IllegalArgumentException(ie.message)
        }

        if (!onlyRunJsonExtractor) {
            // GUIDE TABLE MERGING
            try {
                scriptResponse = runGuideMergeScript(submission, scriptResponse)
            } catch (e: GuideMergerException) {
                subject =
                    mailContentGeneratorService.getTicketSubjectPrefix(submission) + " Could not run GuideMerging script for submission " + submission.identifier
                var body = e.message!!
                when {
                    e.stackTrace!!.contentToString().contains("ChangesController.retriggerMerging") -> {
                        body += "\nMerging approach was re-triggered by ${ldapService.getPerson().fullName}\n"
                    }
                    e.stackTrace!!.contentToString().contains("ChangesController.showFinalSubmissionPage") -> {
                        body += "\nMerging approach was triggered by user ${ldapService.getPerson().fullName} (${ldapService.getPerson().username}; ${ldapService.getPerson().mail}) by finally releasing the submission.\n"
                    }
                    e.stackTrace!!.contentToString().contains("IlseController.setIlseDataIsAvailable") -> {
                        body += "\nMerging approach was triggered by receiving metadata from GPCF after user finished the submission.\n"
                    }
                }
                mailSenderService.sendMailToTicketSystem(subject, body)
                throw e
            }
        }
        mailSenderService.sendMailToTicketSystem(subject, scriptResponse)
    }
}
