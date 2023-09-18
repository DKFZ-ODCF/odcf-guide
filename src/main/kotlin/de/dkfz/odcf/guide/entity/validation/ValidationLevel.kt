package de.dkfz.odcf.guide.entity.validation

import javax.persistence.*

@Entity
class ValidationLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0

    lateinit var name: String

    var defaultObject: Boolean = false

    @ManyToMany
    var fields: Set<Validation> = emptySet()
}
