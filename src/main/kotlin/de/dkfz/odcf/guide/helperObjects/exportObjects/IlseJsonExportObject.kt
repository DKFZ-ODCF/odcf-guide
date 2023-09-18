package de.dkfz.odcf.guide.helperObjects.exportObjects

class IlseJsonExportObject : JsonExportObject() {

    private val ilseId: Int = 0

    fun getIlseId(): String {
        return String.format("i%07d", ilseId)
    }
}
