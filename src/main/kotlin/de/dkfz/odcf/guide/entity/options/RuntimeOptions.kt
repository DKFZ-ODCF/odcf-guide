package de.dkfz.odcf.guide.entity.options

import javax.persistence.Entity
import javax.persistence.Id

@Entity
class RuntimeOptions {

    @Id
    lateinit var name: String

    lateinit var value: String
}
