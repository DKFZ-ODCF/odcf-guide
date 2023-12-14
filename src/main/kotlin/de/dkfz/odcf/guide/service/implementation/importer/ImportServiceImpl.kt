package de.dkfz.odcf.guide.service.implementation.importer

import de.dkfz.odcf.guide.FileRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.helperObjects.enums.ApiType
import de.dkfz.odcf.guide.service.interfaces.external.JsonApiService
import de.dkfz.odcf.guide.service.interfaces.importer.ImportService
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import javax.management.relation.RelationException
import javax.persistence.EntityManager

@Service
class ImportServiceImpl(
    private val fileRepository: FileRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val jsonApiService: JsonApiService,
    private val entityManager: EntityManager,
    private val env: Environment,
) : ImportService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun extractTagmentationLibraryFromSampleIdentifierIfNecessary(sampleIdentifier: String, tagmentationLibrary: String): String {
        return if (tagmentationLibrary == "null" || tagmentationLibrary == "") {
            ".+[-_]lib(\\d+$)".toRegex().find(sampleIdentifier)?.groups?.get(1)?.value.orEmpty()
        } else {
            tagmentationLibrary
        }
    }

    override fun generateInternalIdentifier(): String {
        val latestId = entityManager.createNativeQuery("select nextval('internal_submission_id')").singleResult.toString().toLong()
        return String.format("o%07d", latestId)
    }

    override fun generateIlseIdentifier(ilseId: Int): String {
        return String.format("i%07d", ilseId)
    }

    @Throws(RelationException::class)
    override fun findSampleByFile(file: File, submission: Submission): Sample? {
        if (file.isSampleInitialized()) {
            return file.sample
        }
        return findSampleByFileName(file.fileName, submission)
    }

    @Throws(RelationException::class)
    override fun findSampleByFileName(filename: String, submission: Submission): Sample? {
        val files = findFastqFilePairs(filename, submission)
        if (files.isNotEmpty()) {
            val sample: Sample = files[0].sample
            for (file in files) {
                val currentFileSample = file.sample
                if (currentFileSample != sample) {
                    throw RelationException("Files have different samples.")
                }
            }
            return sample
        }
        return null
    }

    override fun findFastqFilePairs(filename: String, submission: Submission): List<File> {
        val regexSuffix = runtimeOptionsRepository.findByName("fastqFileSuffix")!!.value
        val regex = ("(.*)$regexSuffix\$".toRegex())
        val regexSuffixForLike = regexSuffix
            .replace("[R|I]", "_")
            .replace("[1|2]", "_")
            .replace("\\", "")
        val searchFileName = regex.find(filename)?.groupValues?.get(1)?.plus(regexSuffixForLike) ?: filename // groupValues 0 => full regex match, groupValues 1 => first match group

        return fileRepository.findByFileNameLikeIgnoreCaseAndSample_Submission(searchFileName, submission)
    }

    override fun createTicket(identifier: String, projects: List<String>): String {
        if (env.getRequiredProperty("application.mails.sendmail").toBoolean().not()) return ""

        val ticketSystemPath = runtimeOptionsRepository.findByName("ticketSystemPath")?.value ?: return ""
        val createTicketJson = runtimeOptionsRepository.findByName("createTicketJson")?.value ?: return ""

        try {
            val jsonBody = createTicketJson
                .replace("<IDENTIFIER>", identifier)
                .replace("<PROJECTS>", projects.ifEmpty { listOf("No project") }.sorted().joinToString())
            val result = JSONObject(jsonApiService.postJsonToApi(ticketSystemPath, emptyMap(), jsonBody, ApiType.OTRS))
            return result.getString("TicketNumber")
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
        }
        return ""
    }
}
