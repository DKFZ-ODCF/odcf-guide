package de.dkfz.odcf.guide.entity

import de.dkfz.odcf.guide.helper.EntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SeqTypeTests {

    private val entityFactory = EntityFactory()

    @Test
    fun `check json`() {
        val seqType = entityFactory.getSeqType()
        seqType.singleCell = true
        seqType.tagmentation = false
        seqType.needAntibodyTarget = true
        seqType.needLibPrepKit = false
        seqType.needSampleTypeCategory = true
        seqType.isHiddenForUser = true
        seqType.lowCoverageRequestable = false
        seqType.isRequested = false

        assertThat(seqType.json).isEqualTo(
            """{"isHiddenForUser":true,"lowCoverageRequestable":false,"needAntibodyTarget":true,"needLibPrepKit":false,"needSampleTypeCategory":true,"singleCell":true,"tagmentation":false,"isRequested":false}"""
        )
    }

    @Test
    fun `check seqTypeOptions`() {
        val seqType = entityFactory.getSeqType()
        seqType.singleCell = true
        seqType.tagmentation = false
        seqType.needAntibodyTarget = true
        seqType.needLibPrepKit = false
        seqType.needSampleTypeCategory = true
        seqType.isHiddenForUser = false
        seqType.lowCoverageRequestable = false
        seqType.isRequested = false

        assertThat(seqType.seqTypeOptions).isEqualTo(
            mapOf(
                "singleCell" to true,
                "tagmentation" to false,
                "needAntibodyTarget" to true,
                "needLibPrepKit" to false,
                "needSampleTypeCategory" to true,
                "isHiddenForUser" to false,
                "lowCoverageRequestable" to false,
            )
        )
    }
}
