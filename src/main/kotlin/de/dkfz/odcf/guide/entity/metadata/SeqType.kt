package de.dkfz.odcf.guide.entity.metadata

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.gson.Gson
import de.dkfz.odcf.guide.annotation.SeqTypeOptions
import de.dkfz.odcf.guide.helperObjects.toBool
import javax.persistence.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation

@Entity
class SeqType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0 // will be replaced by hibernate

    @Column(unique = true)
    lateinit var name: String

    lateinit var basicSeqType: String

    @SeqTypeOptions
    var singleCell: Boolean = false

    @SeqTypeOptions
    var tagmentation: Boolean = false

    @SeqTypeOptions
    var needAntibodyTarget: Boolean = false

    @SeqTypeOptions
    var needLibPrepKit: Boolean = false

    @SeqTypeOptions
    var needSampleTypeCategory: Boolean = false

    @SeqTypeOptions
    var isHiddenForUser: Boolean = false

    @SeqTypeOptions
    var lowCoverageRequestable: Boolean = false

    var isRequested: Boolean = false

    @ElementCollection
    @Column(unique = true)
    var importAliases: Set<String>? = null

    override fun toString(): String {
        return name // used for csv/tsv export, be aware while changing
    }

    fun getExtendedSeqTypeString(): String {
        return "$name [${basicSeqType}${" single cell".takeIf { singleCell } ?: ""}]"
    }

    fun setImportAliases(importAliases: String) {
        this.importAliases = importAliases.split(",").toMutableSet()
    }

    val json: String
        get() {
            val seqTypeOptions = this::class.declaredMemberProperties.filter { it.hasAnnotation<SeqTypeOptions>() }.associate {
                it.name to it.call(this) as Boolean
            } + mapOf("isRequested" to this.isRequested)
            return Gson().toJson(seqTypeOptions)
        }

    @get:JsonIgnore
    val seqTypeOptions: Map<String, Boolean>
        get() = this::class.declaredMemberProperties.filter { it.hasAnnotation<SeqTypeOptions>() }.associate {
            it.name to it.call(this).toString().toBool()
        }
}
