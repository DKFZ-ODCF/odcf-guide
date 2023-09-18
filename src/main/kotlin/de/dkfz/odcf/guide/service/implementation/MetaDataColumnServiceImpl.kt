package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.MetaDataColumnRepository
import de.dkfz.odcf.guide.entity.MetaDataColumn
import de.dkfz.odcf.guide.exceptions.ColumnNotFoundException
import de.dkfz.odcf.guide.service.interfaces.MetaDataColumnService
import org.springframework.stereotype.Service
import java.util.*

@Service
class MetaDataColumnServiceImpl(
    private val metaDataColumnRepository: MetaDataColumnRepository
) : MetaDataColumnService {

    override fun getColumn(key: String): MetaDataColumn? {
        val formattedValue = key.lowercase().replace("_", " ")
        return metaDataColumnRepository.findAll().find { it.importAliasSet.contains(formattedValue) || it.columnName == formattedValue }
    }

    @Throws(ColumnNotFoundException::class)
    override fun getValue(key: String, row: Map<String, String>): String {
        return row.entries.find { column ->
            getColumn(key)?.importNames?.contains(column.key.lowercase().replace("_", " ")) ?: false
        }?.value ?: throw ColumnNotFoundException("Column '$key' not found.")
    }

    @Throws(ColumnNotFoundException::class)
    override fun removeValueOrThrowException(key: String, row: MutableMap<String, String>): String {
        val entry = row.entries.find { column ->
            getColumn(key)?.importNames?.contains(column.key.lowercase().replace("_", " ")) ?: false
        } ?: throw ColumnNotFoundException("Column '$key' not found.")
        return row.remove(entry.key)!!
    }

    override fun removeValueOrEmptyValue(key: String, row: MutableMap<String, String>): String {
        val entry = row.entries.find { column ->
            getColumn(key)?.importNames?.contains(column.key.lowercase().replace("_", " ")) ?: false
        } ?: return ""
        return row.remove(entry.key)!!
    }
}
