package de.dkfz.odcf.guide.entity.requestedValues

import javax.persistence.Entity

@Entity
class FieldRequestedValue() : RequestedValue() {

    lateinit var fieldName: String

    lateinit var className: String
}
