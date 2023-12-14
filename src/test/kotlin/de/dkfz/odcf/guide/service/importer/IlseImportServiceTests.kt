package de.dkfz.odcf.guide.service.importer

import de.dkfz.odcf.guide.*
import de.dkfz.odcf.guide.entity.submissionData.ApiSubmission
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.ExternalApiReadException
import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.helperObjects.importObjects.ExternalIlseSubmissionImportObject
import de.dkfz.odcf.guide.service.implementation.importer.IlseImportServiceImpl
import de.dkfz.odcf.guide.service.interfaces.PseudonymService
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.SequencingTechnologyService
import de.dkfz.odcf.guide.service.interfaces.SpeciesService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.external.IlseApiService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import de.dkfz.odcf.guide.service.interfaces.validator.SubmissionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Spy
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class)
class IlseImportServiceTests : AnyObject {

    @InjectMocks
    @Spy
    lateinit var ilseImportServiceMock: IlseImportServiceImpl

    @Mock
    lateinit var importService: ImportService

    @Mock
    lateinit var seqTypeMappingService: SeqTypeMappingService

    @Mock
    lateinit var ldapService: LdapService

    @Mock
    lateinit var ilseService: IlseApiService

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var submissionService: SubmissionService

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var pseudonymService: PseudonymService

    @Mock
    lateinit var speciesService: SpeciesService

    @Mock
    lateinit var sequencingTechnologyService: SequencingTechnologyService

    @Mock
    lateinit var collectorService: CollectorService

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Mock
    lateinit var submissionRepository: SubmissionRepository

    @Mock
    lateinit var runtimeOptionsRepository: RuntimeOptionsRepository

    @Mock
    lateinit var sequencingTechnologyRepository: SequencingTechnologyRepository

    @Mock
    lateinit var validationLevelRepository: ValidationLevelRepository

    private val entityFactory = EntityFactory()

    private var mailBundle = ResourceBundle.getBundle("mails", Locale.getDefault())

    @Test
    fun `When detectUnknownFields with unknown field send mail`() {
        val ilseObject = entityFactory.getExternalIlseSubmissionImportObjectWithUnknownValue()

        ilseImportServiceMock.detectUnknownFields(ilseObject, 0)

        verify(mailSenderService, times(1)).sendMailToTicketSystem(startsWith("Unknown field detected for S#0"), anyString())
    }

    @TestFactory
    fun `check detectUnknownValues functionality for species`() = listOf(
        "Unknown Species [strange unknown strain]" to "Unknown Species [strange unknown strain]",
        "Other Species (please use remarks)" to null,
        "Human (Homo sapiens) [No strain available]" to null
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("check species checked correctly for '$input'") {
            val ilseObject = entityFactory.getIlseSampleImportObject()
            val seqType = entityFactory.getSeqType()
            ilseObject.species = input

            `when`(seqTypeMappingService.getSeqType(ilseObject.type)).thenReturn(seqType)
            `when`(externalMetadataSourceService.getValuesAsSet("projectsWithAliases")).thenReturn(setOf(ilseObject.odcf_project))
            `when`(runtimeOptionsRepository.findByName("unregisteredButAcceptedForImportSpecies")).thenReturn(entityFactory.getRuntimeOption("unregisteredButAcceptedForImportSpecies", "Other Species (please use remarks)"))
            `when`(speciesService.getSpeciesForImport()).thenReturn(listOf("Human (Homo sapiens) [No strain available]"))

            val unknown = emptyMap<String, MutableSet<String>>().toMutableMap()
            ilseImportServiceMock.detectUnknownValues(ilseObject, unknown)

            assertThat(unknown["species"]?.joinToString()).isEqualTo(expected)
        }
    }

