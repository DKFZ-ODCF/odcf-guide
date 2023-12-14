package de.dkfz.odcf.guide.helperObjects.enums

enum class ApiType(val adapterService: String = "") {
    ILSe,
    OTP("externalMetadataSourceService"),
    PROJECT_TARGET_SERVICE("projectTargetService"),
    ITCF,
    OTRS;
}
