package de.dkfz.odcf.guide

import de.dkfz.odcf.guide.helperObjects.encodeUtf8
import de.dkfz.odcf.guide.helperObjects.toBool
import de.dkfz.odcf.guide.helperObjects.toKebabCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CustomFunctionsTests {

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

    @TestFactory
    fun `test toKebabCase`() = listOf(
        "kebabCase" to "kebab-case",
        "KebabCase" to "kebab-case",
        "KebabCAse" to "kebab-case",
        "kebab_case" to "kebab-case",
        "kebab_Case" to "kebab-case",
        "kebab_CAse" to "kebab-case",
        "kebab-case" to "kebab-case",
        "kebab-Case" to "kebab-case",
        "kebab-CAse" to "kebab-case",
        "KEBAB_CASE" to "kebab-case",
        "KEBAB_CASE_TEST" to "kebab-case-test",
        "KEBAB-CASE-TEST" to "kebab-case-test",
        "nothing" to "nothing",
        "UPPER" to "upper",
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("test toKebabCase() with $input should convert to $expected") {
            assertThat(input.toKebabCase()).isEqualTo(expected)
        }
    }

    @TestFactory
    fun `test encodeUtf8`() = listOf(
        "#" to "%23",
        "$" to "%24",
        "&" to "%26",
        "+" to "%2B",
        "," to "%2C",
        "/" to "%2F",
        ":" to "%3A",
        ";" to "%3B",
        "=" to "%3D",
        "?" to "%3F",
        "@" to "%40",
        " " to "+",
        "ä Ä" to "%C3%A4+%C3%84",
        "ö Ö" to "%C3%B6+%C3%96",
        "ü Ü" to "%C3%BC+%C3%9C",
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("test encodeUtf8() with '$input' should convert to '$expected'") {
            assertThat(input.encodeUtf8()).isEqualTo(expected)
        }
    }
}
