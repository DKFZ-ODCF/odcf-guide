package de.dkfz.odcf.guide.helperObjects

import de.dkfz.odcf.guide.entity.parser.Parser
import de.dkfz.odcf.guide.entity.parser.ParserField

open class ParserForm {
    var parser: Parser? = null

    var field: List<ParserField>? = null
}
