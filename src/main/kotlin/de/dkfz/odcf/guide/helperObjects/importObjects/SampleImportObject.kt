package de.dkfz.odcf.guide.helperObjects.importObjects

import de.dkfz.odcf.guide.entity.submissionData.Submission

class SampleImportObject {

    var pid: String = ""
    var sex: String = ""
    var sampleType: String = ""
    var sampleIdentifier: String = ""
    var project: String = ""
    var seqType: String = ""
    var libraryLayout: String = ""
    var isSingleCell: Boolean = false
    var isTagmentation: Boolean = false
    var tagmentationLibrary: String = ""
    var antibodyTarget: String = ""

    lateinit var submission: Submission
}
