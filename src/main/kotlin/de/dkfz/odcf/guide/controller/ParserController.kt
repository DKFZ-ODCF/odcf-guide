package de.dkfz.odcf.guide.controller

import de.dkfz.odcf.guide.ParserRepository
import de.dkfz.odcf.guide.entity.parser.ParserField
import de.dkfz.odcf.guide.exceptions.ParserException
import de.dkfz.odcf.guide.helperObjects.ParserForm
import de.dkfz.odcf.guide.service.interfaces.ParserService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/parser")
class ParserController(
    private val ldapService: LdapService,
    private val parserService: ParserService,
    private val parserRepository: ParserRepository,
    private val collectorService: CollectorService
) {

    @GetMapping("")
    fun showParser(model: Model): String {
        val person = ldapService.getPerson()

        return if (person.isAdmin) {
            model["parsers"] = parserRepository.findAll()
            "parser/overview"
        } else {
            model["errorMessage"] = "You are not allowed to access this page!"
            model["error"] = true
            "redirect:/error/403"
        }
    }

    @GetMapping("/information")
    fun showParserDetails(
        @RequestParam("project") project: String,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        val person = ldapService.getPerson()

        return if (person.isAdmin) {
            model["project"] = project
            val parser = parserRepository.findByProject(project)
            if (parser != null) {
                model["parser"] = parser
                model["parserFields"] = parser.parserFields!!
                "parser/information"
            } else {
                redirectAttributes.addFlashAttribute("error", true)
                redirectAttributes.addFlashAttribute("errorMessage", "There is no Parser for the project '$project'.")
                "redirect:/parser"
            }
        } else {
            model["errorMessage"] = "You are not allowed to access this page!"
            model["error"] = true
            "redirect:/error/403"
        }
    }

    @GetMapping("/add-new-parser")
    fun addNewParser(model: Model): String {
        return if (ldapService.isCurrentUserAdmin()) {
            val fields = listOf(ParserField("patient_id", "pid"), ParserField("sample_type", "sampleType"))
            model["projects"] = collectorService.getProjectsForAdmins()
            model["fields"] = fields
            model["defaultRegex"] = fields.joinToString(separator = "") { "[${it.columnMapping}]" }
            "parser/save-parser"
        } else {
            model["errorMessage"] = "You are not allowed to access this page!"
            model["error"] = true
            "redirect:/error/403"
        }
    }

    @GetMapping("/edit-parser")
    fun editParser(model: Model, @RequestParam project: String): String {
        return if (ldapService.isCurrentUserAdmin()) {
            val parser = parserRepository.findByProject(project)
            if (parser != null) {
                model["parser"] = parser

                parser.parserFields?.forEach { field ->
                    when (field.fieldName) {
                        "pid" -> model["pidField"] = field
                        "sampleType" -> model["stField"] = field
                    }
                }
            }
            model["fields"] = parser?.parserFields.orEmpty()
            model["projects"] = collectorService.getProjectsForAdmins()
            "parser/save-parser"
        } else {
            model["errorMessage"] = "You are not allowed to access this page!"
            model["error"] = true
            "redirect:/error/403"
        }
    }

    @PostMapping("/save-parser")
    fun saveNewParser(
        redirectAttributes: RedirectAttributes,
        @ModelAttribute parserForm: ParserForm
    ): String {
        return try {
            parserService.saveParser(parserForm)
            "redirect:/parser/information?project=${parserForm.parser?.project}"
        } catch (e: ParserException) {
            redirectAttributes.addFlashAttribute("errorMessage", e.message!!)
            redirectAttributes.addFlashAttribute("error", true)
            "redirect:/parser"
        }
    }

    @GetMapping("/deleteParser")
    fun delete(@RequestParam("project") project: String, redirectAttributes: RedirectAttributes): String {
        val (messageType, message) = if (!ldapService.isCurrentUserAdmin()) {
            Pair("error", "Incorrect secret! Did not delete parser.")
        } else {
            val parser = parserRepository.findByProject(project)
            if (parser != null) {
                parserRepository.delete(parser)
                if (parserRepository.findByProject(parser.project) == null) {
                    Pair("success", "Parser of project $project has been deleted as requested.")
                } else {
                    Pair("error", "Could not delete parser of project $project. Please contact ODCF Validation Service Developers!")
                }
            } else {
                Pair("error", "Could not find a parser of project $project")
            }
        }

        redirectAttributes.addFlashAttribute("${messageType}Message", message)
        redirectAttributes.addFlashAttribute(messageType, true)
        return "redirect:/parser/"
    }
}
