package de.dkfz.odcf.guide.entity.basic

import de.dkfz.odcf.guide.entity.BasicEntity
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class GuideEntity : BasicEntity() {

    @Type(type = "uuid-char")
    @Column(unique = true)
    open var uuid: UUID = UUID.randomUUID()
}
