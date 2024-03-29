package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.SeqTypeRepository
import de.dkfz.odcf.guide.exceptions.DuplicatedImportAliasException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.SeqTypeMappingServiceImpl
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class SeqTypeMappingServiceTests {

    @InjectMocks
    lateinit var seqTypeMappingServiceMock: SeqTypeMappingServiceImpl

    @Mock
    lateinit var seqTypeRepository: SeqTypeRepository

    private val entityFactory = EntityFactory()

    @Test
    fun `When getColumn by columnName find Column`() {
        val seqType = entityFactory.getSeqType()

        `when`(seqTypeRepository.findAll()).thenReturn(listOf(seqType))

        assertThat(seqType).isEqualTo(seqTypeMappingServiceMock.getSeqType(seqType.name))
    }

    @Test
    fun `When getColumn by importAlias find Column`() {
        val seqType = entityFactory.getSeqType("seqType", "ilseName1,ilseName2")

        `when`(seqTypeRepository.findAll()).thenReturn(listOf(seqType))

        assertThat(seqType).isEqualTo(seqTypeMappingServiceMock.getSeqType("ilseName1"))
        assertThat(seqType).isEqualTo(seqTypeMappingServiceMock.getSeqType("ilseName2"))
    }

    @Test
    fun `test saveSeqType`() {
        val name = "seqType"
        val basicSeqType = "RNA"
        val importAliases = "ilseName1,ilseName2"

        val seqType = seqTypeMappingServiceMock.saveSeqType(
            name,
            null,
            basicSeqType,
            importAliases,
            "needAntibodyTarget, needLibPrepKit, singleCell, tagmentation"
        )

        assertThat(seqType.name).isEqualTo("seqType")
        assertThat(seqType.basicSeqType).isEqualTo(basicSeqType)
        assertThat(seqType.importAliases).isEqualTo(importAliases.split(",").toSet())
        assertThat(seqType.needAntibodyTarget).isEqualTo(true)
        assertThat(seqType.needLibPrepKit).isEqualTo(true)
        assertThat(seqType.needSampleTypeCategory).isEqualTo(false)
        assertThat(seqType.singleCell).isEqualTo(true)
        assertThat(seqType.tagmentation).isEqualTo(true)
        assertThat(seqType.lowCoverageRequestable).isEqualTo(false)
        assertThat(seqType.isHiddenForUser).isEqualTo(false)
        assertThat(seqType.isRequested).isEqualTo(false)
    }

    @Test
    fun `test update SeqType with no importAlias and new name`() {
        var seqType = entityFactory.getSeqType("oldSeqTypeName", "oldImportAlias")
        val name = "newSeqTypeName"
        val basicSeqType = "RNA"
        val importAlias = ""

        `when`(seqTypeRepository.getOne(1)).thenReturn(seqType)

        seqType = seqTypeMappingServiceMock.saveSeqType(
            name,
            1,
            basicSeqType,
            importAlias,
            "needAntibodyTarget, needLibPrepKit, singleCell, tagmentation"
        )

        assertThat(seqType.name).isEqualTo("newSeqTypeName")
        assertThat(seqType.basicSeqType).isEqualTo(basicSeqType)
        assertThat(seqType.importAliases).isEqualTo(null)
        assertThat(seqType.needAntibodyTarget).isEqualTo(true)
        assertThat(seqType.needLibPrepKit).isEqualTo(true)
        assertThat(seqType.singleCell).isEqualTo(true)
        assertThat(seqType.tagmentation).isEqualTo(true)
        assertThat(seqType.lowCoverageRequestable).isEqualTo(false)
        assertThat(seqType.isHiddenForUser).isEqualTo(false)
    }

    @Test
    fun `When saveSeqType but IlseNames already exist throw Exception`() {
        val seqType1 = entityFactory.getSeqType("seqType1", "ilseName1,ilseName2")
        val basicSeqType = "RNA"
        val name = "seqType"
        val importAlias = "ilseName1"

        `when`(seqTypeRepository.findAll()).thenReturn(listOf(seqType1))

        Assertions.assertThatExceptionOfType(DuplicatedImportAliasException::class.java).isThrownBy {
            seqTypeMappingServiceMock.saveSeqType(
                name,
                null,
                basicSeqType,
                importAlias,
                "needAntibodyTarget, needLibPrepKit, singleCell, tagmentation"
            )
        }.withMessage("This ILSe Name already exists for a different Seq Type, please choose something else.")
    }
}
