package de.dkfz.odcf.guide.helperObjects

import de.dkfz.odcf.guide.entity.submissionData.Sample

class SampleTsvMapping() {

    constructor(sample: Sample) : this() {
        sample_name = sample.name
        parse_identifier = sample.parseIdentifier
        project = sample.project
        pid = sample.pid
        sample_type = sample.sampleType
        xenograft = sample.xenograft.toString()
        sample_type_category = sample.sampleTypeCategory
        species_with_strain = sample.speciesWithStrain
        sex = sample.sex.toString()
        phenotype = sample.phenotype
        sequencing_read_type = sample.libraryLayout.toString()
        sequencing_type = sample.seqType?.name.toString()
        low_coverage_requested = sample.lowCoverageRequested.toString()
        tagmentation_library = sample.tagmentationLibrary
        antibody_target = sample.antibodyTarget
        antibody = sample.antibody
        plate = sample.singleCellPlate
        well_position = sample.singleCellWellPosition
        library_preparation_kit = sample.libraryPreparationKit
        index_type = sample.indexType
        comment = sample.comment
    }

    var sample_name: String = ""

    var parse_identifier: String = ""

    var project: String = ""

    var pid: String = ""

    var sample_type: String = ""

    var xenograft: String = ""

    var sample_type_category: String = ""

    var species_with_strain: String = ""

    var sex: String = ""

    var phenotype: String = ""

    var sequencing_read_type: String = ""

    var sequencing_type: String = ""

    var low_coverage_requested: String = ""

    var tagmentation_library: String = ""

    var antibody_target: String = ""

    var antibody: String = ""

    var plate: String = ""

    var well_position: String = ""

    var library_preparation_kit: String = ""

    var index_type: String = ""

    var comment: String = ""
}
