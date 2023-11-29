package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.MetaDataColumnRepository
import de.dkfz.odcf.guide.entity.MetaDataColumn
import de.dkfz.odcf.guide.exceptions.ColumnNotFoundException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.MetaDataColumnServiceImpl
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class MetaDataColumnServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var metaDataColumnServiceMock: MetaDataColumnServiceImpl

    @Mock
    lateinit var metaDataColumnRepository: MetaDataColumnRepository

    private fun getMetaDataColumn(): MetaDataColumn {
        val column = MetaDataColumn()
        column.columnName = "column name"
        column.exportName = "export name"
        column.importAliases = "alias1;alias2"
        return column
    }

    @Test
    fun `When getExportName return columnName if exportName null`() {
        val column = getMetaDataColumn()
        column.exportName = null

        assertThat(column.exportName!!).isEqualTo("COLUMN_NAME")
    }

    @Test
    fun `When getColumn by columnName find Column`() {
        val column = getMetaDataColumn()

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column))

        assertThat(column).isEqualTo(metaDataColumnServiceMock.getColumn("COLUMN_NAME"))
    }

    @Test
    fun `When getColumn by importAlias find Column`() {
        val column = getMetaDataColumn()

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column))

        assertThat(column).isEqualTo(metaDataColumnServiceMock.getColumn("alias1"))
        assertThat(column).isEqualTo(metaDataColumnServiceMock.getColumn("alias2"))
    }

    @TestFactory
    fun `Check functionality of getValue from CSV Rows`() = listOf(
        "key" to "value",
        "alias" to "value"
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when getValue from column '$input' then return '$expected'") {
            val row = mapOf("key" to "value")
            val column = entityFactory.getMetaDataColumn("key", "Sample", "key")
            column.importAliases = "alias"

            `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column))

            val result = metaDataColumnServiceMock.getValue(input, row)

            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `Check that Exception is thrown if CSV Column was not found for getValue`() {
        val row = mapOf("something else" to "value")
        val column = entityFactory.getMetaDataColumn("key", "Sample", "key")

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column))

        Assertions.assertThatExceptionOfType(ColumnNotFoundException::class.java).isThrownBy {
            metaDataColumnServiceMock.getValue("key", row)
        }.withMessage("Column 'key' not found.")
    }

    @TestFactory
    fun `Check functionality of removeValueOrThrowException`() = listOf(
        "key" to "value",
        "alias" to "value"
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when removeValueOrThrowException from column '$input' then return '$expected' and remove the entry") {
            val row = mapOf("key" to "value").toMutableMap()
            val column = entityFactory.getMetaDataColumn("key", "Sample", "key")
            column.importAliases = "alias"

            `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column))

            val result = metaDataColumnServiceMock.removeValueOrThrowException(input, row)

            assertThat(result).isEqualTo(expected)
            assertThat(row.size).isEqualTo(0)
        }
    }

    @Test
    fun `Check that Exception is thrown if CSV Column was not found for removeValueOrThrowException`() {
        val row = mapOf("something else" to "value").toMutableMap()
        val column = entityFactory.getMetaDataColumn("key", "Sample", "key")

        `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column))

        Assertions.assertThatExceptionOfType(ColumnNotFoundException::class.java).isThrownBy {
            metaDataColumnServiceMock.removeValueOrThrowException("key", row)
        }.withMessage("Column 'key' not found.")
        assertThat(row.size).isEqualTo(1)
    }

    @TestFactory
    fun `Check functionality of removeValueOrEmptyValue`() = listOf(
        "key" to setOf("value", 0),
        "alias" to setOf("value", 0),
        "something else" to setOf("", 1)
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when removeValueOrEmptyValue from column '$input' then return '${expected.first()}'") {
            val row = mapOf("key" to "value").toMutableMap()
            val column = entityFactory.getMetaDataColumn("key", "Sample", "key")
            column.importAliases = "alias"

            `when`(metaDataColumnRepository.findAll()).thenReturn(listOf(column))

            val result = metaDataColumnServiceMock.removeValueOrEmptyValue(input, row)

            assertThat(result).isEqualTo(expected.first())
            assertThat(row.size).isEqualTo(expected.last())
        }
    }
}
