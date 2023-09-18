package de.dkfz.odcf.guide.entity.submissionData

import de.dkfz.odcf.guide.entity.BasicEntity
import javax.persistence.*

@Entity
class ImportSourceData() : BasicEntity() {

    constructor(submissionIdentifier: String, jsonContent: String) : this() {
        this.submissionIdentifier = submissionIdentifier
        this.jsonContent = jsonContent
    }

    @Id
    lateinit var submissionIdentifier: String

    // @Column(columnDefinition="text")
    lateinit var jsonContent: String
}
