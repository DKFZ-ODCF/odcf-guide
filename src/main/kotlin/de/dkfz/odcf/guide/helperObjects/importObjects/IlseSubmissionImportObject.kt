package de.dkfz.odcf.guide.helperObjects.importObjects

class IlseSubmissionImportObject : SubmissionImportObject() {

    private var ilseId: Int = 0

    fun getIlseId(): String {
        return String.format("i%07d", ilseId)
    }
}
