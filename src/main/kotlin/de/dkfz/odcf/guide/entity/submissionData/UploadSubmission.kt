package de.dkfz.odcf.guide.entity.submissionData

import de.dkfz.odcf.guide.entity.Person
import java.util.*
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("UploadSubmission")
class UploadSubmission : Submission {

    constructor() : super()

    constructor(identifier: String, uuid: UUID, ticketNumber: String, type: String?) :
        super(identifier, uuid, ticketNumber, type)

    constructor(identifier: String, uuid: UUID, ticketNumber: String, submitter: Person, type: String?) :
        super(identifier, uuid, ticketNumber, submitter, type)

    override var resettable = false

    var customName: String = ""

    var comment: String = ""
}
