package de.dkfz.odcf.guide.dtoObjects

import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.entity.submissionData.TechnicalSample

class SampleGuiDto {

    var id: Int = 0

    var name: String = ""

    var parseIdentifier: String = ""

    var project: String = ""

    var pid: String = ""

    var sampleType: String = ""

    var xenograft: Boolean = false

    var sampleTypeCategory: String = ""

    val speciesWithStrain: String
        get() = speciesWithStrainList.joinToString("+")

    var speciesWithStrainList: List<String> = emptyList()

    var sex: String = ""

    var phenotype: String = ""

    var libraryLayout: String = ""

    var singleCell: Boolean = false

    var seqType: SeqType? = null

    var lowCoverageRequested: Boolean = false

    var tagmentation: Boolean = false

    var tagmentationLibrary: String = ""

    var antibody: String = ""

    var antibodyTarget: String = ""

    var libraryPreparationKit: String = ""

    var indexType: String = ""

    var singleCellPlate: String = ""

    var singleCellWellPosition: String = ""

    var comment: String = ""

    var technicalSample: TechnicalSample? = null

    var files: List<FileGuiDto>? = null
}
