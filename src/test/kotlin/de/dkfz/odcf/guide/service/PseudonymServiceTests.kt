package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.exceptions.MissingPropertyException
import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.PseudonymServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import de.dkfz.odcf.guide.service.interfaces.mail.MailContentGeneratorService
import de.dkfz.odcf.guide.service.interfaces.mail.MailSenderService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class PseudonymServiceTests {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var pseudonymServiceMock: PseudonymServiceImpl

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Mock
    lateinit var mailSenderService: MailSenderService

    @Mock
    lateinit var mailContentGeneratorService: MailContentGeneratorService

    @Mock
    lateinit var sampleRepository: SampleRepository

    @Test
    fun `check pseudonym with other found samples`() {
        val pseudonym = "testPseudonym"
        val sample = entityFactory.getSample()
        sample.pid = "prefix-$pseudonym"
        val submission = sample.submission
        val sample1 = entityFactory.getSample()
        sample1.pid = "prefix1-$pseudonym"
        sample1.sampleType = "sample-type001"
        val sample2 = entityFactory.getSample()
        sample2.pid = "prefix2-$pseudonym"
        sample2.sampleType = "sample-type02"

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(anyString(), anyString(), anyString())).thenReturn(listOf(sample1, sample2))
        `when`(externalMetadataSourceService.getSingleValue(matches("projectPrefixByProject"), anyMap())).thenReturn("")
        `when`(externalMetadataSourceService.getValuesAsSetMap(matches("pidsByPseudonym"), anyMap())).thenReturn(
            setOf(
                mapOf(
                    "pid" to sample1.pid,
                    "target_entity" to sample1.project,
                    "seq_type" to sample1.seqType!!.name,
                    "sample_type" to sample1.sampleType
                )
            )
        )

        val result = pseudonymServiceMock.getSimilarPids(submission)

        assertThat(result).hasSize(3)
        assertThat(result).contains(
            mapOf(
                "checked_pid" to sample.pid,
                "pid" to sample1.pid,
                "target_entity" to sample1.project,
                "seq_type" to sample1.seqType!!.name,
                "sample_type" to sample1.sampleType,
                "scope" to "OTP",
            )
        )
        assertThat(result).contains(
            mapOf(
                "checked_pid" to sample.pid,
                "pid" to sample1.pid,
                "target_entity" to sample1.submission.identifier,
                "seq_type" to sample1.seqType!!.name,
                "sample_type" to sample1.sampleType,
                "scope" to "GUIDE",
            )
        )
        assertThat(result).contains(
            mapOf(
                "checked_pid" to sample.pid,
                "pid" to sample2.pid,
                "target_entity" to sample2.submission.identifier,
                "seq_type" to sample2.seqType!!.name,
                "sample_type" to sample2.sampleType,
                "scope" to "GUIDE",
            )
        )
    }

    @Test
    fun `check pseudonym with other found samples otp and guide are different`() {
        val pseudonym = "testPseudonym"
        val sample = entityFactory.getSample()
        sample.pid = "prefix-$pseudonym"
        val submission = sample.submission
        val sample1 = entityFactory.getSample()
        sample1.pid = "prefix1-$pseudonym"
        sample1.sampleType = "sample-type001"
        val sample2 = entityFactory.getSample()
        sample2.pid = "prefix2-$pseudonym"
        sample2.sampleType = "sample-type02"

        `when`(sampleRepository.findAllBySubmission(submission)).thenReturn(listOf(sample))
        `when`(sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(anyString(), anyString(), anyString())).thenReturn(listOf(sample1))
        `when`(externalMetadataSourceService.getSingleValue(matches("projectPrefixByProject"), anyMap())).thenReturn("")
        `when`(externalMetadataSourceService.getValuesAsSetMap(matches("pidsByPseudonym"), anyMap())).thenReturn(
            setOf(
                mapOf(
                    "pid" to sample2.pid,
                    "target_entity" to sample2.project,
                    "seq_type" to sample2.seqType!!.name,
                    "sample_type" to sample2.sampleType
                )
            )
        )

        val result = pseudonymServiceMock.getSimilarPids(submission)

        assertThat(result).hasSize(2)
        assertThat(result).contains(
            mapOf(
                "checked_pid" to sample.pid,
                "pid" to sample1.pid,
                "target_entity" to sample1.submission.identifier,
                "seq_type" to sample1.seqType!!.name,
                "sample_type" to sample1.sampleType,
                "scope" to "GUIDE",
            )
        )
        assertThat(result).contains(
            mapOf(
                "checked_pid" to sample.pid,
                "pid" to sample2.pid,
                "target_entity" to sample2.project,
                "seq_type" to sample2.seqType!!.name,
                "sample_type" to sample2.sampleType,
                "scope" to "OTP",
            )
        )
    }

    @Test
    fun `check pseudonym without other found samples`() {
        val pseudonym = "testPseudonym"
        val sample = entityFactory.getSample()
        sample.pid = "prefix-$pseudonym"
        val submission = sample.submission

        `when`(sampleRepository.findAllByPidEndsWithAndProjectNotAndSeqType_Name(anyString(), anyString(), anyString())).thenReturn(listOf(sample))
        `when`(externalMetadataSourceService.getValuesAsSetMap(matches("pidsByPseudonym"), anyMap())).thenReturn(setOf(mapOf()))

        val result = pseudonymServiceMock.getSimilarPids(submission)

        assertThat(result).isEmpty()
    }

    @Test
    fun `check mail`() {
        val submission = entityFactory.getApiSubmission()
        val matchResults = setOf(
            mapOf(
                "checked_pid" to "checked_pid12",
                "pid" to "pid1",
                "target_entity" to "submission1",
                "seq_type" to "seq_type1",
                "sample_type" to "sample_type1",
                "scope" to "GUIDE",
            ),
            mapOf(
                "checked_pid" to "checked_pid12",
                "pid" to "pid2",
                "target_entity" to "project2",
                "seq_type" to "seq_type2",
                "sample_type" to "sample_type2",
                "scope" to "OTP",
            ),
            mapOf(
                "checked_pid" to "checked_pid34",
                "pid" to "pid3",
                "target_entity" to "submission3",
                "seq_type" to "seq_type3",
                "sample_type" to "sample_type3",
                "scope" to "GUIDE",
            ),
            mapOf(
                "checked_pid" to "checked_pid34",
                "pid" to "pid3",
                "target_entity" to "project3",
                "seq_type" to "seq_type3",
                "sample_type" to "sample_type3",
                "scope" to "OTP",
            )
        )
        var subject = ""
        var body = ""

        `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")
        `when`(mailSenderService.sendMailToTicketSystem(anyString(), anyString())).then {
            subject = it.arguments[0] as String
            body = it.arguments[1] as String
            it.arguments
        }

        pseudonymServiceMock.sendMail(matchResults, submission)

        assertThat(subject).isEqualTo("[prefix] Found similar PIDs")
        assertThat(body).isEqualTo(
            "Dear ODCF service,\n\n" +
                "For submission ${submission.identifier} similar PIDs were found:\n\n" +
                "'checked_pid12' has following similar PIDs:\n" +
                "&nbsp;&nbsp;&nbsp;- 'pid1' in submission submission1 (sampleType:'sample_type1', seqType:'seq_type1') in GUIDE,\n" +
                "&nbsp;&nbsp;&nbsp;- 'pid2' in project project2 (sampleType:'sample_type2', seqType:'seq_type2') in OTP\n\n" +
                "'checked_pid34' has following similar PIDs:\n" +
                "&nbsp;&nbsp;&nbsp;- 'pid3' in submission submission3 (sampleType:'sample_type3', seqType:'seq_type3') in GUIDE,\n" +
                "&nbsp;&nbsp;&nbsp;- 'pid3' in project project3 (sampleType:'sample_type3', seqType:'seq_type3') in OTP\n\n" +
                "Kind regards,\n" +
                "ODCF Team"
        )
    }

    @TestFactory
    fun `check mail throws exception`() = listOf(
        setOf(
            mapOf(
                "checked_pid" to "checked_pid",
                "pid" to "pid1",
                "target_entity" to "submission1",
                "seq_type" to "seq_type1",
                "sample_type" to "sample_type1",
            )
        ) to "scope",

        setOf(
            mapOf(
                "checked_pid" to "checked_pid",
                "pid" to "pid1",
                "seq_type" to "seq_type1",
                "sample_type" to "sample_type1",
                "scope" to "GUIDE",
            )
        ) to "target_entity",

        setOf(
            mapOf(
                "checked_pid" to "checked_pid",
                "pid" to "pid1",
                "target_entity" to "submission1",
                "sample_type" to "sample_type1",
                "scope" to "GUIDE",
            )
        ) to "seq_type"
    ).map { (input, expected) ->
        dynamicTest("check mail throws exception for $expected") {
            val submission = entityFactory.getApiSubmission()

            `when`(mailContentGeneratorService.getTicketSubjectPrefix(submission)).thenReturn("[prefix]")

            assertThatExceptionOfType(MissingPropertyException::class.java).isThrownBy {
                pseudonymServiceMock.sendMail(input, submission)
            }.withMessage("property not given '$expected'")
        }
    }
}
