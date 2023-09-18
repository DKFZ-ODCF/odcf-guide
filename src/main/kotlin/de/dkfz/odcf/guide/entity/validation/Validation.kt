package de.dkfz.odcf.guide.entity.validation

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0

    lateinit var field: String

    lateinit var regex: String

    var required: Boolean = false

    lateinit var description: String
}
