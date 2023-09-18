package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.parser.Parser
import de.dkfz.odcf.guide.entity.parser.ParserComponent
import de.dkfz.odcf.guide.entity.parser.ParserField
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.ParserException
import de.dkfz.odcf.guide.helperObjects.ParserForm
import de.dkfz.odcf.guide.service.interfaces.ParserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

@Service
open class ParserServiceImpl(
    private val parserRepository: ParserRepository,
    private val parserFieldRepository: ParserFieldRepository,
    private val parserComponentRepository: ParserComponentRepository,
    private val sampleRepository: SampleRepository,
    private val submissionRepository: SubmissionRepository
) : ParserService {

    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @Transactional(rollbackFor = [Exception::class])
    @Throws(ParserException::class)
    override fun saveParser(parserForm: ParserForm): Parser {
        val parser = parserForm.parser ?: throw ParserException("No Parser found.")
        val project = parserForm.parser!!.project
        if (parserRepository.findByProject(project) != null && parserForm.parser!!.id == 0) {
            throw ParserException("There already exists a Parser for this project.")
        }
        parserForm.field?.filter { it.fieldName.isNotBlank() }?.forEach { parserField ->
            parserField.parser = parser
            parserFieldRepository.save(parserField)
            val components = parserField.parserComponents.orEmpty().filter { it.componentName.isNotBlank() }
            components.forEach { parserComponent ->
                parserComponent.setParserMapping(
                    parserComponent.parserMappingKeys.orEmpty().zip(parserComponent.parserMappingValues.orEmpty()).toMap()
                )
                parserComponent.parserField = parserField
                parserComponentRepository.save(parserComponent)
            }
            if (parser.id != 0) {
                parserComponentRepository.deleteAll(parserComponentRepository.findAllByParserField(parserField).filter { it.id !in components.map { it.id } })
            }
            parserFieldRepository.save(parserField)
        } ?: throw ParserException("No parser fields found")

        parser.parserFields = parserForm.field
        parserRepository.saveAndFlush(parser)
        return parser
    }

    override fun applyParser(submission: Submission) {
        val errorListParseIdentifier = mutableListOf<String>()
        val errorListProject = mutableListOf<String>()

        for (sample in submission.samples) {
            val parser = parserRepository.findByProject(sample.project)

            if (parser != null) {
                val parseIdentifier = sample.parseIdentifier

                if (parseIdentifier.matches(parser.formattedRegex.toRegex())) {

                    for (parserField in parser.parserFields!!) {
                        val components = parserField.parserComponents

                        if (!components.isNullOrEmpty()) {
                            val property = Sample::class.memberProperties.filterIsInstance<KMutableProperty<*>>().find { it.name == parserField.columnMapping }
                            property?.setter?.call(sample, createStringForParserField(parser.formattedRegex.toRegex(), parserField, parseIdentifier, components)) ?: errorListParseIdentifier.add(parseIdentifier)
                        }
                    }
                    sampleRepository.save(sample)
                } else {
                    errorListParseIdentifier.add(parseIdentifier)
                }
            } else {
                errorListProject.add(sample.project)
            }
        }

        val messagesForUser = emptySet<String>().toMutableSet()
        if (errorListParseIdentifier.isNotEmpty()) {
            messagesForUser.add(bundle.getString("parser.parserRegexMismatchException").replace("<PARSE IDENTIFIER>", errorListParseIdentifier.toString()))
        }
        if (errorListProject.isNotEmpty()) {
            messagesForUser.add(bundle.getString("parser.parserNotFoundException").replace("<PROJECT>", errorListProject.toSet().toString()))
        }

        if (messagesForUser.isNotEmpty()) throw ParserException(messagesForUser.joinToString("\n"))
        submissionRepository.save(submission)
    }

    /**
     * Creates the parsed string in the order of components for a given parser field.
     *
     * @param parserRegex Regex for the full parser
     * @param parserField Current property to be parsed (e.g. patient ID, sample type)
     * @param parseIdentifier The string that has to be translated into different properties
     * @param components The components of the current parser field
     * @return the parsed String to write into the property of a sample
     */
    fun createStringForParserField(parserRegex: Regex, parserField: ParserField, parseIdentifier: String, components: List<ParserComponent>): String {
        val parserParts = parserRegex.matchEntire(parseIdentifier)
        var resultString = parserField.orderOfComponents

        components.forEach { component ->
            val name = component.componentName.replace(" ", "")
            val componentValue = try {
                var value = parserParts!!.groups[name]?.value
                if (value != null) {
                    if (component.parserMapping.isNotEmpty()) {
                        value = component.parserMapping[value] ?: value
                    }
                    value.padStart(component.numberOfDigits, '0')
                } else {
                    ""
                }
            } catch (e: IllegalArgumentException) {
                ""
            }

            if (parserField.orderOfComponents.isBlank()) {
                resultString += componentValue
            } else {
                resultString = resultString.replace("[" + component.componentName + "]", componentValue)
            }
        }
        return resultString
    }
}
