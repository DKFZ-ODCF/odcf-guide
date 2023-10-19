package de.dkfz.odcf.guide.controller.metadataValidation

import de.dkfz.odcf.guide.FileRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.SubmissionRepository
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helperObjects.mapDistinctAndNotNullOrBlank
import de.dkfz.odcf.guide.service.implementation.RequestedValueServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.LSFCommandService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.slf4j.LoggerFactory
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
    private val requestedValueServiceImpl: RequestedValueServiceImpl,
    private val sampleRepository: SampleRepository,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

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
        model["hideFinalSubmitButton"] = !submission.isValidated
        model["submission"] = submission
        model["isFinished"] = submission.isFinished
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

        return if (isExtended) {
            var allFilesReadable = true
            val samplesMap = emptyMap<String, Map<Sample, List<File>>>().toMutableMap()
            samplesWithMergeCandidatesWithPaths.entries.forEach { (path, samples) ->
                samplesMap[path] = samples.associateWith { sample ->
                    if (!sample.isMergeSample) {
                        fileRepository.findAllBySample(sample).map { file ->
                            val fileReadable = fileChecker(file.fileName)
                            if (!fileReadable) { allFilesReadable = false }
                            file.isReadable = fileReadable
                            file
                        }
                    } else {
                        listOf(File(sample)) // empty fake file
                    }
                }
            }
            model["samples"] = samplesMap
            model["allFilesReadable"] = allFilesReadable
            model["additionalHeaders"] = samplesWithMergeCandidates.values.flatten()
                .mapNotNull { it.unknownValues?.keys?.toList() }.flatten()
                .distinct().filter { it.isNotBlank() }
            model["showAdditionalHeaders"] = runtimeOptionsRepository.findByName("unknownValuesAllowedOrganizationalUnits")?.value.orEmpty()
                .split(",").contains(submission.submitter.organizationalUnit)

            "metadataValidator/extended/read-only"
        } else {
            "metadataValidator/simple/read-only"
        }
    }

    private fun fileChecker(fileName: String): Boolean {
        val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("windows")
        val process = if (isWindows) {
            logger.info("Windows is not fully supported.")
            return true
            // ProcessBuilder("sh.exe", "--login", "-i", "-c", "\"ls -l $fileName\"").start()
        } else {
            val commandToCheckExternalFiles = runtimeOptionsRepository.findByName("commandToCheckExternalFiles")!!.value
            ProcessBuilder("/bin/bash", "-c", "$commandToCheckExternalFiles $fileName").start()
        }
        process.inputStream.reader(Charsets.UTF_8).use {
            val text = it.readText()
            if (text.isNotBlank()) {
                val permissions = text.split(" ")[0]
                val regex = "[bcdlps-][-r][-w][-x]((?<groupR>r)|-)[-w][-x]((?<otherR>r)|-)[-w][-x].*".toRegex()
                val groups = regex.matchEntire(permissions)?.groups
                return groups?.get("groupR") != null || groups?.get("otherR") != null
            }
        }
        process.waitFor()
        return false
    }
}
