package de.dkfz.odcf.guide.entity

import javax.persistence.*

@Entity
class MetaDataColumn {

    @Id
    lateinit var columnName: String

    var reflectionClassName: String = ""

    var reflectionPropertyNameImport: String = ""

    var reflectionPropertyNameExport: String = ""

    var importAliases: String = ""

    var exportName: String? = null
        get() = field ?: columnName.uppercase().replace(" ", "_")

    @Column(unique = true)
    var columnOrder: Int = 0

    val importNames: Set<String>
        get() = importAliasSet.plus(columnName)

    val importAliasSet: Set<String>
        get() = if (importAliases.isBlank()) { emptySet() } else importAliases.split(";").toSet()
}
