package de.dkfz.odcf.guide.controller.metadataValidation

import com.fasterxml.jackson.core.type.TypeReference
import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.EXTENDED_READ_ONLY
import de.dkfz.odcf.guide.controller.metadataValidation.MetaValController.Companion.SIMPLE_READ_ONLY
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.TechnicalSample
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.helperObjects.enums.ExtendedPage
import de.dkfz.odcf.guide.helperObjects.enums.SimplePage
import de.dkfz.odcf.guide.helperObjects.mapDistinctAndNotNullOrBlank
import de.dkfz.odcf.guide.helperObjects.setParallel
import de.dkfz.odcf.guide.service.interfaces.BrowserService
import de.dkfz.odcf.guide.service.interfaces.RequestedValueService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.ModificationService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.*
import javax.persistence.EntityManager

@Controller
@RequestMapping("/metadata-validator/submission")
class SubmissionTablesGetController(
    private val submissionRepository: SubmissionRepository,
    private val seqTypeRepository: SeqTypeRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val submissionService: SubmissionService,
    private val ldapService: LdapService,
    private val entityManager: EntityManager,
    private val collectorService: CollectorService,
    private val modificationService: ModificationService,
    private val fileRepository: FileRepository,
    private val parserRepository: ParserRepository,
    private val sampleRepository: SampleRepository,
    private val requestedValueService: RequestedValueService,
    private val browserService: BrowserService,
    private val jsonApiService: JsonApiService,
    private val env: Environment
) {

    @InitBinder
    fun initBinder(binder: WebDataBinder) {
        binder.autoGrowCollectionLimit = 2048
    }

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @GetMapping("/switch-to-user-view")
    fun switchToUserView(@RequestParam identifier: String): String {
        val submission = submissionRepository.findByIdentifier(identifier)
        return "redirect:" + (if (submission!!.isExtended) MetaValController.EXTENDED_TABLE_PAGE_USER else MetaValController.SIMPLE_TABLE_PAGE_USER) + "?uuid=${submission.uuid}"
    }

    @GetMapping("/simple/user")
    fun showSimpleMetadataValidator(
        @RequestParam uuid: String,
        @RequestParam(required = false) backAndEdit: Boolean,
        @RequestHeader(value = "User-Agent", required = false) userAgent: String,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        return getUserPage(uuid, backAndEdit, userAgent, model, redirectAttributes, false)
    }

    @GetMapping("/extended/user")
    fun showExtendedMetadataValidator(
        @RequestParam uuid: String,
        @RequestParam(required = false) backAndEdit: Boolean,
        @RequestHeader(value = "User-Agent", required = false) userAgent: String,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        return getUserPage(uuid, backAndEdit, userAgent, model, redirectAttributes, true)
    }

    private fun getUserPage(
        uuid: String,
        backAndEdit: Boolean,
        userAgent: String,
        model: Model,
        redirectAttributes: RedirectAttributes,
        extendedPage: Boolean
    ): String {
        if (!browserService.checkIfBrowserSupported(userAgent)) {
            return "redirect:/error/412"
        }
        val submission = submissionRepository.findByUuid(UUID.fromString(uuid))
            ?: return "redirect:/error/404?parameter=$uuid"

        val timeout = env.getProperty("application.timeout", "${MetaValController.LOCKED_TIMEOUT_IN_MIN}").toInt()
        val person = ldapService.getPerson()

        if (submission.isFinished) {
            val additional = mapOf("header" to "Finally submitted.")
            return redirectReadOnlyPage(submission, redirectAttributes, additional, extendedPage)
        } else if (submission.isPaused) {
            val additional = mapOf(
                "header" to "Submission is in status " + submission.status +
                    (" with the comment: ['${submission.onHoldComment}']".takeUnless { submission.onHoldComment.isBlank() } ?: "") + "."
            )
            return redirectReadOnlyPage(submission, redirectAttributes, additional, extendedPage)
        } else if (backAndEdit) {
            submissionService.changeSubmissionState(submission, Submission.Status.EDITED, ldapService.getPerson().username)
        }

        val availableProjects = collectorService.getProjectsForSubmissionAndUser(submission, ldapService.getPerson())
        val projectPrefixMapping = collectorService.getProjectPrefixesForSamplesInSubmission(submission, availableProjects)

        // "/simple/user" was requested. enforce non-admin view once
        entityManager.detach(person)
        person.isAdmin = false

        setModelAttributes(model, availableProjects, submission, projectPrefixMapping, person, timeout)
        model["uuid"] = uuid
        if (!userAgent.contains("Firefox")) {
            model["info"] = true
            model["infoMessage"] = "Please note: we recommend using Firefox. " +
                "If you experience any technical difficulties please switch over to Firefox."
        }

        model["extended"] = extendedPage
        if (extendedPage) {
            setExtendedModelAttributes(model, submission)
        }

        return if (submission.status == Submission.Status.LOCKED && submission.lockUser != person.username) {
            val diffTime = (Date().time - submission.lockDate!!.time) / (60 * 1000)
            val additional = mapOf(
                "header" to "This page is currently locked for ${timeout - diffTime} minutes because another user working on this already."
            )
            redirectReadOnlyPage(submission, redirectAttributes, additional, extendedPage)
        } else if (submission.isWriteProtected) {
            val additional = emptyMap<String, Any>().toMutableMap()
            additional["header"] = when (listOf(Submission.Status.CLOSED, Submission.Status.EXPORTED).contains(submission.status)) {
                true -> "Finally submitted."
                false -> {
                    if (submission.isValidated) {
                        val buttonName = bundle.getString("readonly.submitFinally")
                        additional["merging"] = collectorService.foundMergeableSamples(submission)
                        additional["addition"] = "You are not done yet - please check carefully and finalize your submission by clicking button '$buttonName'."
                        "Summary of "
                    } else {
                        "${submission.status}."
                    }
                }
            }
            redirectReadOnlyPage(submission, redirectAttributes, additional, extendedPage)
        } else {
            getEditablePage(submission, person, extendedPage, backAndEdit, projectPrefixMapping)
        }
    }

    @GetMapping("/simple/admin")
    fun showAdminMetaDataValidator(
        @RequestParam identifier: String,
        @RequestParam(required = false) backAndEdit: Boolean,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        return getAdminPage(identifier, model, redirectAttributes, backAndEdit)
    }

    @GetMapping("/extended/admin")
    fun showSimpleCsvValidator(
        @RequestParam identifier: String,
        @RequestParam(required = false) backAndEdit: Boolean,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        return getAdminPage(identifier, model, redirectAttributes, backAndEdit)
    }

    private fun getAdminPage(
        identifier: String,
        model: Model,
        redirectAttributes: RedirectAttributes,
        backAndEdit: Boolean
    ): String {

        val submission = submissionRepository.findByIdentifier(identifier) ?: return "redirect:/error/404"
        val extendedPage = submission.isExtended

        val person = ldapService.getPerson()
        if (!person.isAdmin) {
            return if (!extendedPage) "redirect:" + MetaValController.SIMPLE_TABLE_PAGE_USER + "?uuid=" + submission.uuid
            else "redirect:" + MetaValController.EXTENDED_TABLE_PAGE_USER + "?uuid=" + submission.uuid
        }

        val availableProjects = collectorService.getProjectsForAdmins()
        val projectPrefixMapping = collectorService.getProjectPrefixesForSamplesInSubmission(submission, null)

        setModelAttributes(model, availableProjects, submission, projectPrefixMapping, person, 60)

        model["extended"] = extendedPage
        if (extendedPage) {
            setExtendedModelAttributes(model, submission)
        }

        return if (submission.isWriteProtected && !backAndEdit) {
            val headerString = if (submission.isFinished) "Finally submitted." else submission.status.toString()
            val additional = mapOf("header" to headerString)
            redirectReadOnlyPage(submission, redirectAttributes, additional, extendedPage)
        } else {
            getEditablePage(submission, person, extendedPage, backAndEdit, projectPrefixMapping)
        }
    }

    private fun redirectReadOnlyPage(
        submission: Submission,
        redirectAttributes: RedirectAttributes,
        additional: Map<String, Any>,
        extendedPage: Boolean
    ): String {
        additional.forEach {
            redirectAttributes.addFlashAttribute(it.key, it.value)
        }
        return if (!extendedPage) "redirect:$SIMPLE_READ_ONLY?uuid=${submission.uuid}"
        else "redirect:$EXTENDED_READ_ONLY?uuid=${submission.uuid}"
    }

    private fun getEditablePage(
        submission: Submission,
        user: Person,
        extendedPage: Boolean,
        backAndEdit: Boolean,
        projectPrefixMapping: Map<String, String?>
    ): String {
        modificationService.removeProjectPrefixFromPids(submission, projectPrefixMapping)

        if (backAndEdit && submission.isWriteProtected) {
            submissionService.changeSubmissionState(submission, Submission.Status.UNLOCKED, user.username)
        }
        if (!user.isAdmin) {
            submissionService.changeSubmissionState(submission, Submission.Status.LOCKED, user.username)
        }
        return if (!extendedPage && submission.isExtended) {
            if (user.isAdmin) {
                "redirect:${MetaValController.EXTENDED_TABLE_PAGE_ADMIN}?identifier=${submission.identifier}"
            } else {
                "redirect:${MetaValController.EXTENDED_TABLE_PAGE_USER}?uuid=${submission.uuid}"
            }
        } else if (extendedPage) {
            "metadataValidator/extended/details"
        } else {
            "metadataValidator/simple/details"
        }
    }

    private fun setExtendedModelAttributes(model: Model, submission: Submission) {
        model["columns"] = ExtendedPage.values()
        model["readNumbers"] = listOf("1", "2", "i1", "i2", "I1", "I2")
        model["libLayouts"] = listOf("PAIRED", "SINGLE")
        model["centers"] = externalMetadataSourceService.getValuesAsSet("centers")
            .plus(requestedValueService.getRequestedValuesForUserAndFieldNameAndSubmission("center", submission))
        model["pipelines"] = externalMetadataSourceService.getValuesAsSet("pipelines")
            .plus(requestedValueService.getRequestedValuesForUserAndFieldNameAndSubmission("pipelineVersion", submission))
        val seqKitInfos = externalMetadataSourceService.getValuesAsSet("instrument-model-with-sequencing-kits")
        model["instrumentModels"] = seqKitInfos.map { it.replace("[]", "").trim() }.toSet().sorted()
            .plus(requestedValueService.getRequestedValuesForUserAndFieldNameAndSubmission("instrumentModelWithSequencingKit", submission))
        model["files"] = sampleRepository.findAllBySubmissionOrderById(submission)
            .associateWith { sample -> fileRepository.findAllBySample(sample).sortedBy { it.readNumber } }

        val fakeSample = Sample()
        fakeSample.id = -1
        val fakeTSample = TechnicalSample()
        fakeSample.id = -1
        fakeSample.technicalSample = fakeTSample
        val fakeFile = File()
        fakeFile.id = -1
        fakeFile.sample = fakeSample
        model["fakeFiles"] = mapOf(fakeSample to listOf(fakeFile))
    }

    private fun setModelAttributes(
        model: Model,
        availableProjects: Set<String>,
        submission: Submission,
        projectPrefixMapping: Map<String, String?>,
        user: Person,
        timeout: Int
    ) {
        val samples = sampleRepository.findAllBySubmissionOrderById(submission)
        model["columns"] = SimplePage.values()
        model["samples"] = model.getAttribute("samples") ?: samples
        model["selectedProjects"] = samples.mapDistinctAndNotNullOrBlank { it.project }.sorted()
        model["projects"] = availableProjects.sorted()
        model["projectPrefixes"] = projectPrefixMapping
        val seqTypes = seqTypeRepository.findAllByIsRequestedIsFalseOrderByNameAsc().toMutableList()
        if (submission.ownTransfer) {
            seqTypes.forEach {
                entityManager.detach(it)
                it.needLibPrepKit = false
                it.needSampleTypeCategory = false
            }
        }
        val requestedSeqTypes = requestedValueService.getRequestedSeqTypesForUserAndSubmission(submission)
        seqTypes.addAll(requestedSeqTypes)
        model["groupedSeqTypes"] = seqTypes.filterNot { it.isHiddenForUser }
            .groupBy { seqType -> "${seqType.basicSeqType} ${"single cell".takeIf { seqType.singleCell }.orEmpty()}".trim() }
            .toSortedMap()
        model["basicSeqTypes"] = seqTypes.map { it.basicSeqType }.toSet()
        model["sexTypes"] = Sample.Sex.values()
        model["speciesMap"] = externalMetadataSourceService.getValuesAsSetMap("speciesInfos").sortedBy { it["species_with_strain"] }.groupBy({ it["species"] }, { it["species_with_strain"] })
            .plus(("Approval pending" to requestedValueService.getRequestedValuesForUserAndFieldNameAndSubmission("speciesWithStrain", submission)))
        model["strainList"] = externalMetadataSourceService.getValuesAsSet("strains")
        model["antibodyTargets"] = externalMetadataSourceService.getValuesAsSet("antibodyTargets")
            .plus(requestedValueService.getRequestedValuesForUserAndFieldNameAndSubmission("antibodyTarget", submission))
        model["libPrepKitsWithAdapterSequences"] = jsonApiService.getValues("lib-prep-kits-with-adapter-sequences", apiType = ApiType.OTP, typeReference = object : TypeReference<Map<String, String>>() {})
            .plus(requestedValueService.getRequestedValuesForUserAndFieldNameAndSubmission("libraryPreparationKit", submission).associateWith { "" }.toList())
        model["submission"] = submission
        model["identifier"] = collectorService.getFormattedIdentifier(submission.identifier)
        model["seqTypesWithAntibodyTarget"] = seqTypeRepository.findAllByNeedAntibodyTargetIsTrue()
        model["timeout"] = timeout
        model["admin"] = user.isAdmin
        model["validation"] = submission.validationLevel.fields.associateBy { it.field }
        model["fakeFiles"] = listOf("fakeFile")
        model["otpProjectPath"] = runtimeOptionsRepository.findByName("otpProjectPath")?.value.orEmpty()
        model["otpProjectConfig"] = runtimeOptionsRepository.findByName("otpProjectConfig")?.value.orEmpty()
        model["hasSubmissionTypeSamples"] = submission.hasSubmissionTypeSamples
        model["hasStoppedSamples"] = samples.any { it.proceed == Sample.Proceed.NO }
        model["sampleTypeCategories"] = Sample.SampleTypeCategory.values()
        runBlocking(Dispatchers.Default) {
            model["hasParser"] = samples.setParallel { withContext(Dispatchers.IO) { parserRepository.findByProject(it.project) } }.any { it != null }
        }
    }
}
