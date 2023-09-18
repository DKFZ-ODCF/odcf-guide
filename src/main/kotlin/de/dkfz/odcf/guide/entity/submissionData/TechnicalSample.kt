package de.dkfz.odcf.guide.entity.submissionData

import de.dkfz.odcf.guide.entity.basic.GuideEntity
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class TechnicalSample : GuideEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    var center: String = ""

    var externalSubmissionId: String = ""

    var barcode: String = ""

    @Transient
    var instrumentModelWithSequencingKit: String = ""
        set(value) {
            val groups = "(?<platform>[\\w_-]+) (?<model>[\\w _-]+)( \\[(?<kit>.+)])?$".toRegex().matchEntire(value)?.groups
            instrumentPlatform = groups?.get("platform")?.value?.trim() ?: ""
            instrumentModel = groups?.get("model")?.value?.trim() ?: ""
            sequencingKit = groups?.get("kit")?.value?.trim() ?: ""
            field = value
        }
        get() = field.ifBlank { "$instrumentPlatform $instrumentModel".trim() + if (sequencingKit.isNotBlank()) " [$sequencingKit]" else "" }

    var instrumentPlatform: String = ""

    var instrumentModel: String = ""

    var sequencingKit: String = ""

    var lane: Int? = null

    var pipelineVersion: String = ""

    var readCount: Int? = null

    var runDate: String = ""

    var runId: String = ""
}
