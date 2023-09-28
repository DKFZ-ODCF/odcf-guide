package de.dkfz.odcf.guide.entity.submissionData

import com.fasterxml.jackson.annotation.JsonIgnore
import de.dkfz.odcf.guide.annotation.ReflectionDelimiter
import de.dkfz.odcf.guide.entity.basic.GuideEntity
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.helperObjects.SampleTsvMapping
import de.dkfz.odcf.guide.helperObjects.toBool
import javax.persistence.*

@Entity
class Sample() : GuideEntity() {

    constructor(submission: Submission) : this() {
        this.submission = submission
    }

    constructor(name: String, sample: Sample) : this() {
        this.name = name
        this.project = sample.project
        this.pid = sample.pid
        this.sampleType = sample.sampleType
        this.xenograft = sample.xenograft
        this.seqType = sample.seqType
        this.libraryLayout = sample.libraryLayout
        this.tagmentationLibrary = sample.tagmentationLibrary
        this.antibodyTarget = sample.antibodyTarget
        this.singleCellPlate = sample.singleCellPlate
        this.singleCellWellPosition = sample.singleCellWellPosition
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0

    var name: String = ""

    var antibodyTarget: String = ""

    var abstractSampleId: String = ""

    var antibody: String = ""

    var comment: String = ""

    var libraryPreparationKit: String = ""

    var phenotype: String = ""

    var pid: String = ""

    var project: String = ""

    @ReflectionDelimiter("+")
    var speciesWithStrain: String = ""

    var tagmentationLibrary: String = ""

    var xenograft: Boolean = false
        private set

    var sampleTypeCategory: String = ""

    @JsonIgnore
    var singleCellPlate: String = ""

    @JsonIgnore
    var singleCellWellPosition: String = ""

    var tissue: String = ""

    @JsonIgnore
    var requestedSequencingInfo: String = ""

    var baseMaterial: String = ""
        get() {
            return if (seqType != null) {
                ("Single-cell ".takeIf { singleCell } ?: "") + seqType!!.basicSeqType
            } else ""
        }

    @JsonIgnore
    var requestedLanes: Double = 0.0

    @JsonIgnore
    var read1Length: Long = -1

    @JsonIgnore
    var read2Length: Long = -1

    var indexType: String = ""

    @JsonIgnore
    var protocol: String = ""

    @JsonIgnore
    var parseIdentifier: String = ""
        set(value) {
            field = value.ifEmpty { name }
        }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    lateinit var submission: Submission

    @OneToOne(orphanRemoval = true)
    @JoinColumn
    var technicalSample: TechnicalSample? = null

    @Enumerated(EnumType.STRING)
    var sex: Sex = Sex.UNKNOWN
        private set

    @Enumerated(EnumType.STRING)
    var proceed: Proceed = Proceed.UNKNOWN
        private set

    var sampleType: String = ""
        set(sampleType) {
            field = sampleType.replace("_".toRegex(), "-").lowercase()
        }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    var seqType: SeqType? = null

    var lowCoverageRequested: Boolean = false

    @Enumerated(EnumType.STRING)
    var libraryLayout: LibraryLayout? = null
        private set

    @ElementCollection
    var unknownValues: Map<String, String>? = null

    /*================================================================================================================*/

    val singleCell: Boolean
        get() = seqType?.singleCell ?: false

    val sampleTypeReflectingXenograft: String
        get() {
            return if (xenograft) {
                "$sampleType-x"
            } else {
                sampleType
            }
        }

    val isMergeSample: Boolean
        get() = name == SAMPLE_FROM_OTP || name.startsWith(SAMPLE_FROM_ANOTHER_SUBMISSION)

    val isExternalWithdrawnSample: Boolean
        get() = name == WITHDRAWN_SAMPLE_FROM_OTP

    val importIdentifier: String
        get() {
            if (name.contains(SAMPLE_FROM_OTP) || name.startsWith(SAMPLE_FROM_ANOTHER_SUBMISSION)) return ""
            var s = "[" + project +
                "][" + pid +
                "][" + sampleTypeReflectingXenograft +
                "]["
            s += when {
                submission.identifier.startsWith("i") -> {
                    submission.identifier.substring(1).toInt().toString() + "-"
                }
                else -> {
                    submission.identifier + "-"
                }
            }
            return "$s$name]"
        }

    val singleCellWellLabel: String
        get() = listOf(singleCellPlate, singleCellWellPosition).filter { it.isNotBlank() }.joinToString(separator = "-")

    val xenograftDisplayText: String
        get() = if (!isMergeSample) xenograft.toString() else ""

    @get:JsonIgnore
    val getMergingFieldData: Map<String, String>
        get() = mapOf(
            "project" to project,
            "pid" to pid,
            "sampleType" to sampleTypeReflectingXenograft,
            "seqType" to seqType?.name.orEmpty(),
            "libraryLayout" to libraryLayout?.name.orEmpty(),
            "singleCell" to seqType?.singleCell.toString(),
            "antibodyTarget" to antibodyTarget,
            "singleCellWellLabel" to singleCellWellLabel,
        )

    @get:JsonIgnore
    val isStopped: Boolean
        get() = proceed == Proceed.NO

    /*================================================================================================================*/

    fun setSex(sex: String?) {
        if (!sex.isNullOrEmpty()) {
            when (sex.lowercase().substring(0, 1)) {
                "f" -> {
                    this.sex = Sex.FEMALE
                    return
                }
                "m" -> {
                    this.sex = Sex.MALE
                    return
                }
                "o" -> {
                    this.sex = Sex.OTHER
                    return
                }
            }
        }
        this.sex = Sex.UNKNOWN
    }

    fun setProceed(proceed: String) {
        if (proceed.isNotEmpty()) {
            when (proceed.lowercase()) {
                "yes" -> {
                    this.proceed = Proceed.YES
                    return
                }
                "no" -> {
                    this.proceed = Proceed.NO
                    return
                }
                "-" -> {
                    this.proceed = Proceed.UNKNOWN
                    return
                }
            }
        }
        this.proceed = Proceed.UNKNOWN
    }

    fun setLibraryLayout(libraryLayout: String?) {
        if (!libraryLayout.isNullOrEmpty()) {
            when (libraryLayout.lowercase()) {
                "paired" -> {
                    this.libraryLayout = LibraryLayout.PAIRED
                    return
                }
                "single" -> {
                    this.libraryLayout = LibraryLayout.SINGLE
                    return
                }
            }
        }
    }

    fun setXenograft(xenograft: String) {
        this.xenograft = xenograft.toBool()
    }

    fun updateSampleByTsv(tsvMappingObject: SampleTsvMapping, seqType: SeqType?) {
        parseIdentifier = tsvMappingObject.parse_identifier
        project = tsvMappingObject.project
        pid = tsvMappingObject.pid
        sampleType = tsvMappingObject.sample_type
        xenograft = tsvMappingObject.xenograft.toBoolean()
        sampleTypeCategory = tsvMappingObject.sample_type_category
        setSex(tsvMappingObject.sex)
        speciesWithStrain = tsvMappingObject.species_with_strain
        phenotype = tsvMappingObject.phenotype
        this.seqType = seqType
        lowCoverageRequested = tsvMappingObject.low_coverage_requested.toBoolean()
        tagmentationLibrary = tsvMappingObject.tagmentation_library
        antibodyTarget = tsvMappingObject.antibody_target
        antibody = tsvMappingObject.antibody
        singleCellPlate = tsvMappingObject.plate
        singleCellWellPosition = tsvMappingObject.well_position
        libraryPreparationKit = tsvMappingObject.library_preparation_kit
        comment = tsvMappingObject.comment
    }

    override fun toString(): String {
        return "[pid:" + pid +
            " sampleType:" + sampleType +
            " seqType:" + (seqType?.name ?: "") +
            " project:" + project + "]"
    }

    companion object {
        const val SAMPLE_FROM_OTP = "SAMPLE FROM OTP"
        const val SAMPLE_FROM_ANOTHER_SUBMISSION = "SAMPLE(S) FROM SUBMISSION"
        const val WITHDRAWN_SAMPLE_FROM_OTP = "WITHDRAWN SAMPLE FROM OTP"
    }

    enum class Sex {
        FEMALE,
        MALE,
        UNKNOWN,
        OTHER
    }

    enum class LibraryLayout {
        SINGLE,
        PAIRED
    }

    enum class Proceed {
        YES,
        NO,
        UNKNOWN
    }

    enum class SampleTypeCategories {
        DISEASE,
        CONTROL,
        IGNORED,
        UNDEFINED
    }
}
