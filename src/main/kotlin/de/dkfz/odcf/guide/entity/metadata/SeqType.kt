package de.dkfz.odcf.guide.entity.metadata

import javax.persistence.*

@Entity
class SeqType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0 // will be replaced by hibernate

    @Column(unique = true)
    lateinit var name: String

    lateinit var basicSeqType: String

    var singleCell: Boolean = false

    var tagmentation: Boolean = false

    var needAntibodyTarget: Boolean = false

    var needLibPrepKit: Boolean = false

    var needSampleTypeCategory: Boolean = false

    var isDisplayedForUser: Boolean = true

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
        get() = "{" +
            "\"singleCell\":$singleCell," +
            "\"tagmentation\":$tagmentation," +
            "\"needAntibodyTarget\":$needAntibodyTarget," +
            "\"needLibPrepKit\":$needLibPrepKit," +
            "\"needSampleTypeCategory\":$needSampleTypeCategory," +
            "\"lowCoverageRequestable\":$lowCoverageRequestable," +
            "\"isRequested\":$isRequested" +
            "}"
}
