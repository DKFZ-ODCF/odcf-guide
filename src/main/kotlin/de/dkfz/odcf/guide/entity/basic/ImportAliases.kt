package de.dkfz.odcf.guide.entity.basic

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class ImportAliases {

    var importAliasesString: String = ""
        set(value) {
            field = value.removeSuffix(",")
        }

    val importAliases: Set<String>
        get() {
            return if (importAliasesString.isBlank()) {
                emptySet()
            } else {
                importAliasesString.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            }
        }
}
