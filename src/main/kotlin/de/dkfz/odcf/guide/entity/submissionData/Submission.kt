package de.dkfz.odcf.guide.entity.submissionData

import com.fasterxml.jackson.annotation.JsonIgnore
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.basic.GuideEntity
import de.dkfz.odcf.guide.entity.validation.ValidationLevel
import java.util.*
import javax.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "submission_type", discriminatorType = DiscriminatorType.STRING)
abstract class Submission() : GuideEntity() {

    constructor(identifier: String, uuid: UUID, ticketNumber: String, type: String?) : this() {
        this.identifier = identifier
        this.uuid = uuid
        this.ticketNumber = ticketNumber
        setType(type)
    }

    constructor(identifier: String, uuid: UUID, ticketNumber: String, submitter: Person, type: String?) :
        this(identifier, uuid, ticketNumber, type) {
            this.submitter = submitter
        }

    @Enumerated(EnumType.STRING)
    open var status = Status.IMPORTED

    open var importedExternal = false

    @Enumerated(EnumType.STRING)
    open var terminationState = TerminationState.NONE

    @Id
    open lateinit var identifier: String

    @Enumerated(EnumType.STRING)
    open var type: SubmissionType = SubmissionType.UNKNOWN

    open var lockDate: Date? = null

    open var lockUser: String? = null

    open var closedDate: Date? = null

    open var closedUser: String? = null

    open var exportDate: Date? = null

    open var importDate: Date? = null

    open var terminateDate: Date? = null

    open var finishedExternallyDate: Date? = null

    open var removalUser: String? = null

    open var removalDate: Date? = null

    open var ticketNumber: String = ""

    abstract var resettable: Boolean

    open var originProjects: String = ""

    open var externalDataAvailableForMerging: Boolean = false

    open var externalDataAvailabilityDate: Date? = null

    open var startTerminationPeriod: Date? = null

    open var onHoldComment: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    open lateinit var submitter: Person

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "submission")
    open var samples: List<Sample> = emptyList() // to avoid an UnsupportedOperationException, this collection must be a list and not a set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    open lateinit var validationLevel: ValidationLevel

    /*================================================================================================================*/

    val isActive: Boolean
        get() = Status.filterByGroup("active").contains(status)

    val isFinished: Boolean
        get() = Status.filterByGroup("finished").contains(status)

    val isDiscontinued: Boolean
        get() = Status.filterByGroup("discontinued").contains(status)

    val isWriteProtected: Boolean
        get() = isFinished || isDiscontinued || isValidated

    val isLocked: Boolean
        get() = status == Status.LOCKED

    val isOnHold: Boolean
        get() = status == Status.ON_HOLD

    val isTerminated: Boolean
        get() = status == Status.TERMINATED

    val isFinishedExternally: Boolean
        get() = status == Status.FINISHED_EXTERNALLY

    val isValidated: Boolean
        get() = status == Status.VALIDATED

    val isEdited: Boolean
        get() = status == Status.EDITED

    val isRemovedByAdmin: Boolean
        get() = status == Status.REMOVED_BY_ADMIN

    val isAutoClosed: Boolean
        get() = status == Status.AUTO_CLOSED

    val isNotifiedForTermination: Boolean
        get() = terminationState == TerminationState.NOTIFIED

    val isExtended: Boolean
        get() = this is UploadSubmission

    val isApiSubmission: Boolean
        get() = this is ApiSubmission

    val hasSubmissionTypeSamples: Boolean
        get() = type == SubmissionType.SAMPLES

    val originProjectsSet: Set<String>
        get() {
            return originProjects.split(";").toSet()
        }

    val formattedImportDate: String
        get() = getFormattedDate(importDate)

    open val formattedClosedDate: String
        get() = getFormattedDate(closedDate)

    val formattedLockDate: String
        get() = getFormattedDate(lockDate)

    val projects: Set<String>
        get() = samples.map { it.project }.filter { it.isNotBlank() }.toSet()

    val ownTransfer: Boolean
        get() = this is ApiSubmission && this.sequencingTechnology.checkExternalMetadataSource.not()

    /*================================================================================================================*/

    fun setType(type: String?) {
        if (type != null) {
            when (type.lowercase()) {
                "multiplex" -> {
                    this.type = SubmissionType.MULTIPLEX
                    return
                }
                "samples" -> {
                    this.type = SubmissionType.SAMPLES
                    return
                }
                "libraries" -> {
                    this.type = SubmissionType.LIBRARIES
                    return
                }
            }
        }
    }

    enum class Status(val group: String?, val samplesCorrectable: Boolean) {
        IMPORTED("active", true),
        RESET("active", true),
        ON_HOLD("paused", true),
        LOCKED("active", true),
        UNLOCKED("active", true),
        EDITED("active", true),
        VALIDATED("active", false),
        CLOSED("finished", false),
        AUTO_CLOSED("finished", false),
        EXPORTED("finished", false),
        FINISHED_EXTERNALLY("discontinued", false),
        TERMINATED("discontinued", false),
        REMOVED_BY_ADMIN("discontinued", false);

        companion object {
            fun filterByGroup(group: String) = Status.values().filter { it.group == group }
            fun filterBySampleIsCorrectable() = Status.values().filter { it.samplesCorrectable }
        }
    }

    enum class TerminationState {
        NONE,
        NOTIFIED,
        PREVENT_TERMINATION
    }

    enum class SubmissionType {
        MULTIPLEX, SAMPLES, LIBRARIES, UNKNOWN
    }
}
