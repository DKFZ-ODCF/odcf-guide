package de.dkfz.odcf.guide.entity

import javax.persistence.*

@Entity
class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    @Column(unique = true)
    lateinit var name: String
}
