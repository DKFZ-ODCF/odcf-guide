package de.dkfz.odcf.guide.entity

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class BasicEntity {

    @CreationTimestamp
    open var dateCreated: Date = Date()

    @UpdateTimestamp
    open var lastUpdate: Date = Date()

    /*=======================================================================*/

    val formattedDateCreated: String
        get() = getFormattedDate(dateCreated)

    open val formattedLastUpdate: String
        get() = getFormattedDate(lastUpdate)

    fun getFormattedDate(date: Date?): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return if (date != null) format.format(date) else ""
    }
}
