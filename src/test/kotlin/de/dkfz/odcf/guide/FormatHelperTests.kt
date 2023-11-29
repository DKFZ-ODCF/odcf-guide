package de.dkfz.odcf.guide

import de.dkfz.odcf.guide.helperObjects.toBool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class FormatHelperTests {

    @TestFactory
    fun `test toBool`() = listOf(
        "true" to true,
        "false" to false,
        "1" to true,
        "0" to false,
        "yes" to true,
        "no" to false,
        "y" to true,
        "n" to false,
        "t" to true,
        "f" to false,
        "on" to true,
        "off" to false,
        "" to false,
        "something" to false,
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("test toBool() with $input should convert to $expected") {
            assertThat(input.toBool()).isEqualTo(expected)
        }
    }
}
