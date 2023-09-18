package de.dkfz.odcf.guide.entity.submissionData

import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.entity.metadata.SequencingTechnology
import java.util.*
import javax.persistence.*

@Entity
@DiscriminatorValue("ApiSubmission")
class ApiSubmission : Submission {

    constructor() : super()

    constructor(identifier: String, uuid: UUID, ticketNumber: String, type: String?) :
        super(identifier, uuid, ticketNumber, type)

    constructor(identifier: String, uuid: UUID, ticketNumber: String, submitter: Person, type: String?) :
        super(identifier, uuid, ticketNumber, submitter, type)

    override var finishedExternallyDate: Date? = null

    override var resettable = true

    override var externalDataAvailableForMerging: Boolean = false

    override var externalDataAvailabilityDate: Date? = null

    var fasttrack: Boolean = false

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    lateinit var sequencingTechnology: SequencingTechnology

    /*================================================================================================================*/

    val formattedExtDataRecvdDate: String
        get() = getFormattedDate(externalDataAvailabilityDate)
}