    @TestFactory
    fun `check detectUnknownValues functionality for seqType`() = listOf(
        "DNA" to "seqType, DNA",
        "RNA" to "RNA",
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("check species checked correctly for '$input'") {
            val ilseObject = entityFactory.getIlseSampleImportObject()
            ilseObject.type = input

            `when`(seqTypeMappingService.getSeqType(ilseObject.type)).thenReturn(null)
            `when`(externalMetadataSourceService.getValuesAsSet("projectsWithAliases")).thenReturn(setOf("project"))
            `when`(runtimeOptionsRepository.findByName("unregisteredButAcceptedForImportSpecies")).thenReturn(entityFactory.getRuntimeOption("unregisteredButAcceptedForImportSpecies", "Other Species (please use remarks)"))
            `when`(speciesService.getSpeciesForImport()).thenReturn(listOf("Human (Homo sapiens) [No strain available]"))

            val unknown = mapOf(
                "seqType" to setOf("seqType").takeIf { input == "DNA" }.orEmpty().toMutableSet(),
                "species" to setOf("Unknown Species [strange unknown strain]").toMutableSet()
            ).toMutableMap()
            ilseImportServiceMock.detectUnknownValues(ilseObject, unknown)

            assertThat(unknown["seqType"]?.joinToString()).isEqualTo(expected)
        }
    }

    @Test
    fun `check saveSubmission functionality with no samples`() {
        val importObject = ExternalIlseSubmissionImportObject()

        val submission = ilseImportServiceMock.saveSubmission("i0000001", importObject, "ticketNumber")

        verify(submissionRepository).saveAndFlush(submission)
    }

    @Test
    fun `check saveSubmission functionality with samples`() {
        val importObject = entityFactory.getExternalIlseSubmissionImportObjectWithUnknownValue()
        val submitter = entityFactory.getPerson()
        val seqType = entityFactory.getSeqType()
        val seqTech = entityFactory.getSeqTech()

        `when`(ldapService.getPersonByUsername(anyString())).thenReturn(submitter)
        `when`(sequencingTechnologyService.getSequencingTechnology(anyString())).thenReturn(seqTech)
        `when`(externalMetadataSourceService.getSingleValue(matches("isParsableProject"), anyMap())).thenReturn("true")
        `when`(seqTypeMappingService.getSeqType(importObject.samples!!.first().type)).thenReturn(seqType)
        `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(importObject.samples!!.first().sampleName)).thenReturn("tagmentationLibrary")
        `when`(speciesService.getSpeciesWithStrainForSpecies(importObject.samples!!.first().species)).thenReturn("Human (Homo sapiens) [No strain available]")
        `when`(sampleRepository.findAllBySubmission(anySubmission())).thenReturn(listOf(entityFactory.getSample()))

        val submission = ilseImportServiceMock.saveSubmission("i0000001", importObject, "ticketNumber") as ApiSubmission

        verify(submissionRepository, times(2)).saveAndFlush(submission)
        verify(ilseImportServiceMock, times(1)).saveSamples(submission, importObject)
        assertThat(submission.type).isEqualTo(Submission.SubmissionType.MULTIPLEX)
        assertThat(submission.fasttrack).isEqualTo(false)
    }

    @Test
    fun `check saveSubmission functionality without sequencing technology`() {
        val importObject = entityFactory.getExternalIlseSubmissionImportObjectWithUnknownValue()
        val submitter = entityFactory.getPerson()
        val seqType = entityFactory.getSeqType()
        val seqTech = entityFactory.getSeqTech()

        `when`(ldapService.getPersonByUsername(anyString())).thenReturn(submitter)
        `when`(sequencingTechnologyService.getSequencingTechnology(anyString())).thenReturn(null)
        `when`(sequencingTechnologyRepository.findByDefaultObjectIsTrue()).thenReturn(setOf(seqTech))
        `when`(validationLevelRepository.findByDefaultObjectIsTrue()).thenReturn(setOf(entityFactory.getValidationLevel()))
        `when`(externalMetadataSourceService.getSingleValue(matches("isParsableProject"), anyMap())).thenReturn("true")
        `when`(seqTypeMappingService.getSeqType(importObject.samples!!.first().type)).thenReturn(seqType)
        `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(importObject.samples!!.first().sampleName)).thenReturn("tagmentationLibrary")
        `when`(speciesService.getSpeciesWithStrainForSpecies(importObject.samples!!.first().species)).thenReturn("Human (Homo sapiens) [No strain available]")
        `when`(sampleRepository.findAllBySubmission(anySubmission())).thenReturn(listOf(entityFactory.getSample()))
        `when`(mailContentGeneratorService.getTicketSubjectPrefix(anySubmission())).thenReturn("ticketSubjectPrefix")

