package de.dkfz.odcf.guide.entity

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToMany

@Entity
class Person {

    @Id
    lateinit var username: String

    lateinit var lastName: String

    lateinit var firstName: String

    lateinit var mail: String

    lateinit var department: String

    lateinit var organizationalUnit: String

    var accountDisabled: Boolean = false

    var unreadNews: Boolean = false

    @Column(nullable = false)
    var apiToken: String = getToken()

    var isAdmin: Boolean = false

    var isOrganizationalUnitLeader: Boolean = false

    var acceptedDataSecurityStatementDate: Date? = null

    @ManyToMany
    var roles: Set<Role> = emptySet()

    val fullName: String
        get() = "$firstName $lastName"

    val acceptedDataSecurityStatement: Boolean
        get() = acceptedDataSecurityStatementDate != null

    private fun getToken(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..32)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}
