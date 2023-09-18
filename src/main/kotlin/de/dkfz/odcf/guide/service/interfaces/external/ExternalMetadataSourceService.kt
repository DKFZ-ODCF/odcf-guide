package de.dkfz.odcf.guide.service.interfaces.external

interface ExternalMetadataSourceService {
    fun getSingleValue(methodName: String, params: Map<String, String> = emptyMap()): String

    fun getSetOfValues(methodName: String, params: Map<String, String> = emptyMap()): Set<String>

    fun getSetOfMapOfValues(methodName: String, params: Map<String, String> = emptyMap()): Set<Map<String, String>>
}
