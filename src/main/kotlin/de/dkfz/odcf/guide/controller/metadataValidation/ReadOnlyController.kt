package de.dkfz.odcf.guide.controller.metadataValidation

import com.fasterxml.jackson.core.type.TypeReference
import de.dkfz.odcf.guide.FileRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.helperObjects.mapDistinctAndNotNullOrBlank
import de.dkfz.odcf.guide.helperObjects.toBool
import de.dkfz.odcf.guide.service.implementation.RequestedValueServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.external.LSFCommandService
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.SampleService
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.*

@Controller
@RequestMapping("/metadata-validator/submission")
class ReadOnlyController(
    private val submissionRepository: SubmissionRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val ldapService: LdapService,
    private val collectorService: CollectorService,
    private val fileRepository: FileRepository,
    private val lsfCommandService: LSFCommandService,
    private val sampleService: SampleService,
    private val requestedValueServiceImpl: RequestedValueServiceImpl,
    private val sampleRepository: SampleRepository,
    private val jsonApiService: JsonApiService,
    private val remoteCommandsService: RemoteCommandsService,
    private val env: Environment
) {

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @GetMapping("/simple/read-only")
    fun getSimpleReadOnlyPage(model: Model, @RequestParam uuid: String, redirectAttributes: RedirectAttributes): String {
        val submission = submissionRepository.findByUuid(UUID.fromString(uuid)) ?: return "redirect:/error/404?parameter=$uuid"

        return if (submission.isExtended) {
            "redirect:${MetaValController.EXTENDED_READ_ONLY}?uuid=$uuid"
        } else {
            getReadOnlyPage(model, submission, redirectAttributes)
        }
    }

    @GetMapping("/extended/read-only")
    fun getExtendedReadOnlyPage(model: Model, @RequestParam uuid: String, redirectAttributes: RedirectAttributes): String {
        val submission = submissionRepository.findByUuid(UUID.fromString(uuid)) ?: return "redirect:/error/404?parameter=$uuid"

        return if (!submission.isExtended) {
            "redirect:${MetaValController.SIMPLE_READ_ONLY}?uuid=$uuid"
        } else {
            getReadOnlyPage(model, submission, redirectAttributes, true)
        }
    }

    private fun getReadOnlyPage(model: Model, submission: Submission, redirectAttributes: RedirectAttributes, isExtended: Boolean = false): String {
        val formattedIdentifier = collectorService.getFormattedIdentifier(submission.identifier)
        if (!sampleRepository.existsBySubmission(submission)) {
            redirectAttributes.addFlashAttribute("errorMessage", "There were no samples found in submission $formattedIdentifier")
            redirectAttributes.addFlashAttribute("error", true)
            return "redirect:/metadata-validator/overview/" + ("user".takeIf { !ldapService.isCurrentUserAdmin() } ?: "admin")
        }

        val samplesWithMergeCandidates = collectorService.getSampleListEnrichedByMergingSamplesGrouped(submission)
        val samplesWithMergeCandidatesWithPaths = collectorService.getPathsWithSampleList(samplesWithMergeCandidates, submission)
        val samples = sampleRepository.findAllBySubmission(submission)

        model["admin"] = ldapService.isCurrentUserAdmin()
        model["submission"] = submission
        model["samples"] = samplesWithMergeCandidatesWithPaths
        model["identifier"] = formattedIdentifier
        model["merging"] = collectorService.foundMergeableSamples(submission)
        model["otpProjectPath"] = runtimeOptionsRepository.findByName("otpProjectPath")?.value.orEmpty()
        model["projects"] = samples.mapDistinctAndNotNullOrBlank { it.project }.sorted()
        model["seqTypes"] = samples.mapDistinctAndNotNullOrBlank { it.seqType?.name }.sorted().joinToString()
        model["numberOfXenograft"] = samples.count { it.xenograft }
        model["antibodyTargets"] = samples.mapDistinctAndNotNullOrBlank { it.antibodyTarget }.sorted().joinToString()
        model["numberOfStopped"] = samples.count { it.proceed == Sample.Proceed.NO }
        model["showAntibody"] = samples.any { it.seqType?.needAntibodyTarget ?: false }
        model["showTagmentationLib"] = samples.any { it.seqType?.tagmentation ?: false }
        model["showSingleCellWellLabel"] = samples.any { (it.seqType?.singleCell ?: false) && !(it.seqType?.name?.contains("10x") ?: false) }
        model["showLowCoverageRequested"] = samples.any { it.seqType?.lowCoverageRequestable ?: false }
        model["showSampleTypeCategory"] = samples.any { it.seqType?.needSampleTypeCategory ?: false }
        model["hasSubmissionTypeSamples"] = submission.hasSubmissionTypeSamples
        model["numberOfWithdrawn"] = samplesWithMergeCandidates.values.flatten().count { it.isExternalWithdrawnSample }
        model["clusterJobs"] = lsfCommandService.collectJobs(submission)
        model["usedRequestedValues"] = requestedValueServiceImpl.getSubmissionUsesRequestedValues(submission)
        model["libPrepKitAdapterSequence"] = samples.map { it.libraryPreparationKit }.distinct().associateWith {
            val adapterSequence = jsonApiService.getValues("adapter-sequence-for-lib-prep-kit", mapOf("libPrepKit" to it), ApiType.OTP, typeReference = object : TypeReference<Map<String, String>>() {})[it]
            val shortenedSequence = getShortAdapterSequence(adapterSequence.orEmpty())
            adapterSequence to "[$shortenedSequence...]".takeIf { shortenedSequence.isNotBlank() }.orEmpty()
        }
        val nearlyIdenticalPid = samples.associate { it.pid to sampleService.checkIfSamePidIsAvailable(it.pid, it.project) }
        model["nearlyIdenticalPid"] = nearlyIdenticalPid
        model["detectedNearlyIdenticalPid"] = nearlyIdenticalPid.any { it.value?.first == "danger" }
        model["allFilesReadable"] = true

        if (submission.isLocked && submission.lockUser != ldapService.getPerson().username) {
            val diffTime = (Date().time - submission.lockDate!!.time) / (60 * 1000)
            val timeout = env.getProperty("application.timeout", "${MetaValController.LOCKED_TIMEOUT_IN_MIN}").toInt()
            model["header"] = bundle.getString("readonly.header.timeout").replace("{0}", "${timeout - diffTime}")
        } else if (submission.isValidated) {
            model["merging"] = collectorService.foundMergeableSamples(submission)
            model["header"] = bundle.getString("readonly.header.validate").replace("{0}", bundle.getString("readonly.submitFinally"))
        } else if (submission.isFinished) {
            model["header"] = bundle.getString("readonly.header.finished")
        }

        return if (isExtended) {
            val samplesMap = emptyMap<String, Map<Sample, List<File>>>().toMutableMap()
            samplesWithMergeCandidatesWithPaths.entries.forEach { (path, samples) ->
                samplesMap[path] = samples.associateWith { sample ->
                    if (!sample.isMergeSample) {
                        fileRepository.findAllBySample(sample).map { file ->
                            val fileReadable = remoteCommandsService.getFromRemote("test -r ${file.fileName}; echo \$?").trim().toBool().not()
                            if (!fileReadable) { model["allFilesReadable"] = false }
                            file.isReadable = fileReadable
                            file
                        }
                    } else {
                        listOf(File(sample)) // empty fake file
                    }
                }
            }
            model["samples"] = samplesMap
            model["additionalHeaders"] = samplesWithMergeCandidates.values.asSequence().flatten()
                .mapNotNull { it.unknownValues?.keys?.toList() }.flatten()
                .distinct().filter { it.isNotBlank() }.toList()
            model["showAdditionalHeaders"] = runtimeOptionsRepository.findByName("unknownValuesAllowedOrganizationalUnits")?.value.orEmpty()
                .split(",").contains(submission.submitter.organizationalUnit)

            "metadataValidator/extended/read-only"
        } else {
            "metadataValidator/simple/read-only"
        }
    }

    private fun getShortAdapterSequence(adapterSequence: String): String {
        val regex = """^.*\n([ATGCN]+)[\s\S]*$""".toRegex()
        val slice = regex.matchEntire(adapterSequence)?.groups?.get(1)?.value?.slice(0..3)
        return slice.orEmpty()
    }
}
