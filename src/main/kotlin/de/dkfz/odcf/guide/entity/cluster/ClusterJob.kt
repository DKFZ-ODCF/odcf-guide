package de.dkfz.odcf.guide.entity.cluster

import de.dkfz.odcf.guide.entity.basic.GuideEntity
import de.dkfz.odcf.guide.entity.submissionData.Submission
import java.util.*
import javax.persistence.*

@Entity
class ClusterJob() : GuideEntity() {

    constructor(submission: Submission, jobName: String, command: String, state: State, pathToLog: String, visibleForUser: Boolean) : this() {
        this.submission = submission
        this.jobName = jobName
        this.command = command
        this.pathToLog = pathToLog
        this.state = state
        this.visibleForUser = visibleForUser
    }

    constructor(job: ClusterJob) : this() {
        this.jobName = job.jobName
        this.command = job.command
        this.pathToLog = job.pathToLog
        this.submission = job.submission
        this.parentJob = job.parentJob
        this.state = State.PENDING
        this.visibleForUser = job.visibleForUser
        job.restartedJob = this
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0

    var remoteId: Int = -1

    lateinit var jobName: String

    var hostName: String = ""

    @Enumerated(EnumType.STRING)
    var state: State = State.UNKNOWN
        private set

    var startTime: Date? = null

    var endTime: Date? = null

    var exitCode: Int = -1

    lateinit var pathToLog: String

    var command: String = ""

    var visibleForUser: Boolean = true

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    lateinit var submission: Submission

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    var parentJob: ClusterJob? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    var restartedJob: ClusterJob? = null

    val startable: Boolean
        get() = parentJob == null || parentJob!!.state == State.DONE

    val printableName: String
        get() = jobName.replace(submission.identifier, "").replace("_", " ").replaceFirstChar { it.uppercase() }.trim()

    fun setState(state: String) {
        if (state.isNotBlank()) {
            this.state = State.findByLSFState(state) ?: State.UNKNOWN
            return
        }
        this.state = State.UNKNOWN
    }

    enum class State(val lsfState: String) {
        PENDING("NO_LSF_STATE"),
        SUBMITTED("PEND"),
        RUNNING("RUN"),
        DONE("DONE"),
        FAILED("EXIT"),
        UNKNOWN("");

        companion object {
            fun findByLSFState(state: String): State? = values().find { it.lsfState == state }
        }
    }
}