        val submission = ilseImportServiceMock.saveSubmission("i0000001", importObject, "ticketNumber") as ApiSubmission

        verify(submissionRepository, times(2)).saveAndFlush(submission)
        verify(ilseImportServiceMock, times(1)).saveSamples(submission, importObject)
        verify(mailSenderService, times(1)).sendMailToTicketSystem(
            mailBundle.getString("importer.sequencingTechnologyUnknownSubject").replace("{0}", "ticketSubjectPrefix"),
            mailBundle.getString("importer.sequencingTechnologyUnknownBody")
                .replace("{0}", submission.identifier)
                .replace("{1}", "NovaSeq 6000 Paired-End 100bp S1")
        )
    }

    @TestFactory
    fun `check stateChange and sendMail for species`() = listOf(
        "Unknown Species [strange unknown strain]" to mapOf(
            "conditionalNo" to "",
            "mailCounter" to 1,
            "stateChangeCounter" to 1
        ),
        "Other Species (please use remarks)" to mapOf(
            "conditionalNo" to "no ",
            "mailCounter" to 0,
            "stateChangeCounter" to 0
        ),
        "Human (Homo sapiens) [No strain available]" to mapOf(
            "conditionalNo" to "no ",
            "mailCounter" to 0,
            "stateChangeCounter" to 0
        ),
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("check species sends ${expected["conditionalNo"]}mail for '$input'") {
            val submission = entityFactory.getApiSubmission()
            val ilseObject = entityFactory.getExternalIlseSubmissionImportObjectWithUnknownValue()
            ilseObject.samples!!.first().species = input
            val sample = entityFactory.getSample()
            val seqType = entityFactory.getSeqType()

            `when`(seqTypeMappingService.getSeqType(ilseObject.samples!!.first().type)).thenReturn(seqType)
            `when`(externalMetadataSourceService.getValuesAsSet("projectsWithAliases")).thenReturn(setOf(ilseObject.samples!!.first().odcf_project))
            `when`(runtimeOptionsRepository.findByName("unregisteredButAcceptedForImportSpecies")).thenReturn(entityFactory.getRuntimeOption("unregisteredButAcceptedForImportSpecies", "Other Species (please use remarks)"))
            `when`(speciesService.getSpeciesForImport()).thenReturn(listOf("Human (Homo sapiens) [No strain available]"))
            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
            doReturn(sample).`when`(ilseImportServiceMock).saveSample(anySubmission(), anyObject())

            ilseImportServiceMock.saveSamples(submission, ilseObject)

            verify(mailSenderService, times(expected["mailCounter"] as Int)).sendMailToTicketSystem(endsWith("I found unknown field values"), contains("{species=[${ilseObject.samples!!.first().species}]}"))
            verify(submissionService, times(expected["stateChangeCounter"] as Int)).changeSubmissionState(submission, Submission.Status.ON_HOLD, null, null, "Found unknown species [${ilseObject.samples!!.first().species}]. It has to be verified by the ODCF.")
        }
    }

    @Test
    fun `Test save sample single`() {
        val submission = entityFactory.getApiSubmission()
        val sampleImportObject = entityFactory.getIlseSampleImportObject()
        val seqType = entityFactory.getSeqType()

        `when`(seqTypeMappingService.getSeqType(sampleImportObject.type)).thenReturn(seqType)
        `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleName)).thenReturn("tagmentationLibrary")
        `when`(speciesService.getSpeciesWithStrainForSpecies(sampleImportObject.species)).thenReturn("Human (Homo sapiens) [No strain available]")

        val sample = ilseImportServiceMock.saveSample(submission, sampleImportObject)

        assertThat(sample.name).isEqualTo("sampleName")
        assertThat(sample.parseIdentifier).isEqualTo("sampleName")
        assertThat(sample.read1Length).isEqualTo(51)
        assertThat(sample.libraryLayout).isEqualTo(Sample.LibraryLayout.SINGLE)
        assertThat(sample.baseMaterial).isEqualTo(sample.seqType!!.basicSeqType)
        assertThat(sample.requestedLanes).isEqualTo(4.0)
        assertThat(sample.speciesWithStrain).isEqualTo("Human (Homo sapiens) [No strain available]")
    }

    @Test
    fun `Test save sample paired`() {
        val submission = entityFactory.getApiSubmission()
        val sampleImportObject = entityFactory.getIlseSampleImportObject()
        sampleImportObject.read_2_length = 55
        val seqType = entityFactory.getSeqType()

        `when`(seqTypeMappingService.getSeqType(sampleImportObject.type)).thenReturn(seqType)
        `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleName)).thenReturn("tagmentationLibrary")
        `when`(speciesService.getSpeciesWithStrainForSpecies(sampleImportObject.species)).thenReturn("Human (Homo sapiens)[No strain available]")

        val sample = ilseImportServiceMock.saveSample(submission, sampleImportObject)

        assertThat(sample.name).isEqualTo("sampleName")
        assertThat(sample.read1Length).isEqualTo(51)
        assertThat(sample.read2Length).isEqualTo(55)
        assertThat(sample.libraryLayout).isEqualTo(Sample.LibraryLayout.PAIRED)
        assertThat(sample.baseMaterial).isEqualTo(sample.seqType!!.basicSeqType)
        assertThat(sample.requestedLanes).isEqualTo(4.0)
        assertThat(sample.speciesWithStrain).isEqualTo("Human (Homo sapiens)[No strain available]")
    }

    @TestFactory
    fun `test save sample with projects`() = listOf(
        "000_OTHER" to "",
        "PROJECT_IN_OTP" to "PROJECT_IN_OTP",
        "PROJECT_NOT_IN_OTP" to "",
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("test save sample with project $input") {
            val submission = entityFactory.getApiSubmission()
            val sampleImportObject = entityFactory.getIlseSampleImportObject()
            sampleImportObject.odcf_project = input
            val seqType = entityFactory.getSeqType()

            `when`(seqTypeMappingService.getSeqType(sampleImportObject.type)).thenReturn(seqType)
            `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleName)).thenReturn("tagmentationLibrary")
            `when`(speciesService.getSpeciesWithStrainForSpecies(sampleImportObject.species)).thenReturn("Human (Homo sapiens)[No strain available]")
            `when`(externalMetadataSourceService.getValuesAsSet("projectsWithAliases")).thenReturn(setOf("PROJECT_IN_OTP"))

            val sample = ilseImportServiceMock.saveSample(submission, sampleImportObject)

            assertThat(sample.project).isEqualTo(expected)
        }
    }

    @Test
    fun `Test save sample well label`() {
        val submission = entityFactory.getApiSubmission()
        val sampleImportObject = entityFactory.getIlseSampleImportObject()
        sampleImportObject.odcf_single_cell_well_label = "first-second"
        val seqType = entityFactory.getSeqType()

        `when`(seqTypeMappingService.getSeqType(sampleImportObject.type)).thenReturn(seqType)
        `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleName)).thenReturn("tagmentationLibrary")
        `when`(speciesService.getSpeciesWithStrainForSpecies(sampleImportObject.species)).thenReturn("species")

        val sample = ilseImportServiceMock.saveSample(submission, sampleImportObject)

        assertThat(sample.singleCellPlate).isEqualTo("first")
        assertThat(sample.singleCellWellPosition).isEqualTo("second")
    }

    @Test
    fun `Test successful reimport`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        submission.originProjects = "project_before_reset"

        // sample after reset:
        val sampleImportObject = entityFactory.getIlseSampleImportObject()
        sampleImportObject.odcf_project = "project_after_reset"
        val externalIlseSubmissionImportObject = ExternalIlseSubmissionImportObject()
        externalIlseSubmissionImportObject.samples = listOf(sampleImportObject)

        var submissionStatusChangedToReset = false
        val ilseId = submission.identifier.substring(1).toInt()
        `when`(ilseService.getSubmissionImportObjectFromApi(ilseId, true)).thenReturn(externalIlseSubmissionImportObject)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(seqTypeMappingService.getSeqType(sampleImportObject.type)).thenReturn(entityFactory.getSeqType())
        `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleName)).thenReturn("")
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())
        `when`(submissionService.changeSubmissionState(anySubmission(), anyObject(), anyString(), nullable(String::class.java), nullable(String::class.java))).then {
            submissionStatusChangedToReset = (it.arguments[1] as Submission.Status) == Submission.Status.RESET
            it
        }
        `when`(speciesService.getSpeciesWithStrainForSpecies(sampleImportObject.species)).thenReturn("species")
        `when`(externalMetadataSourceService.getValuesAsSet("projectsWithAliases")).thenReturn(setOf("project_after_reset"))

        ilseImportServiceMock.reimport(submission)

        assertThat(submission.originProjectsSet).doesNotContain("project_before_reset")
        assertThat(submission.originProjectsSet).contains("project_after_reset")
        assertThat(submissionStatusChangedToReset).isTrue
    }

    @Test
    fun `Test reimport of unresettable submission`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)
        submission.resettable = false
        submission.originProjects = "project_before_reset"

        // sample after reset:
        val sampleImportObject = entityFactory.getIlseSampleImportObject()
        sampleImportObject.odcf_project = "project_after_reset"
        val externalIlseSubmissionImportObject = ExternalIlseSubmissionImportObject()
        externalIlseSubmissionImportObject.samples = listOf(sampleImportObject)

        var submissionStatusChangedToReset = false
        val ilseId = submission.identifier.substring(1).toInt()
        `when`(ilseService.getSubmissionImportObjectFromApi(ilseId)).thenReturn(externalIlseSubmissionImportObject)
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(seqTypeMappingService.getSeqType(sampleImportObject.type)).thenReturn(entityFactory.getSeqType())
        `when`(importService.extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleImportObject.sampleName)).thenReturn("")
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())
        `when`(submissionService.changeSubmissionState(anySubmission(), anyObject(), anyString(), nullable(String::class.java), nullable(String::class.java))).then {
            submissionStatusChangedToReset = (it.arguments[1] as Submission.Status) == Submission.Status.RESET
            it
        }

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            ilseImportServiceMock.reimport(submission)
        }.withMessage("Could not reset submission ${submission.identifier}")

        assertThat(submission.originProjectsSet).contains("project_before_reset")
        assertThat(submission.originProjectsSet).doesNotContain("project_after_reset")
        assertThat(submissionStatusChangedToReset).isFalse
    }

    @Test
    fun `Test unsuccessful reimport`() {
        val submission = entityFactory.getApiSubmission()
        val sample = entityFactory.getSample(submission)

        val ilseId = submission.identifier.substring(1).toInt()
        `when`(ilseService.getSubmissionImportObjectFromApi(ilseId, true)).thenThrow(ExternalApiReadException("IlseApiReadException", ApiType.ILSe))
        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(ldapService.getPerson()).thenReturn(entityFactory.getPerson())

        assertThatExceptionOfType(ExternalApiReadException::class.java).isThrownBy {
            ilseImportServiceMock.reimport(submission)
        }.withMessage("ILSe: IlseApiReadException")
    }

    @Test
    fun `check summariesAfterImport functionality`(): Unit = runBlocking {
        launch {
            val submission = entityFactory.getApiSubmission(Submission.Status.AUTO_CLOSED, "") as ApiSubmission
            val sample = entityFactory.getSample(submission)
            submission.fasttrack = true

            `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
            `when`(externalMetadataSourceService.getSingleValue(matches("projectNotificationStatus"), anyMap())).thenReturn("true")

            ilseImportServiceMock.summariesAfterImport(submission)
            delay(200)

            verify(pseudonymService, times(1)).checkSimilarPidsAndSendMail(submission)
            verify(mailSenderService, times(1)).sendMailFasttrackImported(submission)
        }
    }
}
