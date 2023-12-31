package de.dkfz.odcf.guide.helperObjects.enums

enum class SimplePage(val displayName: String, val hasExplanation: Boolean) {
    SAMPLE_IDENTIFIER("sampleIdentifier", true),
    PARSE_IDENTIFIER("parseIdentifier", true),
    PROJECT("project", true),
    PID("pid", true),
    SAMPLE_TYPE("sampleType", true),
    SAMPLE_TYPE_ON_FILESYSTEM("sampleTypeOnFileSystem", true),
    XENOGRAFT("xenograft", true),
    SAMPLE_TYPE_CATEGORY("sampleTypeCategory", true),
    SPECIES("speciesWithStrain", true),
    SEX("sex", true),
    PHENOTYPE("phenotype", true),
    LIBRARY_LAYOUT("libraryLayout", true),
    OTP_SEQ_TYPE("otpSeqType", true),
    LOW_COVERAGE_REQUESTED("lowCoverageRequested", false),
    TAGMENTATION_LIBRARY("tagmentationLibrary", true),
    ANTIBODY_TARGET("antibodyTarget", true),
    ANTIBODY("antibody", true),
    LIBRARY_PREPARATION_KIT("libraryPreparationKit", true),
    INDEX_TYPE("indexType", true),
    PLATE("plate", true),
    WELL_POSITION("wellPosition", true),
    COMMENT("comment", true),
}

enum class ExtendedPage(val displayName: String, val hasExplanation: Boolean, val hideClass: String) {
    SAMPLE_IDENTIFIER("sampleIdentifier", true, ""),
    PLUS("row-edit-button", false, ""),
    MINUS("row-edit-button", false, ""),
    FASTQ_FILE_NAME("file", false, ""),
    MD5("md5", false, ""),
    READ_NUMBER("readNumber", false, ""),
    BASE_COUNT("baseCount", false, "h-tSample"),
    CYCLE_COUNT("cycleCount", false, "h-tSample"),
    READ_COUNT("readCount", false, "h-tSample"),
    PARSE_IDENTIFIER("parseIdentifier", true, ""),
    PROJECT("project", true, "h-sample"),
    PID("pid", true, "h-sample"),
    SAMPLE_TYPE("sampleType", true, "h-sample"),
    SAMPLE_TYPE_ON_FILESYSTEM("sampleTypeOnFileSystem", true, "h-sample"),
    XENOGRAFT("xenograft", true, "h-sample"),
    SAMPLE_TYPE_CATEGORY("sampleTypeCategory", true, "h-sample"),
    SPECIES("speciesWithStrain", true, "h-sample"),
    SEX("sex", true, "h-sample"),
    PHENOTYPE("phenotype", true, "h-sample"),
    LIBRARY_LAYOUT("libraryLayout", true, "h-sample"),
    OTP_SEQ_TYPE("otpSeqType", true, "h-sample"),
    LOW_COVERAGE_REQUESTED("lowCoverageRequested", false, "h-sample"),
    TAGMENTATION_LIBRARY("tagmentationLibrary", true, "h-sample"),
    ANTIBODY_TARGET("antibodyTarget", true, "h-sample"),
    ANTIBODY("antibody", true, "h-sample"),
    LIBRARY_PREPARATION_KIT("libraryPreparationKit", true, "h-sample"),
    INDEX_TYPE("indexType", true, "h-sample"),
    PLATE("plate", true, "h-sample"),
    WELL_POSITION("wellPosition", true, "h-sample"),
    INDEX("index", false, "h-tSample"),
    ILSE_NUMBER("ilseNumber", false, "h-tSample"),
    CENTER("center", false, "h-tSample"),
    RUN_ID("runId", true, "h-tSample"),
    RUN_DATE("runDate", true, "h-tSample"),
    LANE_NUMBER("laneNumber", false, "h-tSample"),
    INSTRUMENT_MODEL_SEQUENCING_KIT("instrumentModelSequencingKit", true, "h-tSample"),
    FASTQ_GENERATOR("fastqGenerator", true, "h-tSample"),
    COMMENT("comment", true, ""),
}
