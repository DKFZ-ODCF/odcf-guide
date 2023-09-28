package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.RequestedValueServiceImpl
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anySet
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class RequestedValueServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var requestedValueServiceMock: RequestedValueServiceImpl

    @Mock
    lateinit var fieldRequestedValuesRepository: FieldRequestedValuesRepository

    @Mock
    lateinit var seqTypeRepository: SeqTypeRepository

    @Mock
    lateinit var seqTypeRequestedValuesRepository: SeqTypeRequestedValuesRepository

    @Mock
    lateinit var requestedValuesRepository: RequestedValuesRepository

    @Mock
    lateinit var mailService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var ldapService: LdapService

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var technicalSampleRepository: TechnicalSampleRepository

    @Mock
    lateinit var fileRepository: FileRepository

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var deletionService: DeletionService

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @Test
    fun `test accept requested value`() {
        val requestedValue = entityFactory.getRequestedValue()
        requestedValue.state = RequestedValue.State.REQUESTED

        requestedValueServiceMock.acceptValue(requestedValue)

        assertThat(requestedValue.state).isEqualTo(RequestedValue.State.ACCEPTED)
        assertThat(requestedValue.createdValueAs).isEqualTo("")
        verify(requestedValuesRepository, times(1)).save(requestedValue)
    }

    @Test
    fun `test accept requested seq type`() {
        val seqType = entityFactory.getSeqType()
        seqType.isRequested = true
        val requestedSeqType = entityFactory.getRequestedSeqType()
        requestedSeqType.requestedSeqType = seqType

        requestedValueServiceMock.acceptValue(requestedSeqType)

        assertThat(requestedSeqType.state).isEqualTo(RequestedValue.State.ACCEPTED)
        assertThat(requestedSeqType.createdValueAs).isEqualTo("")
        verify(requestedValuesRepository, times(1)).save(requestedSeqType)
        assertThat(seqType.isRequested).isEqualTo(false)
        verify(seqTypeRepository, times(1)).save(seqType)
    }

    @Test
    fun `test accept and correct value with sample`() {
        val requestedValue = entityFactory.getRequestedValue()
        requestedValue.state = RequestedValue.State.REQUESTED
        val sample = entityFactory.getSample()
        sample.pid = "requestedValue"
        val sample2 = entityFactory.getSample()
        val newValue = "new value"

        `when`(sampleRepository.findAllBySubmissionIn(requestedValue.usedSubmissions.toSet())).thenReturn(
            setOf(sample, sample2)
        )

        requestedValueServiceMock.acceptAndCorrectValue(requestedValue, newValue)

        assertThat(requestedValue.state).isEqualTo(RequestedValue.State.ACCEPTED)
        assertThat(requestedValue.createdValueAs).isEqualTo(newValue)
        assertThat(sample.pid).isEqualTo(newValue)
        assertThat(sample2.pid).isNotEqualTo(newValue)
        verify(requestedValuesRepository, times(1)).save(requestedValue)
        verify(sampleRepository, times(1)).saveAll(anySet())
    }

    @Test
    fun `test accept and correct value with mixed-in species`() {
        val requestedValue = entityFactory.getRequestedValue()
        requestedValue.fieldName = "speciesWithStrain"
        requestedValue.state = RequestedValue.State.REQUESTED
        val sample = entityFactory.getSample()
        sample.speciesWithStrain = "requestedValue+Human (Homo sapiens) [No strain available]"
        val sample2 = entityFactory.getSample()
        sample2.speciesWithStrain = "Human (Homo sapiens) [No strain available]+requestedValue"
        val sample3 = entityFactory.getSample()
        sample3.speciesWithStrain = "requestedValue"
        val sample4 = entityFactory.getSample()
        sample4.speciesWithStrain = "Human (Homo sapiens) [No strain available]"
        val newValue = "new value"

        `when`(sampleRepository.findAllBySubmissionIn(requestedValue.usedSubmissions.toSet())).thenReturn(
            setOf(sample, sample2, sample3, sample4)
        )

        requestedValueServiceMock.acceptAndCorrectValue(requestedValue, newValue)

        assertThat(requestedValue.state).isEqualTo(RequestedValue.State.ACCEPTED)
        assertThat(requestedValue.createdValueAs).isEqualTo(newValue)
        assertThat(sample.speciesWithStrain).isEqualTo("new value+Human (Homo sapiens) [No strain available]")
        assertThat(sample2.speciesWithStrain).isEqualTo("Human (Homo sapiens) [No strain available]+new value")
        assertThat(sample3.speciesWithStrain).isEqualTo(newValue)
        assertThat(sample4.speciesWithStrain).isNotEqualTo(newValue)
        verify(requestedValuesRepository, times(1)).save(requestedValue)
        verify(sampleRepository, times(1)).saveAll(anySet())
    }

    @Test
    fun `test accept and correct value with technical sample`() {
        val requestedValue = entityFactory.getRequestedValue()
        requestedValue.className = "TechnicalSample"
        requestedValue.fieldName = "center"
        requestedValue.state = RequestedValue.State.REQUESTED

        val sample = entityFactory.getSample()
        val technicalSample = entityFactory.getTechnicalSample(sample)
        technicalSample.center = "requestedValue"
        val newValue = "new value"

        `when`(sampleRepository.findAllBySubmissionIn(requestedValue.usedSubmissions.toSet())).thenReturn(setOf(sample))

        requestedValueServiceMock.acceptAndCorrectValue(requestedValue, newValue)

        assertThat(requestedValue.state).isEqualTo(RequestedValue.State.ACCEPTED)
        assertThat(requestedValue.createdValueAs).isEqualTo(newValue)
        verify(requestedValuesRepository, times(1)).save(requestedValue)
        verify(technicalSampleRepository, times(1)).saveAll(anySet())
    }

    @Test
    fun `test accept and correct value with file`() {
        val requestedValue = entityFactory.getRequestedValue()
        requestedValue.className = "File"
        requestedValue.fieldName = "fileName"
        requestedValue.state = RequestedValue.State.REQUESTED

        val sample = entityFactory.getSample()
        val file = entityFactory.getFile(sample)
        file.fileName = "requestedValue"
        val newValue = "new value"

        `when`(sampleRepository.findAllBySubmissionIn(requestedValue.usedSubmissions.toSet())).thenReturn(setOf(sample))

        requestedValueServiceMock.acceptAndCorrectValue(requestedValue, newValue)

        assertThat(requestedValue.state).isEqualTo(RequestedValue.State.ACCEPTED)
        assertThat(requestedValue.createdValueAs).isEqualTo(newValue)
        verify(requestedValuesRepository, times(1)).save(requestedValue)
        verify(fileRepository, times(1)).saveAll(anySet())
    }

    @Test
    fun `test accept and correct seq type`() {
        val seqType = entityFactory.getSeqType()
        seqType.isRequested = true
        val requestedSeqType = entityFactory.getRequestedSeqType()
        requestedSeqType.requestedSeqType = seqType
        val sample = entityFactory.getSample()
        sample.seqType = seqType
        val newValue = "new seq type name"

        `when`(sampleRepository.findAllBySubmissionIn(requestedSeqType.usedSubmissions.toSet())).thenReturn(setOf(sample))

        requestedValueServiceMock.acceptAndCorrectValue(requestedSeqType, newValue)

        assertThat(requestedSeqType.state).isEqualTo(RequestedValue.State.ACCEPTED)
        assertThat(requestedSeqType.createdValueAs).isEqualTo(newValue)
        assertThat(sample.seqType.toString()).isEqualTo(newValue)
        verify(requestedValuesRepository, times(1)).save(requestedSeqType)
        assertThat(seqType.name).isEqualTo(newValue)
        assertThat(seqType.isRequested).isEqualTo(false)
        verify(seqTypeRepository, times(1)).save(seqType)
    }

    @Test
    fun `test reject value`() {
        val requestedValue = entityFactory.getRequestedValue()
        requestedValue.state = RequestedValue.State.REQUESTED
        val sample = entityFactory.getSample()
        sample.pid = "requestedValue"
        sample.submission.status = Submission.Status.EDITED
        requestedValue.usedSubmissions.add(sample.submission)
        val sample2 = entityFactory.getSample()
        sample2.submission.status = Submission.Status.VALIDATED
        requestedValue.usedSubmissions.add(sample2.submission)

        `when`(sampleRepository.findAllBySubmissionIn(requestedValue.usedSubmissions.toSet())).thenReturn(
            setOf(sample, sample2)
        )

        requestedValueServiceMock.rejectValue(requestedValue)

        assertThat(requestedValue.state).isEqualTo(RequestedValue.State.REJECTED)
        assertThat(requestedValue.createdValueAs).isEqualTo("")
        assertThat(sample.pid).isEqualTo("")
        assertThat(sample.submission.status).isEqualTo(Submission.Status.EDITED)
        assertThat(sample2.submission.status).isEqualTo(Submission.Status.UNLOCKED)
        verify(requestedValuesRepository, times(1)).save(requestedValue)
        verify(sampleRepository, times(1)).saveAll(anySet())
    }

    @Test
    fun `test reject seqType`() {
        val seqType = entityFactory.getSeqType()
        seqType.name = "requestedSeqType"
        seqType.isRequested = true
        val requestedSeqType = entityFactory.getRequestedSeqType()
        requestedSeqType.requestedSeqType = seqType
        val sample = entityFactory.getSample()
        sample.seqType = seqType
        sample.submission.status = Submission.Status.EDITED
        requestedSeqType.usedSubmissions.add(sample.submission)
        val sample2 = entityFactory.getSample()
        sample2.submission.status = Submission.Status.VALIDATED
        requestedSeqType.usedSubmissions.add(sample2.submission)

        `when`(sampleRepository.findAllBySubmissionIn(requestedSeqType.usedSubmissions.toSet())).thenReturn(setOf(sample, sample2))
        `when`(seqTypeRequestedValuesRepository.findAllByRequestedSeqType(seqType)).thenReturn(setOf(requestedSeqType))

        requestedValueServiceMock.rejectValue(requestedSeqType)

        assertThat(requestedSeqType.state).isEqualTo(RequestedValue.State.REJECTED)
        assertThat(requestedSeqType.createdValueAs).isEqualTo("")
        verify(requestedValuesRepository, times(1)).save(requestedSeqType)
        verify(deletionService, times(1)).deleteSeqType(seqType)
        verify(deletionService, times(0)).deleteSeqType(sample2.seqType!!)
    }

    @Test
    fun `test reject value with mixed-in species`() {
        val requestedValue = entityFactory.getRequestedValue()
        requestedValue.fieldName = "speciesWithStrain"
        requestedValue.state = RequestedValue.State.REQUESTED
        val sample = entityFactory.getSample()
        sample.submission.status = Submission.Status.EDITED
        sample.speciesWithStrain = "requestedValue+Human (Homo sapiens) [No strain available]"
        val sample2 = entityFactory.getSample()
        sample2.submission.status = Submission.Status.VALIDATED
        sample2.speciesWithStrain = "Human (Homo sapiens) [No strain available]+requestedValue"
        requestedValue.usedSubmissions.add(sample.submission)
        requestedValue.usedSubmissions.add(sample2.submission)

        `when`(sampleRepository.findAllBySubmissionIn(requestedValue.usedSubmissions.toSet())).thenReturn(
            setOf(sample, sample2)
        )

        requestedValueServiceMock.rejectValue(requestedValue)

        assertThat(requestedValue.state).isEqualTo(RequestedValue.State.REJECTED)
        assertThat(requestedValue.createdValueAs).isEqualTo("")
        assertThat(sample.speciesWithStrain).isEqualTo(sample2.speciesWithStrain).isEqualTo("Human (Homo sapiens) [No strain available]")
        assertThat(sample.submission.status).isEqualTo(Submission.Status.EDITED)
        assertThat(sample2.submission.status).isEqualTo(Submission.Status.UNLOCKED)
        verify(requestedValuesRepository, times(1)).save(requestedValue)
        verify(sampleRepository, times(1)).saveAll(anySet())
    }

    @Test
    fun `Check getRequestedValuesForFieldAndSubmission functionality`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val requestedValue = entityFactory.getRequestedValue(submission)

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContainsOrRequester(submission, person)).thenReturn(setOf(requestedValue))

        val filteredValues = requestedValueServiceMock.getRequestedValuesForUserAndFieldNameAndSubmission("pid", submission)

        assertThat(filteredValues.size).isEqualTo(1)
        assertThat(filteredValues).isEqualTo(setOf("requestedValue(ReqVal)"))
    }

    @Test
    fun `Check getRequestedValuesForFieldAndSubmission functionality with filtering`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val requestedValue = entityFactory.getRequestedValue(submission)
        val requestedValue2 = entityFactory.getRequestedValue(submission)
        requestedValue2.fieldName = "someOtherFieldName"
        val requestedValue3 = entityFactory.getRequestedValue(submission)
        requestedValue3.setStateByString("rejected")

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContainsOrRequester(submission, person)).thenReturn(setOf(requestedValue, requestedValue2, requestedValue3))

        val filteredValues = requestedValueServiceMock.getRequestedValuesForUserAndFieldNameAndSubmission("pid", submission)

        assertThat(filteredValues.size).isEqualTo(1)
        assertThat(filteredValues).isEqualTo(setOf("requestedValue(ReqVal)"))
    }

    @Test
    fun `Check getRequestedSeqTypesForUserAndSubmission functionality`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val reqestedSeqType = entityFactory.getRequestedSeqType(submission, entityFactory.getSeqType())

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(seqTypeRequestedValuesRepository.findAllByUsedSubmissionsContainsOrRequester(submission, person)).thenReturn(setOf(reqestedSeqType))

        val filteredValues = requestedValueServiceMock.getRequestedSeqTypesForUserAndSubmission(submission)

        assertThat(filteredValues.size).isEqualTo(1)
        assertThat(filteredValues).isEqualTo(setOf(reqestedSeqType.requestedSeqType))
    }

    @Test
    fun `Check getRequestedSeqTypesForUserAndSubmission functionality with filtering`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val requestedSeqType = entityFactory.getRequestedSeqType(submission, entityFactory.getSeqType())
        val requestedSeqType2 = entityFactory.getRequestedSeqType(submission, entityFactory.getSeqType())
        requestedSeqType2.setStateByString("rejected")

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(seqTypeRequestedValuesRepository.findAllByUsedSubmissionsContainsOrRequester(submission, person)).thenReturn(setOf(requestedSeqType, requestedSeqType2))

        val filteredValues = requestedValueServiceMock.getRequestedSeqTypesForUserAndSubmission(submission)

        assertThat(filteredValues.size).isEqualTo(1)
        assertThat(filteredValues).isEqualTo(setOf(requestedSeqType.requestedSeqType))
    }

    @Test
    fun `Check saveRequestedValue functionality`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val fieldName = "fieldName"
        val className = "className"
        val newValue = "newValue"

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")).thenReturn("subject")
        `when`(fieldRequestedValuesRepository.findAllByFieldNameAndRequestedValue(fieldName, newValue)).thenReturn(setOf())
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        requestedValueServiceMock.saveRequestedValue(fieldName, className, newValue, submission)

        verify(fieldRequestedValuesRepository, times(1)).save(any())
        verify(mailService, times(1)).sendMailToTicketSystem(anyString(), anyString())
    }

    @Test
    fun `Check saveRequestedValue functionality with filtering and already existing ReqVal`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val fieldName = "pid"
        val className = "Sample"
        val newValue = "requestedValue"
        val reqVal1 = entityFactory.getRequestedValue()
        reqVal1.requester = person
        // all the following reqVals will get filtered out but are needed for the missing branch test (even though I'm still missing one that I can't figure out)
        val reqVal2 = entityFactory.getRequestedValue()
        reqVal2.usedSubmissions.add(submission)
        val reqVal3 = entityFactory.getRequestedValue(submission)
        reqVal3.requester = person
        val reqVal4 = entityFactory.getRequestedValue()

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")).thenReturn("subject")
        `when`(fieldRequestedValuesRepository.findAllByFieldNameAndRequestedValue(fieldName, newValue)).thenReturn(setOf(reqVal1, reqVal2, reqVal3, reqVal4))
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        requestedValueServiceMock.saveRequestedValue(fieldName, className, newValue, submission)

        verify(fieldRequestedValuesRepository, times(1)).save(any())
        verify(mailService, times(0)).sendMailToTicketSystem(anyString(), anyString())
        assertThat(reqVal1.usedSubmissions.size).isEqualTo(2)
    }

    @Test
    fun `Check saveRequestedValue functionality with already existing ReqVal but for a different user and in a different submission`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val fieldName = "pid"
        val className = "Sample"
        val newValue = "requestedValue"
        val reqVal1 = entityFactory.getRequestedValue()

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")).thenReturn("subject")
        `when`(fieldRequestedValuesRepository.findAllByFieldNameAndRequestedValue(fieldName, newValue)).thenReturn(setOf(reqVal1))
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        requestedValueServiceMock.saveRequestedValue(fieldName, className, newValue, submission)

        verify(fieldRequestedValuesRepository, times(1)).save(any())
        verify(mailService, times(1)).sendMailToTicketSystem(anyString(), anyString())
        assertThat(reqVal1.usedSubmissions.size).isEqualTo(1)
    }

    @Test
    fun `Check saveSeqTypeRequestedValue functionality`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val seqType = entityFactory.getSeqType()
        seqType.isRequested = true

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)
        `when`(seqTypeRequestedValuesRepository.findAllByRequestedValue(seqType.name)).thenReturn(setOf())
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")).thenReturn("subject")

        requestedValueServiceMock.saveSeqTypeRequestedValue(seqType, submission)

        verify(seqTypeRequestedValuesRepository, times(1)).save(any())
        verify(mailService, times(1)).sendMailToTicketSystem(anyString(), anyString())
    }

    @Test
    fun `Check saveSeqTypeRequestedValue functionality with filtering and already existing requested SeqType`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val seqType = entityFactory.getSeqType()
        seqType.isRequested = true
        val reqSeqType1 = entityFactory.getRequestedSeqType(entityFactory.getUploadSubmission(), seqType)
        reqSeqType1.requester = person
        // all the following reqSeqTypes will get filtered out but are needed for the missing branch test (even though I'm still missing one that I can't figure out)
        val reqSeqType2 = entityFactory.getRequestedSeqType()
        reqSeqType2.usedSubmissions.add(submission)
        val reqSeqType3 = entityFactory.getRequestedSeqType()
        reqSeqType3.requester = person
        val reqSeqType4 = entityFactory.getRequestedSeqType()

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")).thenReturn("subject")
        `when`(seqTypeRequestedValuesRepository.findAllByRequestedValue(seqType.name)).thenReturn(setOf(reqSeqType1, reqSeqType2, reqSeqType3, reqSeqType4))
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        requestedValueServiceMock.saveSeqTypeRequestedValue(seqType, submission)

        verify(seqTypeRequestedValuesRepository, times(1)).save(any())
        verify(mailService, times(0)).sendMailToTicketSystem(anyString(), anyString())
        assertThat(reqSeqType1.usedSubmissions.size).isEqualTo(2)
    }

    @Test
    fun `Check saveSeqTypeRequestedValue functionality with already existing ReqVal but for a different user and in a different submission`() {
        val submission = entityFactory.getUploadSubmission()
        val person = entityFactory.getPerson()
        val seqType = entityFactory.getSeqType()
        seqType.isRequested = true
        val reqSeqType1 = entityFactory.getRequestedSeqType(entityFactory.getUploadSubmission(), seqType)

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier(submission.identifier)).thenReturn(submission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")).thenReturn("subject")
        `when`(seqTypeRequestedValuesRepository.findAllByRequestedValue(seqType.name)).thenReturn(setOf(reqSeqType1))
        `when`(collectorService.getFormattedIdentifier(submission.identifier)).thenReturn(submission.identifier)

        requestedValueServiceMock.saveSeqTypeRequestedValue(seqType, submission)

        verify(seqTypeRequestedValuesRepository, times(1)).save(any())
        verify(mailService, times(1)).sendMailToTicketSystem(anyString(), anyString())
        assertThat(reqSeqType1.usedSubmissions.size).isEqualTo(1)
    }

    @Test
    fun `Check sendNewValueRequestMail functionality`() {
        val person = entityFactory.getPerson()
        val submission = entityFactory.getUploadSubmission()

        val subject = mailBundle.getString("newValuesController.newValueRegistrationMailSubject").replace("{1}", "fieldName")
        val body = mailBundle.getString("newValuesController.newValueRegistrationMailBody")
            .replace("{0}", person.fullName)
            .replace("{1}", person.mail)
            .replace("{2}", "formattedIdentifier")
            .replace("{3}", "fieldName")
            .replace("{4}", "newValue")

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(submissionRepository.findByIdentifier("formattedIdentifier")).thenReturn(submission)
        `when`(mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")).thenReturn(subject)
        `when`(collectorService.getFormattedIdentifier(anyString())).thenReturn("formattedIdentifier")

        requestedValueServiceMock.sendNewValueRequestMail("formattedIdentifier", "newValue", "fieldName")

        verify(mailService, times(1)).sendMailToTicketSystem(subject, body)
    }

    @TestFactory
    fun `Check getSubmissionUsesRequestedValues functionality with sample`() = listOf(
        "requestedValue" to mapOf("antibodyTarget" to setOf("requestedValue")),
        "someOtherValue" to mapOf()
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when checkSubmissionUsesRequestedValues where submission uses '$input' then return '$expected'") {
            val submission = entityFactory.getUploadSubmission()
            val sample = entityFactory.getSample(submission)
            sample.antibodyTarget = input
            val requestedValue = entityFactory.getRequestedValue(submission)
            requestedValue.requestedValue = "requestedValue"
            requestedValue.fieldName = "antibodyTarget"
            requestedValue.className = "Sample"
            val requestedValue2 = entityFactory.getRequestedValue(submission)
            requestedValue2.setStateByString("accepted")

            `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedValue, requestedValue2))
            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

            val usesReqValues = requestedValueServiceMock.getSubmissionUsesRequestedValues(submission)

            assertThat(usesReqValues).isEqualTo(expected)
        }
    }

    @TestFactory
    fun `Check getSubmissionUsesRequestedValues functionality with technical sample`() = listOf(
        "requestedValue" to mapOf("center" to setOf("requestedValue")),
        "someOtherValue" to mapOf()
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when checkSubmissionUsesRequestedValues where submission uses '$input' then return '$expected'") {
            val submission = entityFactory.getUploadSubmission()
            val sample = entityFactory.getSample(submission)
            val tsample = entityFactory.getTechnicalSample(sample)
            tsample.center = input
            val requestedValue = entityFactory.getRequestedValue(submission)
            requestedValue.requestedValue = "requestedValue"
            requestedValue.fieldName = "center"
            requestedValue.className = "TechnicalSample"

            `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedValue))
            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

            val usesReqValues = requestedValueServiceMock.getSubmissionUsesRequestedValues(submission)

            assertThat(usesReqValues).isEqualTo(expected)
        }
    }

    @Test
    fun `Check getSubmissionUsesRequestedValues when submission doesn't use the requested value`() {
        val submission = entityFactory.getUploadSubmission()
        val requestedValue = entityFactory.getRequestedValue(submission)
        val sample = entityFactory.getSample(submission)
        requestedValue.requestedValue = "requestedValue"
        requestedValue.fieldName = "center"
        requestedValue.className = "TechnicalSample"

        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedValue))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))

        val usesReqValues = requestedValueServiceMock.getSubmissionUsesRequestedValues(submission)

        assertThat(usesReqValues).isEqualTo(emptyMap<String, Set<String>>())
    }

    @Test
    fun `Check getSubmissionUsesRequestedValues when submission uses multiple requested values`() {
        val submission = entityFactory.getUploadSubmission()
        val seqType = entityFactory.getSeqType()
        seqType.isRequested = true
        val sample = entityFactory.getSample(submission)
        sample.seqType = seqType
        sample.speciesWithStrain = "speciesRequest"
        sample.antibodyTarget = "abtRequest"
        val sample2 = entityFactory.getSample(submission)
        sample2.antibodyTarget = "abtRequest2"
        val requestedValueCenter = entityFactory.getRequestedValue("speciesRequest", "speciesWithStrain", "Sample", submission)
        val requestedValueAbt = entityFactory.getRequestedValue("abtRequest", "antibodyTarget", "Sample", submission)
        val requestedValueAbt2 = entityFactory.getRequestedValue("abtRequest2", "antibodyTarget", "Sample", submission)
        val requestedSeqType = entityFactory.getRequestedSeqType(submission, seqType)
        val requestedSeqType2 = entityFactory.getRequestedSeqType()
        requestedSeqType2.setStateByString("accepted")

        `when`(seqTypeRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedSeqType, requestedSeqType2))
        `when`(fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission)).thenReturn(setOf(requestedValueCenter, requestedValueAbt, requestedValueAbt2))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample, sample2))

        val usesReqValues = requestedValueServiceMock.getSubmissionUsesRequestedValues(submission)

        assertThat(usesReqValues).isEqualTo(
            mapOf(
                "antibodyTarget" to setOf("abtRequest", "abtRequest2"),
                "speciesWithStrain" to setOf("speciesRequest"),
                "seqType" to setOf("requestedSeqType")
            )
        )
    }
}
