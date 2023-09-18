package de.dkfz.odcf.guide.helperObjects.importObjects

open class SubmissionImportObject {

    var userName: String = ""

    var userMail: String = ""

    var otrsTicketNumber: String = ""

    var sequencingType: String = ""

    var libraryLayout: String = ""

    var samples: List<SampleImportObject>? = null
}
