package de.dkfz.odcf.guide.entity.requestedValues

import de.dkfz.odcf.guide.entity.BasicEntity
import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.submissionData.Submission
import javax.persistence.*

@MappedSuperclass
abstract class RequestedValue() : BasicEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    open var id: Int = 0

    open lateinit var requestedValue: String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    open lateinit var requester: Person

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    open lateinit var originSubmission: Submission

    @ManyToMany
    open var usedSubmissions: MutableSet<Submission> = mutableSetOf()

    open var createdValueAs: String = ""

    @Enumerated(EnumType.STRING)
    open var state: State = State.REQUESTED

    /*==============================================================================================================*/

    fun setStateByString(state: String) {
        this.state = State.findByName(state) ?: State.REQUESTED
    }

    val isFinished: Boolean
        get() = State.values().filter { it.process == "finished" }.contains(state)

    enum class State(val process: String) {
        REQUESTED("active"),
        ACCEPTED("finished"),
        REJECTED("finished");

        companion object {
            fun findByName(name: String): State? = State.values().find { it.name.lowercase() == name.lowercase() }
        }
    }
}
