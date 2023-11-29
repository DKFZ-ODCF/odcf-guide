package de.dkfz.odcf.guide.entity

import de.dkfz.odcf.guide.helper.EntityFactory
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class ParserComponentTests {

    private val entityFactory = EntityFactory()

    @Test
    fun `check parserMapping`() {
        val parserComponent = entityFactory.getParserComponent()
        val parserMapping = mutableMapOf("T" to "Tumor", "M" to "Metastasis", "C" to "Control")

        Assertions.assertThat(parserComponent.parserMapping).isEqualTo(parserMapping)
    }

    @Test
    fun `check setParserMapping`() {
        val parserComponent = entityFactory.getParserComponent()
        parserComponent.setParserMapping(mapOf("O" to "Other"))
        val parserMapping = mutableMapOf("T" to "Tumor", "M" to "Metastasis", "C" to "Control", "O" to "Other")

        val parserComponentReplace = entityFactory.getParserComponent()
        val parserReplaceMapping = mapOf("T" to "Test", "M" to "More", "C" to "Choices")
        parserComponentReplace.setParserMapping(parserReplaceMapping)

        Assertions.assertThat(parserComponent.parserMapping).isEqualTo(parserMapping)
        Assertions.assertThat(parserComponentReplace.parserMapping).isEqualTo(parserReplaceMapping)
    }
}
