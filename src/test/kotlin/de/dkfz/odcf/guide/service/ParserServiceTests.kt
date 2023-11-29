package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.exceptions.ParserException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.helperObjects.ParserForm
import de.dkfz.odcf.guide.service.implementation.ParserServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class ParserServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var parserServiceMock: ParserServiceImpl

    @Mock
    lateinit var parserRepository: ParserRepository

    @Mock
    lateinit var parserFieldRepository: ParserFieldRepository

    @Mock
    lateinit var parserComponentRepository: ParserComponentRepository

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Test
    fun `save a parser successfully`() {
        val parserForm = entityFactory.getParserForm()

        val result = parserServiceMock.saveParser(parserForm)

        assertThat(result.parserFields).hasSize(2)
        assertThat(result.parserFields).containsAll(parserForm.field)
        assertThat(result.parserRegex).isEqualTo(parserForm.parser!!.parserRegex)
        assertThat(result.project).isEqualTo(parserForm.parser!!.project)
        assertThat(result.formattedRegex).isEqualTo(
            "^" +
                "(?<${parserForm.field!!.first().parserComponents!!.first().componentName}>" +
                "${parserForm.field!!.first().parserComponents!!.first().componentRegex})" +
                "(?<${parserForm.field!!.first().parserComponents!!.last().componentName}>" +
                "${parserForm.field!!.first().parserComponents!!.last().componentRegex})" +
                "_" +
                "(?<${parserForm.field!!.last().parserComponents!!.first().componentName}>" +
                "${parserForm.field!!.last().parserComponents!!.first().componentRegex})" +
                "(?<${parserForm.field!!.last().parserComponents!!.last().componentName}>" +
                "${parserForm.field!!.last().parserComponents!!.last().componentRegex})" +
                "\$"
        )
    }

    @Test
    fun `update a parser successfully`() {
        val parserForm = entityFactory.getParserForm()

        `when`(parserComponentRepository.findAllByParserField(parserForm.field!!.first()))
            .thenReturn(parserForm.field!!.first().parserComponents!!.plus(entityFactory.getParserComponent()))

        val result = parserServiceMock.saveParser(parserForm)

        assertThat(result.parserFields).hasSize(2)
        assertThat(result.parserFields).containsAll(parserForm.field)
        assertThat(result.parserFields!!.flatMap { it.parserComponents!! }).hasSize(4)
    }

    @Test
    fun `When saveParser with no parser in the ParserForm throw Exception`() {
        val parserForm = ParserForm()

        assertThatExceptionOfType(ParserException::class.java).isThrownBy {
            parserServiceMock.saveParser(parserForm)
        }.withMessage("No Parser found.")
    }

    @Test
    fun `When saveParser with duplicated project throw Exception`() {
        val parserForm = entityFactory.getParserForm()

        `when`(parserRepository.findByProject("project")).thenReturn(entityFactory.getParser())

        assertThatExceptionOfType(ParserException::class.java).isThrownBy {
            parserServiceMock.saveParser(parserForm)
        }.withMessage("There already exists a Parser for this project.")
    }

    @Test
    fun `When saveParser with no parser field pid in the ParserForm throw Exception`() {
        val parserForm = entityFactory.getParserForm()
        parserForm.field = null

        assertThatExceptionOfType(ParserException::class.java).isThrownBy {
            parserServiceMock.saveParser(parserForm)
        }.withMessage("No parser fields found")
    }

    @Test
    fun `check createStringForParserField`() {
        val parser = entityFactory.getParser()
        val parserField = entityFactory.getParserField(parser)
        val parseIdentifier = "T1"
        val component1 = entityFactory.getParserComponent(parserField)
        component1.componentRegex = "[A-Z]"
        val component2 = entityFactory.getParserComponent(parserField, 2)
        component2.componentRegex = "[0-9]"
        val components = listOf(component1, component2)
        parser.parserRegex = "[fieldName1]"
        parser.parserFields = listOf(parserField)
        parserField.parserComponents = components
        parserField.orderOfComponents = "[${component1.componentName}]-[${component2.componentName}]"

        val stringForParserField = parserServiceMock.createStringForParserField(parser.formattedRegex.toRegex(), parserField, parseIdentifier, components)

        assertThat(stringForParserField).isEqualTo("Tumor-01")
    }

    @Test
    fun `check createStringForParserField with emptyOrderOfComponents and obsolete Parser Component`() {
        val parser = entityFactory.getParser()
        val parserField = entityFactory.getParserField(parser)
        val parseIdentifier = "T1"
        val component1 = entityFactory.getParserComponent(parserField)
        component1.componentRegex = "[A-Z]"
        val component2 = entityFactory.getParserComponent(parserField)
        component2.componentRegex = "[0-9]"
        component2.numberOfDigits = 2
        val component3 = entityFactory.getParserComponent(parserField)
        val components = listOf(component1, component2, component3)
        parserField.parserComponents = listOf(component1, component2)
        parser.parserRegex = "[fieldName1]"
        parser.parserFields = listOf(parserField)

        val stringForParserField = parserServiceMock.createStringForParserField(parser.formattedRegex.toRegex(), parserField, parseIdentifier, components)

        assertThat(stringForParserField).isEqualTo("Tumor01")
    }

    @Test
    fun `When applyParser with a project without parser throw Exception`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)

        `when`(parserRepository.findByProject("project")).thenReturn(null)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        assertThatExceptionOfType(ParserException::class.java).isThrownBy {
            parserServiceMock.applyParser(submission)
        }.withMessage("No Parser for Project [project] found. If you want to set up a parser for your project, please contact us.")
    }

    @Test
    fun `When applyParser with not matching parser identifier throw Exception`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        sample.parseIdentifier = "parseID"
        val parser = entityFactory.getParser()

        `when`(parserRepository.findByProject("project")).thenReturn(parser)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        assertThatExceptionOfType(ParserException::class.java).isThrownBy {
            parserServiceMock.applyParser(submission)
        }.withMessage("Parse Identifier [parseID] does not match any pattern that can be converted by the provided parser.")
    }

    @Test
    fun `apply parser with wrong entity mapping`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        val parser = entityFactory.getParser()
        parser.parserRegex = "abc"
        sample.parseIdentifier = "abc"
        val field = entityFactory.getParserField(parser)
        field.columnMapping = "fakeNews"
        parser.parserFields = listOf(field)

        `when`(parserRepository.findByProject("project")).thenReturn(parser)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        assertThatExceptionOfType(ParserException::class.java).isThrownBy {
            parserServiceMock.applyParser(submission)
        }.withMessage("Parse Identifier [abc] does not match any pattern that can be converted by the provided parser.")
    }

    @Test
    fun `check applyParser`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        sample.parseIdentifier = "P_1"
        val parser = entityFactory.getParser()

        val pidField = entityFactory.getParserField(parser)
        pidField.fieldName = "patient_id"
        pidField.columnMapping = "pid"
        val pidComponent = entityFactory.getParserComponent(pidField)
        pidComponent.componentRegex = "P"
        pidComponent.setParserMapping(mutableMapOf("P" to "Patient"))
        pidField.parserComponents = listOf(pidComponent)

        val stField = entityFactory.getParserField(parser)
        stField.fieldName = "sample_type"
        stField.columnMapping = "sampleType"
        val stComponent = entityFactory.getParserComponent(stField)
        stComponent.componentRegex = "[0-9]"
        stComponent.numberOfDigits = 2
        stField.parserComponents = listOf(stComponent)

        parser.parserRegex = "[pid]_[sampleType]"
        parser.parserFields = listOf(pidField, stField)

        `when`(parserRepository.findByProject("project")).thenReturn(parser)
        `when`(sampleRepository.save(sample)).thenReturn(sample)
        `when`(submissionRepository.save(submission)).thenReturn(submission)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        parserServiceMock.applyParser(submission)

        assertThat(sample.pid).isEqualTo("Patient")
        assertThat(sample.sampleType).isEqualTo("01")
    }

    @Test
    fun `check applyParser when one regex is part of the other`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        sample.parseIdentifier = "cc1_t1"
        val parser = entityFactory.getParser()
        val pidField = entityFactory.getParserField(parser)
        pidField.fieldName = "patient_id"
        pidField.columnMapping = "pid"
        val pidComponent1 = entityFactory.getParserComponent(pidField, 2)
        pidComponent1.componentRegex = "[a-z]{2}"
        val pidComponent2 = entityFactory.getParserComponent(pidField, 2)
        pidComponent2.componentRegex = "[0-9]{1}"
        pidField.parserComponents = listOf(pidComponent1, pidComponent2)

        val stField = entityFactory.getParserField(parser)
        stField.fieldName = "sample_type"
        stField.columnMapping = "sampleType"
        val stComponent1 = entityFactory.getParserComponent(stField)
        stComponent1.componentRegex = "[t|c]"
        val stComponent2 = entityFactory.getParserComponent(stField, 2)
        stComponent2.componentRegex = "[0-9]"
        stField.parserComponents = listOf(stComponent1, stComponent2)

        parser.parserFields = listOf(pidField, stField)
        parser.parserRegex = "[${pidField.columnMapping}]_[${stField.columnMapping}]"

        `when`(parserRepository.findByProject("project")).thenReturn(parser)
        `when`(sampleRepository.save(sample)).thenReturn(sample)
        `when`(submissionRepository.save(submission)).thenReturn(submission)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        parserServiceMock.applyParser(submission)

        assertThat(sample.pid).isEqualTo("cc01")
        assertThat(sample.sampleType).isEqualTo("t01")
    }

    @Test
    fun `check applyParser with optional components`() {
        val submission = entityFactory.getUploadSubmission()
        val sample = entityFactory.getSample(submission)
        sample.parseIdentifier = "P_1A"
        val parser = entityFactory.getParser()

        val pidField = entityFactory.getParserField(parser)
        pidField.fieldName = "patient_id"
        pidField.columnMapping = "pid"
        val pidComponent = entityFactory.getParserComponent(pidField)
        pidComponent.componentRegex = "P"
        pidComponent.setParserMapping(mutableMapOf("P" to "Patient"))
        pidField.parserComponents = listOf(pidComponent)

        val stField = entityFactory.getParserField(parser)
        stField.fieldName = "sample_type"
        stField.columnMapping = "sampleType"
        stField.orderOfComponents = "[st1][st2][st3]"
        val st1Component = entityFactory.getParserComponent(stField, 2)
        st1Component.componentName = "st1"
        st1Component.componentRegex = "[0-9]"
        val st2Component = entityFactory.getParserComponent(stField)
        st2Component.componentName = "st2"
        st2Component.componentRegex = "[A]"
        st2Component.optional = true
        val st3Component = entityFactory.getParserComponent(stField)
        st3Component.componentName = "st3"
        st3Component.componentRegex = "[B]"
        st3Component.parserMappingString = ""
        st3Component.optional = true
        stField.parserComponents = listOf(st1Component, st2Component, st3Component)

        parser.parserRegex = "[pid]_[sampleType]"
        parser.parserFields = listOf(pidField, stField)

        `when`(parserRepository.findByProject("project")).thenReturn(parser)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(sampleRepository.save(sample)).thenReturn(sample)
        `when`(submissionRepository.save(submission)).thenReturn(submission)

        parserServiceMock.applyParser(submission)

        assertThat(sample.pid).isEqualTo("Patient")
        assertThat(sample.sampleType).isEqualTo("01a")
    }
}
