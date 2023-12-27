package de.dkfz.odcf.guide.service.implementation.validator

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.annotation.PrivateSetter
import de.dkfz.odcf.guide.dtoObjects.FileGuiDto
import de.dkfz.odcf.guide.dtoObjects.SampleGuiDto
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.TechnicalSample
import de.dkfz.odcf.guide.entity.validation.ValidationLevel
import de.dkfz.odcf.guide.helperObjects.SampleForm
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.RequestedValueService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.*
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.ParseException
import java.util.*
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaSetter

@Service
open class SampleServiceImpl(
    private val sampleRepository: SampleRepository,
    private val technicalSampleRepository: TechnicalSampleRepository,
    private val fileRepository: FileRepository,
    private val validationRepository: ValidationRepository,
    private val submissionRepository: SubmissionRepository,
    private val seqTypeRepository: SeqTypeRepository,
    private val seqTypeRequestedValuesRepository: SeqTypeRequestedValuesRepository,
    private val collectorService: CollectorService,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val modificationService: ModificationService,
    private val fileService: FileService,
    private val externalMetadataSourceService: ExternalMetadataSourceService,
    private val submissionService: SubmissionService,
    private val requestedValueService: RequestedValueService,
    private val ldapService: LdapService
) : SampleService {

    override fun validateSamples(form: SampleForm, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>> {
        return validateSamples(form.sampleList.orEmpty(), validationLevel)
    }

    override fun validateFiles(form: SampleForm, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>> {
        return validateFiles(form.sampleList.orEmpty().flatMap { it.files.orEmpty() }, validationLevel)
    }

    override fun validateSamples(samples: List<SampleGuiDto>, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>> {
        val errors = emptyMap<Int, Map<String, Boolean>>().toMutableMap()
        samples.filter { it.id != 0 }.forEach { sample ->
            val sampleErrors = validateSample(sample, validationLevel)
            if (sampleErrors.isNotEmpty()) {
                errors[sample.id] = sampleErrors
            }
        }
        return errors
    }

    override fun validateFiles(files: List<FileGuiDto>, validationLevel: ValidationLevel): Map<Int, Map<String, Boolean>> {
        val errors = emptyMap<Int, Map<String, Boolean>>().toMutableMap()
        files.forEach { file ->
            val fileErrors = validateFile(file, validationLevel)
            if (fileErrors.isNotEmpty()) {
                errors[file.id] = fileErrors
            }
        }
        return errors
    }

    override fun validateSample(sample: SampleGuiDto, validationLevel: ValidationLevel): Map<String, Boolean> {
        val errors = emptyMap<String, Boolean>().toMutableMap()

        validateTextField("pid", sample.pid, errors, sample, validationLevel)
        validateTextField("sampleType", sample.sampleType, errors, sample, validationLevel)
        validateDropdown("project", sample.project, errors, validationLevel)
        validateDropdown("sex", sample.sex, errors, validationLevel)
        validateTextField("comment", sample.comment, errors, sample, validationLevel)
        if (sample.seqType == null) {
            if (validationLevel.fields.any { it.field == "seqType" }) errors["seqType"] = true
        } else {
            if (sample.seqType!!.tagmentation) {
                validateTextField("tagmentationLibrary", sample.tagmentationLibrary, errors, sample, validationLevel)
            }
            if (sample.seqType!!.singleCell && !sample.seqType!!.name.startsWith("10x")) {
                validateTextField("singleCellPlate", sample.singleCellPlate, errors, sample, validationLevel)
                validateTextField("singleCellWellPosition", sample.singleCellWellPosition, errors, sample, validationLevel)
            }
        }
        if (sample.technicalSample != null) {
            val technicalSample = sample.technicalSample!!
            validateTextField("readCount", technicalSample.readCount.toString(), errors, technicalSample, validationLevel)
            validateTextField("barcode", technicalSample.barcode, errors, technicalSample, validationLevel)
            validateTextField("externalSubmissionId", technicalSample.externalSubmissionId, errors, technicalSample, validationLevel)
            validateTextField("lane", technicalSample.lane.toString(), errors, technicalSample, validationLevel)
        }

        return errors
    }

    override fun validateFile(file: FileGuiDto, validationLevel: ValidationLevel): Map<String, Boolean> {
        val errors = emptyMap<String, Boolean>().toMutableMap()

        validateTextField("fileName", file.fileName, errors, file, validationLevel)
        validateTextField("md5", file.md5, errors, file, validationLevel)
        validateTextField("baseCount", file.baseCount.toString(), errors, file, validationLevel)
        validateTextField("cycleCount", file.cycleCount.toString(), errors, file, validationLevel)

        return errors
    }

    /**
     * Validates text field by verifying whether that text field is required and follows the Regex for that property.
     * If not, adds the field name mapped to `true` to the Map of errors.
     *
     *
     * @param T Class of the object to be validated
     * @param fieldName Name of the property to be validated
     * @param field Value in the text field to be validated
     * @param errors The field names mapped to a Boolean about whether the field has errors
     * @param objectToBeValidated the object to which the property to be validated belongs
     */
    private fun <T> validateTextField(fieldName: String, field: String, errors: MutableMap<String, Boolean>, objectToBeValidated: T, validationLevel: ValidationLevel) {
        if (validationLevel.fields.none { it.field == fieldName }) return
        val validation = validationRepository.findByField(fieldName)
        var regex = Regex(validation.regex)

        if (fieldName == "sampleType") {
            // fetch all sample types for this project from OTP;
            // if at least one of these already existing sample types does not match our (new, more stringent) regex,
            // we apply the old (less stringent) regex and allow more such sample types to be added (in this submission)
            objectToBeValidated as SampleGuiDto
            externalMetadataSourceService.getValuesAsSet("sampleTypesByProject", mapOf("project" to objectToBeValidated.project)).forEach {
                if (!regex.matches(it)) {
                    regex = Regex(validationRepository.findByField("oldSampleType").regex)
                    return@forEach
                }
            }
        }

        if (validation.required && (field.isBlank() || field == "null" || !regex.matches(field))) {
            errors[fieldName] = true
        }
    }

    /**
     * Validates dropdown by checking whether the dropdown has a value selected.
     * If not, adds the field name mapped to `true` to the Map of errors.
     *
     * @param fieldName Name of the property to be validated
     * @param field Value in the dropdown to be validated
     * @param errors The field names mapped to a Boolean about whether the field has errors
     */
    private fun validateDropdown(fieldName: String, field: Any?, errors: MutableMap<String, Boolean>, validationLevel: ValidationLevel) {
        if (validationLevel.fields.none { it.field == fieldName }) return
        if (field == null || field.toString().isBlank()) {
            errors[fieldName] = true
        }
    }

    override fun updateSamples(submission: Submission, form: SampleForm) {
        /* id == 0 are empty objects and id == -1 are new objects */
        val samples = mutableListOf<Sample>()
        val files = mutableListOf<File>()
        form.sampleList!!.filter { it.id != 0 }.forEach { sampleDto ->
            if (sampleDto.id == -1) sampleDto.id = 0
            val sample = convertToEntity(sampleDto)
            samples.add(sample)
            sampleDto.files.orEmpty().filter { it.id != 0 }.forEach { fileDto ->
                if (fileDto.id == -1) fileDto.id = 0
                files.add(fileService.convertToEntity(fileDto, sample))
            }
        }
        val username = ldapService.getPerson().username
        submissionService.changeSubmissionState(submission, Submission.Status.LOCKED, username)
        if (files.isNotEmpty()) {
            updateFilesAndSamples(submission, samples, files)
        } else {
            updateSamples(submission, samples)
        }
        submissionService.changeSubmissionState(submission, Submission.Status.UNLOCKED, username)
    }

    override fun updateSamples(submission: Submission, samples: List<Sample>) {
        val projectPrefixMapping = collectorService.getProjectPrefixesForSamplesInSubmission(submission, submission.originProjectsSet).toMutableMap()
        samples.forEach { sample ->
            sample.pid = handlePrefix(sample, projectPrefixMapping)
            sample.sampleType = sample.sampleType.lowercase()
            handleRequestedValues(setOf(sample::speciesWithStrain, sample::antibodyTarget, sample::libraryPreparationKit), submission)
            handleRequestedSeqTypes(sample.seqType, submission)
            sampleRepository.save(sample)
        }
        deleteNotNeededSeqTypeRequests()
    }

    override fun updateFilesAndSamples(submission: Submission, samples: List<Sample>, files: List<File>) {
        val projectPrefixMapping = collectorService.getProjectPrefixesForSamplesInSubmission(submission, submission.originProjectsSet).toMutableMap()
        samples.forEach { sample ->
            sample.submission = submission
            sample.pid = handlePrefix(sample, projectPrefixMapping)
            sample.sampleType = sample.sampleType.lowercase()
            sample.name = "${sample.pid}_${sample.sampleType}"
            val technicalSample = sample.technicalSample!!

            handleRequestedValues(
                setOf(
                    sample::speciesWithStrain,
                    sample::antibodyTarget,
                    sample::libraryPreparationKit,
                    technicalSample::center,
                    technicalSample::instrumentModelWithSequencingKit,
                    technicalSample::pipelineVersion,
                ),
                submission
            )
            handleRequestedSeqTypes(sample.seqType, submission)

            technicalSampleRepository.save(technicalSample)
            sampleRepository.save(sample)
            fileRepository.saveAll(files)
        }
        mergeFastqFilePairs(fileRepository.findAllBySampleIn(samples), submission)
        deletedFilesAndSamples(submission)
        deleteNotNeededSeqTypeRequests()
        submission.startTerminationPeriod = Date()
        submissionRepository.saveAndFlush(submission)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun mergeFastqFilePairs(sampleFiles: List<File>, submission: Submission) {
        val regexSuffix = runtimeOptionsRepository.findByName("fastqFileSuffix")!!.value
        val regex = ("$regexSuffix\$".toRegex())

        val similarFilesGroup = sampleFiles.groupBy { it.fileName.replace(regex, "") }

        similarFilesGroup.forEach { (_, similarFiles) ->
            val samples = similarFiles.map { it.sample }.distinct().toMutableList()

            if (samples.size == 1) {
                val sample = samples.first()
                val splitFiles = fileRepository.findAllBySample(sample).minus(similarFiles.toSet())
                if (splitFiles.isNotEmpty()) {
                    val splitSample = cloneSample(sample)
                    fileRepository.saveAll(splitFiles.map { it.sample = splitSample; it })
                }
            } else if (samples.size > 1) {
                val mergeSample = samples.removeFirst()
                samples.forEach { redundantSample ->
                    if (mergeSample == redundantSample) {
                        val files = fileRepository.findAllBySample(redundantSample)
                        files.forEach { it.sample = mergeSample }
                        fileRepository.saveAll(files)
                        sampleRepository.delete(redundantSample)
                    }
                }
            }
        }
    }

    /**
     * Clones a given sample with all its properties into a new object and saves the new Sample as well as its TechnicalSample
     *
     * @param sample Sample to be cloned
     * @return New cloned sample
     */
    fun cloneSample(sample: Sample): Sample {
        val newSample = Sample()
        BeanUtils.copyProperties(sample, newSample, "id", "uuid", "technicalSample")

        /* copy properties with dedicated setters */
        val propertiesWithPrivateSetters = Sample::class.declaredMemberProperties.filterIsInstance<KMutableProperty1<out Any, Any?>>().filter { it.javaSetter == null }
        for (property in propertiesWithPrivateSetters) {
            property.isAccessible = true
            property.setter.call(newSample, property.getter.call(sample))
            property.isAccessible = false
        }
        newSample.unknownValues = sample.unknownValues?.toMap()

        sample.technicalSample?.let {
            val newTechnicalSample = TechnicalSample()
            BeanUtils.copyProperties(it, newTechnicalSample, "id", "uuid")
            newSample.technicalSample = newTechnicalSample
            technicalSampleRepository.save(newTechnicalSample)
        }
        sampleRepository.save(newSample)
        return newSample
    }

    /**
     * Adds the project prefix to the PID of a sample and returns it.
     *
     * @param sample Sample containing the PID that should be updated with the project prefix.
     * @param projectPrefixMapping Map of all the available prefixes for the projects.
     * @return PID with the project prefix added to the front of it
     */
    private fun handlePrefix(sample: Sample, projectPrefixMapping: MutableMap<String, String?>): String {
        if (!projectPrefixMapping.containsKey(sample.project)) {
            modificationService.updateProjectPrefixesMap(sample, projectPrefixMapping)
        }
        return if (projectPrefixMapping[sample.project] == null || sample.pid.startsWith(projectPrefixMapping[sample.project].toString())) {
            sample.pid
        } else {
            projectPrefixMapping[sample.project].toString() + sample.pid
        }
    }

    fun handleRequestedValues(properties: Set<KMutableProperty0<String>>, submission: Submission) {
        properties.forEach { property ->
            val propertyList = if (property.name == Sample::speciesWithStrain.name) property.get().split("+") else listOf(property.get())
            propertyList.forEach {
                if (it.endsWith("(ReqVal)")) {
                    val className = (property as CallableReference).owner.toString().split(".").last()
                    requestedValueService.saveRequestedValue(property.name, className, it.removeSuffix("(ReqVal)"), submission)
                }
            }
            property.set(property.get().replace("(ReqVal)", ""))
        }
    }

    /**
     * If a temporary sequencing type is used in a submission, call `requestedValueService.saveSeqTypeRequestedValue`
     * to request said seqType and send an E-Mail to the data managers.
     *
     * @param property the seqType property of a sample to check if it is a seqType that should be requested
     * @param submission the submission in which the new requested seqType is used
     */
    fun handleRequestedSeqTypes(seqType: SeqType?, submission: Submission) {
        if (seqType != null && seqType.isRequested) {
            requestedValueService.saveSeqTypeRequestedValue(seqType, submission)
        }
    }

    /**
     * Delete the temporary `seqType` objects that might have been created by the user inside the GUI
     * but haven't been selected in the dropdown and haven't been saved.
     *
     * Because of that, these `seqType` objects have no corresponding `seqTypeRequestedValue` object and
     * are no longer needed.
     */
    fun deleteNotNeededSeqTypeRequests() {
        val seqTypes = seqTypeRepository.findAllByIsRequestedIsTrue()
        val requestedSeqTypes = seqTypeRequestedValuesRepository.findAllByRequestedSeqType_IsRequestedIsTrue().map { it.requestedSeqType }.toSet()

        seqTypeRepository.deleteAll(seqTypes.minus(requestedSeqTypes))
    }

    override fun deletedFilesAndSamples(submission: Submission) {
        val samples = sampleRepository.findAllBySubmission(submission)
        fileRepository.deleteAll(fileRepository.findAllBySampleIn(samples).filter { it.deletionFlag })
        sampleRepository.deleteAll(samples.filter { it.deletionFlag })
    }

    @Throws(ParseException::class)
    override fun convertToEntity(sampleGuiDto: SampleGuiDto): Sample {
        val sample = if (sampleGuiDto.id == 0) {
            Sample()
        } else {
            sampleRepository.getOne(sampleGuiDto.id)
        }
        sample.deletionFlag = false
        SampleGuiDto::class.memberProperties.forEach { property ->
            val sampleProperty = Sample::class.memberProperties.filterIsInstance<KMutableProperty<*>>().find { it.name == property.name } ?: return@forEach
            sampleProperty.findAnnotation<PrivateSetter>()?.let { annotation ->
                val setter = Sample::class.functions.find { it.name == annotation.name }
                setter?.call(sample, property.get(sampleGuiDto).toString())
                return@forEach
            }
            sampleProperty.setter.call(sample, property.get(sampleGuiDto))
        }
        return sample
    }

    override fun getSimilarPids(pid: String, project: String): Set<Map<String, String>> {
        return externalMetadataSourceService.getValuesAsSetMap(
            "similar-pids",
            mapOf(
                "project" to project,
                "pid" to pid,
                "threshold" to "0.3",
                "limit" to "10",
            )
        )
    }

    override fun checkIfSamePidIsAvailable(pid: String, project: String): Pair<String, String?>? {
        val nearlyIdenticalPid = getSimilarPids(pid, project).filter { it["similarity_num"] == "1" }
        if (nearlyIdenticalPid.isNotEmpty() && nearlyIdenticalPid.all { it["pid"] != pid }) {
            val regex = pid.replace("[^A-Za-z0-9]".toRegex(), "[^A-Za-z0-9]").toRegex(RegexOption.IGNORE_CASE)
            nearlyIdenticalPid.firstOrNull { it["pid"]?.matches(regex) == true }?.let { return "danger" to it["pid"] }
            return "warning" to nearlyIdenticalPid.first()["pid"]
        }
        return null
    }
}
