package de.dkfz.odcf.guide.entity

import de.dkfz.odcf.guide.helper.EntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SampleTests {

    private val entityFactory = EntityFactory()

    @Test
    fun `check toString`() {
        val sample = entityFactory.getSample()
        val seqType = sample.seqType?.name

        assertThat(sample.toString()).isEqualTo(
            "[pid:prefix_pid sampleType:sample-type01 seqType:$seqType project:project]"
        )
    }

    @Test
    fun `check singleCellWellLabel`() {
        val sample = entityFactory.getSample()

        assertThat(sample.singleCellWellLabel).isEqualTo("singleCellPlate-singleCellWellPosition")
    }

    @Test
    fun `check sampleTypeReflectingXenograft`() {
        val sample = entityFactory.getSample()
        val xenograftSample = entityFactory.getSample()
        xenograftSample.setXenograft("true")

        assertThat(sample.sampleTypeReflectingXenograft).isEqualTo("sample-type01")
        assertThat(xenograftSample.sampleTypeReflectingXenograft).isEqualTo("sample-type01-x")
    }

    @Test
    fun `check isExternalMergeSample`() {
        val sample = entityFactory.getSample()
        val externalMergeSample = entityFactory.getSample()
        externalMergeSample.name = "SAMPLE FROM OTP"

        assertThat(sample.isMergeSample).isEqualTo(false)
        assertThat(externalMergeSample.isMergeSample).isEqualTo(true)
    }

    @Test
    fun `check importIdentifier`() {
        val sample = entityFactory.getSample()
        val externalMergeSample = entityFactory.getSample()
        externalMergeSample.name = "SAMPLE FROM OTP"
        val externalWithdrawnSample = entityFactory.getSample()
        externalWithdrawnSample.name = "WITHDRAWN SAMPLE FROM OTP"

        assertThat(sample.importIdentifier).isEqualTo("[project][prefix_pid][sample-type01][${sample.submission.identifier.filter { it.isDigit() }.toInt()}-sampleIdentifier]")
        assertThat(externalMergeSample.importIdentifier).isEqualTo("")
        assertThat(externalWithdrawnSample.importIdentifier).isEqualTo("")
    }

    @Test
    fun `check inputMaterial`() {
        val sample = entityFactory.getSample()
        val sample1 = entityFactory.getSample()
        val sample2 = entityFactory.getSample()
        val sample3 = entityFactory.getSample()
        sample.baseMaterial = "testMaterial"
        sample1.baseMaterial = ""
        sample1.seqType?.singleCell = true
        sample1.seqType?.basicSeqType = "DNA"
        sample2.baseMaterial = ""
        sample2.seqType?.singleCell = false
        sample2.seqType?.basicSeqType = "RNA"
        sample3.seqType = null

        assertThat(sample.baseMaterial).isEqualTo(sample.seqType!!.basicSeqType)
        assertThat(sample1.baseMaterial).isEqualTo("Single-cell DNA")
        assertThat(sample2.baseMaterial).isEqualTo("RNA")
        assertThat(sample3.baseMaterial).isEqualTo("")
    }

    @Test
    fun `check comparison of two samples`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        val sample2 = entityFactory.getSample(submission)
        sample2.seqType = sample.seqType
        val sample3 = entityFactory.getSample(submission)
        sample3.name = "otherSample"
        val sample4 = entityFactory.getSample()

        assertThat(sample == sample2).isEqualTo(true)
        assertThat(sample == sample3).isEqualTo(false)
        assertThat(sample == sample4).isEqualTo(false)
    }

    @Test
    fun `check comparison of two samples with difference in property excluded from comparison`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        sample.unknownValues = mapOf("unknown" to "sample")
        val sample2 = entityFactory.getSample(submission)
        sample2.seqType = sample.seqType
        sample2.unknownValues = mapOf("unknown2" to "sample2")

        assertThat(sample == sample2).isEqualTo(true)
    }
}
