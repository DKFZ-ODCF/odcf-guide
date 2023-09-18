package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.annotation.ReflectionDelimiter
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.requestedValues.FieldRequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue.State.ACCEPTED
import de.dkfz.odcf.guide.entity.requestedValues.RequestedValue.State.REJECTED
import de.dkfz.odcf.guide.entity.requestedValues.SeqTypeRequestedValue
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.TechnicalSample
import de.dkfz.odcf.guide.service.interfaces.RequestedValueService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.DeletionService
import org.springframework.stereotype.Service
import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Service
class RequestedValueServiceImpl(
    private val fieldRequestedValuesRepository: FieldRequestedValuesRepository,
    private val seqTypeRepository: SeqTypeRepository,
    private val seqTypeRequestedValuesRepository: SeqTypeRequestedValuesRepository,
    private val requestedValuesRepository: RequestedValuesRepository,
    private val mailService: MailSenderService,
    private val mailContentGeneratorService: MailContentGeneratorService,
    private val ldapService: LdapService,
    private val submissionRepository: SubmissionRepository,
    private val sampleRepository: SampleRepository,
    private val technicalSampleRepository: TechnicalSampleRepository,
    private val fileRepository: FileRepository,
    private val collectorService: CollectorService,
    private val deletionService: DeletionService,
) : RequestedValueService {

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    override fun acceptValue(requestedValue: RequestedValue) {
        requestedValue.state = ACCEPTED
        requestedValuesRepository.save(requestedValue)
        if (requestedValue is SeqTypeRequestedValue) {
            val seqType = requestedValue.requestedSeqType
            seqType!!.isRequested = false
            seqTypeRepository.save(seqType)
        }
    }

    override fun acceptAndCorrectValue(requestedValue: RequestedValue, newValue: String) {
        if (requestedValue is FieldRequestedValue) correctFields(requestedValue, newValue)
        requestedValue.createdValueAs = newValue
        if (requestedValue is SeqTypeRequestedValue) {
            val seqType = requestedValue.requestedSeqType
            seqType!!.name = requestedValue.createdValueAs.ifBlank { requestedValue.requestedValue }
        }
        acceptValue(requestedValue)
    }

    override fun rejectValue(requestedValue: RequestedValue) {
        if (requestedValue is SeqTypeRequestedValue) {
            deletionService.deleteSeqType(requestedValue.requestedSeqType!!)
        } else {
            correctFields(requestedValue as FieldRequestedValue, "", true)
        }
        requestedValue.state = REJECTED
        requestedValuesRepository.save(requestedValue)
    }

    private fun correctFields(requestedValue: FieldRequestedValue, newValue: String, unlockSubmission: Boolean = false) {
        val submissions = requestedValue.usedSubmissions.toSet()
        if (unlockSubmission) {
            submissions.forEach { if (it.isValidated) it.status = Submission.Status.UNLOCKED }
            submissionRepository.saveAll(submissions)
        }
        val samples = sampleRepository.findAllBySubmissionIn(submissions)
        val clazz = Class.forName("de.dkfz.odcf.guide.entity.submissionData.${requestedValue.className}").kotlin
        val property = clazz.memberProperties.filterIsInstance<KMutableProperty<*>>().single { it.name == requestedValue.fieldName }
        val oldValue = requestedValue.createdValueAs.ifBlank { requestedValue.requestedValue }
        when (clazz) {
            Sample::class -> sampleRepository.saveAll(correctFields(samples, property, oldValue, newValue))
            TechnicalSample::class -> technicalSampleRepository.saveAll(correctFields(samples.mapNotNull { it.technicalSample }.toSet(), property, oldValue, newValue))
            File::class -> fileRepository.saveAll(correctFields(samples.map { it.files }.flatten().toSet(), property, oldValue, newValue))
        }
    }

    private fun <T> correctFields(objects: Set<T>, property: KMutableProperty<*>, oldValue: String, newValue: String): Set<T> {
        objects.forEach {
            var delimiter = ""
            val values = if (property.name == Sample::speciesWithStrain.name) {
                delimiter = "+"
                property.getter.call(it).toString().split(delimiter)
            } else {
                listOf(property.getter.call(it).toString())
            }
            val newValues = values.map { if (it == oldValue) newValue else it }.filter { it.isNotBlank() }
            property.setter.call(it, newValues.joinToString(delimiter))
        }
        return objects
    }

    override fun getRequestedValuesForUserAndFieldNameAndSubmission(fieldName: String, submission: Submission): Set<String> {
        val user = ldapService.getPerson()
        return fieldRequestedValuesRepository.findAllByUsedSubmissionsContainsOrRequester(submission, user)
            .filter { it.fieldName == fieldName && !it.isFinished }
            .map { it.requestedValue + "(ReqVal)" }.sorted().toSet()
    }

    override fun getRequestedSeqTypesForUserAndSubmission(submission: Submission): Set<SeqType> {
        val user = ldapService.getPerson()
        return seqTypeRequestedValuesRepository.findAllByUsedSubmissionsContainsOrRequester(submission, user)
            .filter { !it.isFinished }
            .mapNotNull { it.requestedSeqType }.toSet()
    }

    override fun saveRequestedValue(fieldName: String, className: String, newValue: String, submission: Submission) {
        val user = ldapService.getPerson()
        var requestedValue = fieldRequestedValuesRepository.findAllByFieldNameAndRequestedValue(fieldName, newValue).filter { !it.isFinished }
            .firstOrNull { it.requester == user || it.usedSubmissions.contains(submission) }

        if (requestedValue == null) {
            requestedValue = FieldRequestedValue()
            requestedValue.fieldName = fieldName
            requestedValue.className = className
            requestedValue.requestedValue = newValue
            requestedValue.requester = user
            requestedValue.originSubmission = submission
            requestedValue.state = RequestedValue.State.REQUESTED

            sendNewValueRequestMail(submission.identifier, newValue, fieldName)
        }

        requestedValue.usedSubmissions.add(submission)
        fieldRequestedValuesRepository.save(requestedValue)
    }

    override fun saveSeqTypeRequestedValue(newSeqType: SeqType, submission: Submission) {
        val user = ldapService.getPerson()
        var requestedSeqType = seqTypeRequestedValuesRepository.findAllByRequestedValue(newSeqType.name).filter { !it.isFinished }
            .firstOrNull { it.requester == user || it.usedSubmissions.contains(submission) }

        if (requestedSeqType == null) {
            requestedSeqType = SeqTypeRequestedValue()
            requestedSeqType.requestedValue = newSeqType.name
            requestedSeqType.requestedSeqType = newSeqType
            requestedSeqType.requester = user
            requestedSeqType.originSubmission = submission
            requestedSeqType.state = RequestedValue.State.REQUESTED

            sendNewValueRequestMail(submission.identifier, newSeqType.name, "seqType")
        }

        requestedSeqType.usedSubmissions.add(submission)
        seqTypeRequestedValuesRepository.save(requestedSeqType)
    }

    override fun sendNewValueRequestMail(submissionIdentifier: String, newValue: String, fieldName: String) {
        val person = ldapService.getPerson()
        val submission = submissionRepository.findByIdentifier(submissionIdentifier)!!

        val subject = mailContentGeneratorService.getTicketSubject(submission, "newValuesController.newValueRegistrationMailSubject")
            .replace("{1}", fieldName)
        val body = mailBundle.getString("newValuesController.newValueRegistrationMailBody")
            .replace("{0}", person.fullName)
            .replace("{1}", person.mail)
            .replace("{2}", collectorService.getFormattedIdentifier(submissionIdentifier))
            .replace("{3}", fieldName)
            .replace("{4}", newValue)

        mailService.sendMailToTicketSystem(subject, body)
    }

    override fun getSubmissionUsesRequestedValues(submission: Submission): Map<String, Set<String>> {
        val samples = sampleRepository.findBySubmission(submission)

        val requestedSeqTypes = seqTypeRequestedValuesRepository.findAllByUsedSubmissionsContains(submission).filterNot { it.isFinished }
        val usedReqSeqTypes = mutableSetOf<SeqTypeRequestedValue>()

        requestedSeqTypes.forEach { reqSeqType ->
            samples.forEach {
                if (it.seqType == reqSeqType.requestedSeqType) usedReqSeqTypes.add(reqSeqType)
            }
        }

        val requestedValues = fieldRequestedValuesRepository.findAllByUsedSubmissionsContains(submission).filterNot { it.isFinished }
        val usedReqValues = mutableSetOf<FieldRequestedValue>()

        requestedValues.forEach { requestedValue ->
            val clazz = Class.forName("de.dkfz.odcf.guide.entity.submissionData.${requestedValue.className}").kotlin
            val property = clazz.memberProperties.filterIsInstance<KMutableProperty<*>>()
                .single { it.name == requestedValue.fieldName }
            when (clazz) {
                Sample::class -> samples.forEach { sample ->
                    val delimiter = property.findAnnotation<ReflectionDelimiter>()?.delimiter ?: "NO_DELIMITER"
                    property.getter.call(sample).toString().split(delimiter).forEach {
                        if (it == requestedValue.requestedValue) usedReqValues.add(requestedValue)
                    }
                }
                TechnicalSample::class -> samples.mapNotNull { it.technicalSample }.toSet().forEach {
                    if (property.getter.call(it) == requestedValue.requestedValue) usedReqValues.add(requestedValue)
                }
            }
        }

        return usedReqSeqTypes.groupBy({ "seqType" }, { it.requestedValue }).mapValues { it.value.toSet() } +
            usedReqValues.groupBy({ it.fieldName }, { it.requestedValue }).mapValues { it.value.toSet() }
    }
}
