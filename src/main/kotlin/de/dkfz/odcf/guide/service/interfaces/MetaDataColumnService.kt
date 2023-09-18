package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.MetaDataColumn
import de.dkfz.odcf.guide.exceptions.ColumnNotFoundException

interface MetaDataColumnService {

    /**
     * Searches for a MetaDataColumn object for a given name or import alias.
     *
     * @param key Name or import alias for which a column should be found.
     * @return MetaDataColumn object, if it was found.
     */
    fun getColumn(key: String): MetaDataColumn?

    @Throws(ColumnNotFoundException::class)
    fun getValue(key: String, row: Map<String, String>): String

    @Throws(ColumnNotFoundException::class)
    fun removeValueOrThrowException(key: String, row: MutableMap<String, String>): String

    fun removeValueOrEmptyValue(key: String, row: MutableMap<String, String>): String
}
