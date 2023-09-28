package de.dkfz.odcf.guide.entity.basic

import com.fasterxml.jackson.annotation.JsonIgnore
import de.dkfz.odcf.guide.entity.BasicEntity
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.MappedSuperclass
import javax.persistence.Transient

@MappedSuperclass
abstract class GuideEntity : BasicEntity() {

    @Type(type = "uuid-char")
    @Column(unique = true)
    open var uuid: UUID = UUID.randomUUID()

    @JsonIgnore
    @Transient
    var deletionFlag = true
}
