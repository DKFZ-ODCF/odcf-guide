package de.dkfz.odcf.guide.helperObjects.enums

enum class MetaDataColumnTitles(
    val importAlias: List<String> = emptyList(),
    val exportName: String = ""
) {
    ALIGN_TOOL,
    ANTIBODY,
    ANTIBODY_TARGET,
    BASE_COUNT,
    BASE_MATERIAL,
    CENTER(listOf("CENTER_NAME"), "CENTER_NAME"),
    COMMENT,
    CYCLE_COUNT,
    FASTQ_FILE_NAME(listOf("FASTQ_FILE"), "FASTQ_FILE"),
    FASTQ_GENERATOR(listOf("BCL2FASTQ_VERSION", "PIPELINE_VERSION")),
    ILSE_NO,
    INDEX(listOf("BARCODE")),
    INSERT_SIZE,
    INSTRUMENT_MODEL,
    INSTRUMENT_PLATFORM,
    LANE_NO,
    LIB_PREP_KIT,
    MD5_SUM(listOf("MD5"), "MD5"),
    PATIENT_ID(listOf("PID")),
    PROJECT,
    READ_COUNT,
    READ_NUMBER(listOf("READ", "MATE"), "READ"),
    RUN_DATE,
    RUN_ID,
    SAMPLE_NAME(listOf("SAMPLE_ID")),
    SAMPLE_SUBMISSION_TYPE,
    SAMPLE_TYPE(listOf("BIOMATERIAL_ID"), "BIOMATERIAL_ID"),
    SEQUENCING_KIT,
    SEQUENCING_READ_TYPE(listOf("LIBRARY_LAYOUT")),
    SEQUENCING_TYPE,
    SEX(listOf("GENDER")),
    SINGLE_CELL,
    TAGMENTATION(listOf("TAGMENTATION_BASED_LIBRARY"), "TAGMENTATION_BASED_LIBRARY"),
    TAGMENTATION_LIBRARY(listOf("CUSTOMER_LIBRARY"), "CUSTOMER_LIBRARY"),
    SPECIES,
    PHENOTYPE;

    companion object {
        fun getColumn(value: String): MetaDataColumnTitles? = values().find { it.importAlias.contains(value) || it.name == value }
        fun getExportName(column: MetaDataColumnTitles): String = if (column.exportName.isNotBlank()) column.exportName else column.name
    }
}
