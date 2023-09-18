package de.dkfz.odcf.guide.helperObjects.importObjects

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class IlseSampleImportObject {

    var submission_id: String = ""
        set(value) {
            field = checkString(value)
        }

    var submission_type: String = ""
        set(value) {
            field = checkString(value)
        }

    var submitter: String = ""
        set(value) {
            field = checkString(value)
        }

    var sequencing_type: String = ""
        set(value) {
            field = checkString(value)
        }

    var read_1_length: Long = -1

    var read_2_length: Long = -1

    var lanes: Double = 0.0

    var iag_id: String = ""
        set(value) {
            field = checkString(value)
        }

    var asid: String = ""
        set(value) {
            field = checkString(value)
        }

    var sampleName: String = ""
        set(value) {
            field = checkString(value)
        }

    var pseudonym: String = ""
        set(value) {
            field = checkString(value)
        }

    var sex: String = ""
        set(value) {
            field = checkString(value)
        }

    var tissue: String = ""
        set(value) {
            field = checkString(value)
        }

    var odcf_project: String = ""
        set(value) {
            field = checkString(value)
        }

    var odcf_comment: String = ""
        set(value) {
            field = checkString(value)
        }

    var odcf_custom_name: String = ""
        set(value) {
            field = checkString(value)
        }

    var odcf_single_cell_well_label: String = ""
        set(value) {
            field = checkString(value)
        }

    var type: String = ""
        set(value) {
            field = checkString(value)
        }

    var isTagmentation: Boolean = false

    var antibody_target: String = ""
        set(value) {
            field = checkString(value)
        }

    var species: String = ""
        set(value) {
            field = checkString(value)
        }

    var base_material: String = ""
        set(value) {
            field = checkString(value)
        }

    var libprepKit: String = ""
        set(value) {
            field = checkString(value)
        }

    var indexType: String = ""
        set(value) {
            field = checkString(value)
        }

    var protocol: String = ""
        set(value) {
            field = checkString(value)
        }

    var proceed: String = ""
        set(value) {
            field = checkString(value)
        }

    var fasttrack: Boolean = false

    var status: String = ""

    @JsonAnySetter
    @get:JsonAnyGetter
    val unknown = HashMap<String, Any>()

    private fun checkString(value: String): String {
        return value.replace(Regex("^null$", RegexOption.IGNORE_CASE), "").trim()
    }
}
