package de.dkfz.odcf.guide.service.external

import de.dkfz.odcf.guide.ImportSourceDataRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.submissionData.ImportSourceData
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.external.IlseApiServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.IlseApiService
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import java.io.File

@SpringBootTest
class IlseApiServiceTests @Autowired constructor(val ilseService: IlseApiService) {

    private val entityFactory = EntityFactory()

    @InjectMocks
    @Spy
    lateinit var ilseApiServiceMock: IlseApiServiceImpl

    @Mock
    lateinit var env: Environment

    @Mock
    lateinit var importSourceDataRepository: ImportSourceDataRepository

    @Mock
    lateinit var jsonApiService: JsonApiService

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Test
    fun `Test getting import object returns ilse object`() {

        `when`(runtimeOptionsRepository.findByName("jsonToDB")).thenReturn(entityFactory.getRuntimeOption("true"))
        doReturn(File("testData/ilse_test.json").readText()).`when`(ilseApiServiceMock).getJsonFromApi(0)

        val ilseObject = ilseApiServiceMock.getSubmissionImportObjectFromApi(0, true)
        assertThat(ilseObject).isNotNull
        assertThat(ilseObject.samples!!.size).isEqualTo(4)
        assertThat(ilseObject.samples!![0].submission_id).isEqualTo("0")
        assertThat(ilseObject.samples!![0].submission_type).isEqualTo("multiplex")
        assertThat(ilseObject.samples!![0].submitter).isEqualTo("submitter")
        assertThat(ilseObject.samples!![0].sequencing_type).isEqualTo("NovaSeq 6000 Paired-End 100bp S1")
        assertThat(ilseObject.samples!![0].lanes).isEqualTo(4.0)
        assertThat(ilseObject.samples!![0].iag_id).isEqualTo("")
        assertThat(ilseObject.samples!![0].asid).isEqualTo("386920")
        assertThat(ilseObject.samples!![0].sampleName).isEqualTo("sample_name")
        assertThat(ilseObject.samples!![0].pseudonym).isEqualTo("pseudonym")
        assertThat(ilseObject.samples!![0].sex).isEqualTo("f")
        assertThat(ilseObject.samples!![0].tissue).isEqualTo("N")
        assertThat(ilseObject.samples!![0].odcf_project).isEqualTo("odcf_project")
        assertThat(ilseObject.samples!![0].odcf_comment).isEqualTo("")
        assertThat(ilseObject.samples!![0].odcf_custom_name).isEqualTo("")
        assertThat(ilseObject.samples!![0].odcf_single_cell_well_label).isEqualTo("")
        assertThat(ilseObject.samples!![0].type).isEqualTo("EXON")
        assertThat(ilseObject.samples!![0].isTagmentation).isEqualTo(false)
        assertThat(ilseObject.samples!![0].antibody_target).isEqualTo("")
        assertThat(ilseObject.samples!![0].species).isEqualTo("mm10")
        assertThat(ilseObject.samples!![0].base_material).isEqualTo("genomic DNA")
        assertThat(ilseObject.samples!![0].libprepKit).isEqualTo("")
        assertThat(ilseObject.samples!![0].indexType).isEqualTo("")
        assertThat(ilseObject.samples!![0].protocol).isEqualTo("Ultra Low RNA Seq")
        assertThat(ilseObject.samples!![0].proceed).isEqualTo("-")

        verify(importSourceDataRepository, times(1)).saveAndFlush(any(ImportSourceData::class.java))
    }

    @Test
    fun `Test not writing to DB when replaceExistingEntry set to false`() {

        `when`(runtimeOptionsRepository.findByName("jsonToDB")).thenReturn(entityFactory.getRuntimeOption("true"))
        doReturn(File("testData/ilse_test.json").readText()).`when`(ilseApiServiceMock).getJsonFromApi(0)

        ilseApiServiceMock.getSubmissionImportObjectFromApi(0, false)

        verify(importSourceDataRepository, times(0)).saveAndFlush(any(ImportSourceData::class.java))
    }

    @Test
    fun `Test not writing to DB when jsonToDB set to false`() {

        `when`(runtimeOptionsRepository.findByName("jsonToDB")).thenReturn(entityFactory.getRuntimeOption("false"))
        doReturn(File("testData/ilse_test.json").readText()).`when`(ilseApiServiceMock).getJsonFromApi(0)

        ilseApiServiceMock.getSubmissionImportObjectFromApi(0, true)

        verify(importSourceDataRepository, times(0)).saveAndFlush(any(ImportSourceData::class.java))
    }

    @Test
    fun `Test getting import object with wrong ilse number throws exception`() {

        doReturn(File("testData/ilse_test_wrong_ilse.json").readText()).`when`(ilseApiServiceMock).getJsonFromApi(0)

        assertThatExceptionOfType(ExternalApiReadException::class.java).isThrownBy {
            ilseApiServiceMock.getSubmissionImportObjectFromApi(0)
        }.withMessageContaining("ILSe: At least one ILSe number from API does not match with the given ILSe number '0'")
    }

    @Test
    fun `Test getting import object with missing values does not fail`() {

        doReturn(File("testData/ilse_test_missing.json").readText()).`when`(ilseApiServiceMock).getJsonFromApi(0)

        val ilseObject = ilseApiServiceMock.getSubmissionImportObjectFromApi(0)
        assertThat(ilseObject).isNotNull
        assertThat(ilseObject.samples).isNotNull
        assertThat(ilseObject.samples!![0].submission_id).isEqualTo("0")
    }

    @Test
    fun `Test getting import object with unknown value`() {

        doReturn(File("testData/ilse_test_unknown.json").readText()).`when`(ilseApiServiceMock).getJsonFromApi(0)

        val ilseObject = ilseApiServiceMock.getSubmissionImportObjectFromApi(0)
        assertThat(ilseObject).isNotNull
        assertThat(ilseObject.samples).isNotNull
        assertThat(ilseObject.samples!![0].unknown).isNotEmpty
    }

    @Test
    fun `Test getJsonFromApi throws ExternalApiReadException`() {
        assertThatExceptionOfType(ExternalApiReadException::class.java).isThrownBy {
            ilseApiServiceMock.getSubmissionImportObjectFromApi(123456)
        }
    }

    @Test
    fun `test empty json`() {
        val ilse = 123456

        doReturn("{ \"$ilse\" : [ { } ] }").`when`(ilseApiServiceMock).getJsonFromApi(ilse)

        assertThatExceptionOfType(ExternalApiReadException::class.java).isThrownBy {
            ilseApiServiceMock.getSubmissionImportObjectFromApi(ilse)
        }.withMessage("ILSe: JSON is empty")
    }
}
