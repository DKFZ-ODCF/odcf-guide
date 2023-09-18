package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.SeqTypeRepository
import de.dkfz.odcf.guide.SequencingTechnologyRepository
import de.dkfz.odcf.guide.ValidationLevelRepository
import de.dkfz.odcf.guide.ValidationRepository
import de.dkfz.odcf.guide.entity.metadata.SequencingTechnology
import de.dkfz.odcf.guide.entity.validation.Validation
import de.dkfz.odcf.guide.entity.validation.ValidationLevel
import de.dkfz.odcf.guide.exceptions.DuplicatedImportAliasException
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/metadata-input")
class InputController(
    private val ldapService: LdapService,
    private val collectorService: CollectorService,
    private val seqTypeMappingService: SeqTypeMappingService,
    private val deletionService: DeletionService,
    private val sequencingTechnologyRepository: SequencingTechnologyRepository,
    private val validationLevelRepository: ValidationLevelRepository,
    private val seqTypeRepository: SeqTypeRepository,
    private val validationRepository: ValidationRepository,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/ilse-import")
    fun import(model: Model): String {
        return pageHelper(model, "gui-import")
    }

    @GetMapping("/sequencing-type")
    fun add(model: Model): String {
        val seqTypes = seqTypeRepository.findAllByIsRequestedIsFalseOrderByNameAsc()
        model["basicSeqTypes"] = seqTypes.map { it.basicSeqType }.toSet()
        model["seqTypes"] = seqTypes
        return pageHelper(model, "seqType")
    }

    @GetMapping("/sequencing-technology")
    fun addSequencingTechnology(model: Model): String {
        model["sequencingTechnologies"] = sequencingTechnologyRepository.findAll()
        model["validationLevels"] = validationLevelRepository.findAll()
        return pageHelper(model, "sequencingTechnology")
    }

    @GetMapping("/validation")
    fun changeValidation(model: Model): String {
        model["validations"] = validationRepository.findAll()
        return pageHelper(model, "validationOverview")
    }

    @PostMapping("/save-seq-type")
    fun saveSeqType(
        redirectAttributes: RedirectAttributes,
        @RequestParam name: String,
        @RequestParam basicSeqType: String,
        @RequestParam(required = false) seqTypeId: Int?,
        @RequestParam(required = false) ilseNames: String?,
        @RequestParam(required = false) needAntibodyTarget: Boolean?,
        @RequestParam(required = false) needLibPrepKit: Boolean?,
        @RequestParam(required = false) singleCell: Boolean?,
        @RequestParam(required = false) tagmentation: Boolean?,
        @RequestParam(required = false) lowCoverageRequestable: Boolean?,
        @RequestParam(required = false) isDisplayedForUser: Boolean?,
    ): String {
        try {
            seqTypeMappingService.saveSeqType(
                name = name,
                seqTypeId = seqTypeId,
                basicSeqType = basicSeqType,
                ilseNames = ilseNames,
                needAntibodyTarget = needAntibodyTarget != null,
                needLibPrepKit = needLibPrepKit != null,
                singleCell = singleCell != null,
                tagmentation = tagmentation != null,
                lowCoverageRequestable = lowCoverageRequestable != null,
                isDisplayedForUser = isDisplayedForUser != null,
                newSeqTypeRequest = false,
            )
        } catch (e: DuplicatedImportAliasException) {
            redirectAttributes.addFlashAttribute("errorMessage", e.message!!)
            redirectAttributes.addFlashAttribute("error", true)
        }
        return "redirect:/metadata-input/sequencing-type"
    }

    @GetMapping("/delete-seq-type")
    fun deleteSeqType(@RequestParam id: Int, redirectAttributes: RedirectAttributes): String {
        val seqType = seqTypeRepository.getOne(id)
        val (type, message) = if (ldapService.isCurrentUserAdmin()) {
            try {
                if (deletionService.deleteSeqType(seqType)) {
                    "success" to "SeqType '${seqType.name}' has been deleted."
                } else {
                    "error" to "Could not delete seqType '${seqType.name}'."
                }
            } catch (e: DataIntegrityViolationException) {
                "error" to "There were samples found that use this seqType but couldn't be corrected. Could not delete seqType '${seqType.name}'."
            }
        } else {
            "error" to "You are not allowed to delete sequencing types."
        }
        redirectAttributes.addFlashAttribute("${type}Message", message)
        redirectAttributes.addFlashAttribute(type, true)
        return "redirect:/metadata-input/sequencing-type"
    }

    @PostMapping("/save-sequencing-technology")
    fun saveSequencingTechnology(
        model: Model,
        redirectAttributes: RedirectAttributes,
        @RequestParam id: Int,
        @RequestParam name: String,
        @RequestParam(required = false) importAliases: String?,
        @RequestParam validationLevel: ValidationLevel,
    ): String {
        val sequencingTechnology = sequencingTechnologyRepository.findById(id).orElse(SequencingTechnology())
        sequencingTechnology.name = name
        sequencingTechnology.importAliasesString = importAliases.orEmpty()
        sequencingTechnology.validationLevel = validationLevel
        try {
            sequencingTechnologyRepository.save(sequencingTechnology)
        } catch (e: DataIntegrityViolationException) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sequencing technology '$name' already exists.")
            redirectAttributes.addFlashAttribute("error", true)
        }
        return "redirect:/metadata-input/sequencing-technology"
    }

    private fun pageHelper(model: Model, selectedPage: String): String {
        val person = ldapService.getPerson()

        return if (person.isAdmin) {
            model.addAttribute("user", person)
            "metadataInput/$selectedPage"
        } else {
            model["errorMessage"] = "You are not allowed to access this page!"
            model["urls"] = collectorService.getUrlsByPerson(person)
            model["error"] = true
            "metadataValidator/user-overview"
        }
    }

    @PostMapping("/save-validation")
    fun saveValidation(
        redirectAttributes: RedirectAttributes,
        @RequestParam id: Int,
        @RequestParam(required = false) validationName: String,
        @RequestParam(required = false) regex: String?,
        @RequestParam(required = false) required: Boolean?,
        @RequestParam(required = false) description: String?,
    ): String {
        val validation = validationRepository.findById(id).orElse(Validation())
        validation.field = validationName
        validation.regex = regex.orEmpty()
        validation.required = required != null
        validation.description = description.orEmpty().replace("\r\n", "<br>")
        try {
            validationRepository.save(validation)
            logger.info("### Validation was modified:\n Field name: $validationName, Required: ${required != null}\n Regex: '$regex'\n Description: $description")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorMessage", e.message!!)
            redirectAttributes.addFlashAttribute("error", true)
        }
        return "redirect:/metadata-input/validation"
    }
}
