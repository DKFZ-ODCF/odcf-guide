package de.dkfz.odcf.guide.entity.parser

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@Entity
class Parser() {

    constructor(project: String, parserRegex: String) : this() {
        this.project = project
        this.parserRegex = parserRegex
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    var parserRegex: String = ""

    var project: String = ""

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parser", orphanRemoval = true)
    var parserFields: List<ParserField>? = null

    val formattedRegex: String
        get() {
            var tempRegex = "^${this.parserRegex}$"
            this.parserFields?.forEach {
                tempRegex = tempRegex.replace("[${it.columnMapping}]", it.formattedRegex)
            }
            return tempRegex
        }
}
