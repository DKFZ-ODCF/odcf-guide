package de.dkfz.odcf.guide.entity.parser

import javax.persistence.*

@Entity
class ParserComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    lateinit var parserField: ParserField

    var componentName: String = ""

    var componentRegex: String = ""

    var numberOfDigits: Int = 1

    var parserMappingString: String = ""

    var optional: Boolean = false

    @ElementCollection
    @Transient
    var parserMappingKeys: List<String>? = null

    @ElementCollection
    @Transient
    var parserMappingValues: List<String>? = null

    /*================================================================================================================*/

    val parserMapping: MutableMap<String, String>
        get() = if (parserMappingString.isNotBlank()) {
            parserMappingString.split(";").associate { val (left, right) = it.split("="); left to right }.toMutableMap()
        } else mutableMapOf()

    fun setParserMapping(mappingElement: Map<String, String>) {
        val mappingHelper = parserMapping + mappingElement
        var s = ""
        mappingHelper.forEach { if (it.key.isNotBlank()) s += "$it;" }
        parserMappingString = s.trim(';')
    }
}
