package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.exceptions.UserNotFoundException
import de.dkfz.odcf.guide.service.interfaces.*
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.LSFCommandService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.SampleService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import java.security.InvalidParameterException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@Controller
class AjaxController(
    private val submissionRepository: SubmissionRepository,
    private val personRepository: PersonRepository,
    private val parserRepository: ParserRepository,
    private val clusterJobRepository: ClusterJobRepository,
    private val fieldRequestedValuesRepository: FieldRequestedValuesRepository,
    private val seqTypeRepository: SeqTypeRepository,
    private val mailService: MailSenderService,
    private val feedbackService: FeedbackService,
    private val lsfCommandService: LSFCommandService,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val ldapService: LdapService,
    private val mergingService: MergingService,
    private val speciesService: SpeciesService,
    private val sampleService: SampleService,
    private val env: Environment
) {

    private val VALUE_ALREADY_EXISTS = "This value already exists!"

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/refresh-timer-in-db")
    @ResponseBody
    fun refreshLockDateInDB(@RequestParam("UUID") uuid: String): String {
        val submission = submissionRepository.findByUuid(UUID.fromString(uuid))
        submission!!.lockDate = Date()
        submissionRepository.save(submission)
        return "Successfully refreshed timer in DB"
    }

    @GetMapping("/get-content-for-project")
    @ResponseBody
    @Throws(InvalidParameterException::class)
    fun findProjectDependentContent(
        @RequestParam projectName: String,
        @RequestParam requestedField: String
    ): Set<String> {
        when (requestedField) {
            "sample-types" -> return externalMetadataSourceService.getValuesAsSet("sampleTypesByProject", mapOf("project" to projectName))
            "seq-types" -> return externalMetadataSourceService.getValuesAsSet("seqTypesByProject", mapOf("project" to projectName))
            "project-prefix" -> return setOf(externalMetadataSourceService.getSingleValue("projectPrefixByProject", mapOf("project" to projectName)))
        }
        throw InvalidParameterException("Parameter 'requestedField' must be either sampletypes or seqtypes")
    }

    @GetMapping("/get-parser-availability-for-project")
    @ResponseBody
    fun checkParserAvailable(
        @RequestParam("projectName") projectName: String,
    ): Boolean {
        return parserRepository.findByProject(projectName) != null
    }

    @GetMapping("/get-seq-type-data")
    @ResponseBody
    fun getSeqTypeData(
        @RequestParam("seqTypeId") seqTypeId: Int,
    ): SeqType? {
        return seqTypeRepository.findById(seqTypeId).orElse(null)
    }

    @GetMapping("/get-species-otp-validity")
    @ResponseBody
    fun getSpeciesOtpValidity(
        @RequestParam pid: String,
        @RequestParam sampleType: String,
        @RequestParam project: String,
        @RequestParam species: Set<String>,
    ): Boolean {
        if (pid.isBlank() || sampleType.isBlank() || project.isBlank() || species.any { it.endsWith("(ReqVal)") }) {
            return true
        }
        val speciesFromPidOtp = externalMetadataSourceService.getSingleValue("speciesFromPid", mapOf("pid" to pid))
        val speciesListFromSampleOtp = externalMetadataSourceService.getValuesAsSet(
            "speciesListFromSample", mapOf("pid" to pid, "sampleType" to sampleType)
        ).plus(speciesFromPidOtp).filter { it.isNotBlank() }.toSet()
        val speciesOfProject = externalMetadataSourceService.getSingleValue("speciesForProject", mapOf("project" to project))
        return (speciesFromPidOtp.isBlank() || speciesFromPidOtp == species.first()) &&
            speciesService.compareSets(speciesListFromSampleOtp, species) &&
            speciesOfProject.contains(species.first())
    }

    @GetMapping("/new-species-similarity-check")
    @ResponseBody
    fun newSpeciesSimilarityCheck(@RequestParam newSpecies: String): Map<String, String> {
        val speciesFromOtp = externalMetadataSourceService.getValuesAsSetMap("speciesInfos").mapNotNull { it["species_with_strain"] }.toSet()
            .plus(fieldRequestedValuesRepository.findByFieldName("speciesWithStrain").filter { !it.isFinished }.map { it.requestedValue + " (Approval pending)" }.toSet())
        return checkSimilarity(speciesFromOtp, newSpecies.removeSuffix("(ReqVal)"), true)
    }

    @GetMapping("/new-seq-type-similarity-check")
    @ResponseBody
    fun newSeqTypeSimilarityCheck(@RequestParam newSeqType: String): Map<String, String> {
        val alreadyExistingSeqTypes = seqTypeRepository.findAll().map { it.name }.toSet()
        return checkSimilarity(alreadyExistingSeqTypes, newSeqType.removeSuffix("(ReqVal)"), true)
    }

    @GetMapping("/new-value-similarity-check")
    @ResponseBody
    fun newValuesSimilarityCheck(
        @RequestParam newValue: String,
        @RequestParam fieldName: String
    ): Map<String, String> {
        val lookupName = if (fieldName == "pipelineVersion") "pipelines" else fieldName + "s"

        val alreadyExistingValues = externalMetadataSourceService.getValuesAsSet(lookupName)
            .map { if (fieldName == "instrumentModelWithSequencingKit") it.replace("[]", "").trim() else it }.toSet()
            .plus(fieldRequestedValuesRepository.findByFieldName(fieldName).filter { !it.isFinished }.map { it.requestedValue + " (Approval pending)" }.toSet())

        return checkSimilarity(alreadyExistingValues, newValue.removeSuffix("(ReqVal)"), true)
    }

    private fun checkSimilarity(
        originalValues: Set<String>,
        newValue: String,
        checkPartialMatches: Boolean = false,
        regex: Regex = Regex("[^a-z0-9]")
    ): Map<String, String> {
        if (originalValues.contains(newValue)) {
            return mapOf(
                "status" to "1",
                "response" to VALUE_ALREADY_EXISTS
            )
        }

        // filters out all extra characters like parentheses to compare the raw Strings
        val alphanumericNewValue = regex.replace(newValue.lowercase(), "")
        val similarValues = originalValues.filter {
            val alphanumericOriginalValue = regex.replace(it.lowercase(), "")
            val originalSpeciesName = it.lowercase().split("(")[0].trim()
            alphanumericOriginalValue == alphanumericNewValue ||
                (checkPartialMatches && (alphanumericOriginalValue.contains(alphanumericNewValue) || alphanumericNewValue.contains(originalSpeciesName)))
        }

        if (similarValues.isNotEmpty()) {
            val responseText = bundle.getString("ajaxController.registerNewEntity.similarValues")
                .replace("{0}", similarValues.joinToString("") { "<li>$it</li>" })
                .replace("{1}", newValue)
            return mapOf(
                "status" to "2",
                "response" to responseText
            )
        }
        return mapOf("status" to "0")
    }

    @PostMapping("/send-mail")
    @ResponseBody
    fun sendMail(
        @RequestParam name: String,
        @RequestParam mail: String,
        @RequestParam messageText: String,
        @RequestParam(name = "submission", required = false) submissionId: String?
    ): String {

        val sender = "$name<$mail>"
        var subject = "Question from GUIDE"

        if (submissionId != null) {
            subject += " to submission $submissionId"
        }
        mailService.sendMail(sender, env.getRequiredProperty("application.mails.ticketSystemAddress"), "", subject, messageText)

        return "Successfully sent mail."
    }

    @PostMapping("/send-feedback")
    @ResponseBody
    fun sendFeedback(
        @RequestParam name: String,
        @RequestParam mail: String,
        @RequestParam message: String,
        @RequestParam("feedback") rating: String,
        @RequestParam(name = "submission", required = false) submissionId: String
    ) {
        val feedback = feedbackService.saveFeedback(rating, message, submissionId)
        feedbackService.sendFeedbackMail(name, mail, feedback.rating, feedback.message, submissionId)
    }

    @PostMapping("/change-sequencing-data-received")
    fun changeSequencingDataReceived(@RequestParam submission: ApiSubmission, @RequestParam(required = false) received: Boolean): String {
        submission.externalDataAvailableForMerging = received
        val date = if (received) {
            val todayMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
            Date.from(todayMidnight.atZone(ZoneId.systemDefault()).toInstant())
        } else {
            null
        }
        submission.externalDataAvailabilityDate = date
        submission.startTerminationPeriod = date
        submissionRepository.save(submission)
        try {
            if (received && submission.isAutoClosed.not()) {
                if (submission.ownTransfer && submission.isFinished) {
                    val job = lsfCommandService.submitClusterJob(submission.sequencingTechnology.clusterJobTemplate!!, mapOf("-i" to "${submission.identifier.filter { it.isDigit() }.toInt()}"), submission)
                    job.submission = submission
                    clusterJobRepository.save(job)
                } else if (!submission.ownTransfer) {
                    mergingService.doMerging(submission, !submission.isFinished)
                }
            }
        } catch (e: Exception) {
            logger.info(e.message)
        }
        return "redirect:/metadata-validator/"
    }

    // please note if you change this url also @LoginPageInterceptor
    @PostMapping("/privacy-policy-accept")
    fun acceptDataSecurityStatement(): String {
        val person = ldapService.getPerson()
        person.acceptedDataSecurityStatementDate = Date()
        personRepository.saveAndFlush(person)
        return "redirect:/metadata-validator/"
    }

    @GetMapping("/get-user-token")
    fun setCookie(response: HttpServletResponse): ResponseEntity<*> {
        return try {
            val cookie = Cookie("token", ldapService.getPerson().apiToken)
            cookie.maxAge = 365 * 24 * 60 * 60
            response.addCookie(cookie)
            ResponseEntity("token loaded", HttpHeaders(), HttpStatus.OK)
        } catch (e: UserNotFoundException) {
            ResponseEntity("not logged in yet", HttpHeaders(), HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/mark-news-as-read")
    fun markNewsAsRead(): ResponseEntity<*> {
        val person = ldapService.getPerson()
        person.unreadNews = false
        personRepository.saveAndFlush(person)
        return ResponseEntity("done", HttpHeaders(), HttpStatus.OK)
    }

    @GetMapping("/admin/new-news")
    fun getPage(): String {
        return "admin/new-news"
    }

    @PostMapping("admin/rest-news-flag")
    fun resetForNews(): RedirectView {
        return if (ldapService.isCurrentUserAdmin()) {
            personRepository.findAll().forEach {
                it.unreadNews = true
                personRepository.saveAndFlush(it)
            }
            RedirectView("/")
        } else {
            RedirectView("/error/403")
        }
    }

    @GetMapping("/get-value-from-external-metadata-source/{methodName}")
    @ResponseBody
    fun getValueFromExternalMetadataSource(
        @PathVariable methodName: String,
        @RequestParam methodParams: Map<String, String>,
    ): String {
        return externalMetadataSourceService.getSingleValue(methodName, methodParams)
    }

    @GetMapping("/get-similar-pids")
    @ResponseBody
    fun getSimilarPids(
        @RequestParam pid: String,
        @RequestParam project: String,
    ): String {
        return sampleService.getSimilarPids(pid, project).sortedByDescending { it["similarity_num"] }.map { it["pid"] }.joinToString(",")
    }
}
