package de.dkfz.odcf.guide.entity.parser

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
class ParserField() {

    constructor(fieldName: String) : this() {
        this.fieldName = fieldName
    }

    constructor(fieldName: String, columnMapping: String) : this(fieldName) {
        this.columnMapping = columnMapping
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    lateinit var parser: Parser

    var fieldName: String = ""

    var fieldRegex: String = ""

    var orderOfComponents: String = ""

    var columnMapping: String = ""

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parserField", orphanRemoval = true)
    var parserComponents: List<ParserComponent>? = null

    val formattedRegex: String
        get() {
            var tempFieldRegex = this.fieldRegex
            this.parserComponents?.forEach { component ->
                val matchGroupName = component.componentName.replace(" ", "")
                val matchingGroupRegex = "(?<$matchGroupName>${component.componentRegex})${"?".takeIf { component.optional }.orEmpty()}"
                if (this.fieldRegex.isNotBlank()) {
                    tempFieldRegex = tempFieldRegex.replace("[${component.componentName}]", matchingGroupRegex)
                } else {
                    tempFieldRegex += matchingGroupRegex
                }
            }
            return tempFieldRegex
        }
}
