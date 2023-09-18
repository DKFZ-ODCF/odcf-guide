package de.dkfz.odcf.guide.entity.submissionData

import de.dkfz.odcf.guide.entity.basic.GuideEntity
import javax.persistence.*

@Entity
class File() : GuideEntity() {

    constructor(sample: Sample) : this() {
        this.sample = sample
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    lateinit var sample: Sample

    /**name or path*/
    var fileName: String = ""

    var readNumber: String = ""

    var md5: String = ""

    var baseCount: Long? = null

    var cycleCount: Long? = null

    @Transient
    var isReadable: Boolean? = null

    fun isSampleInitialized() = ::sample.isInitialized
}
