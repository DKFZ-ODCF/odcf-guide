package de.dkfz.odcf.guide.service.implementation

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import de.dkfz.odcf.guide.FileRepository
import de.dkfz.odcf.guide.MetaDataColumnRepository
import de.dkfz.odcf.guide.RuntimeOptionsRepository
import de.dkfz.odcf.guide.SampleRepository
import de.dkfz.odcf.guide.annotation.ExcludeFromJacocoGeneratedReport
import de.dkfz.odcf.guide.dtoObjects.FileGuiDto
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.entity.submissionData.TechnicalSample
import de.dkfz.odcf.guide.exceptions.*
import de.dkfz.odcf.guide.helperObjects.SampleTsvMapping
import de.dkfz.odcf.guide.service.interfaces.FileService
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

@Service
class FileServiceImpl(
    private val sampleRepository: SampleRepository,
    private val fileRepository: FileRepository,
    private val metaDataColumnRepository: MetaDataColumnRepository,
    private val runtimeOptionsRepository: RuntimeOptionsRepository,
    private val collectorService: CollectorService,
    private val seqTypeMappingService: SeqTypeMappingService,
    private val remoteCommandsService: RemoteCommandsService,
) : FileService {

    private val tab = '\t'
    private var bundle = ResourceBundle.getBundle("messages", Locale.getDefault())

    @Throws(IOException::class)
    override fun readFromCsv(stream: InputStream): List<MutableMap<String, String>> {
        val values = emptyList<MutableMap<String, String>>().toMutableList()
        val csvSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator(tab).withoutQuoteChar()
        val csvMapper = CsvMapper()
        csvMapper.enable(CsvParser.Feature.TRIM_SPACES)
        csvMapper.enable(CsvParser.Feature.SKIP_EMPTY_LINES)
        val iter: MappingIterator<MutableMap<String, String>> = csvMapper.readerFor(MutableMap::class.java).with(csvSchema).readValues(stream)
        while (iter.hasNext()) {
            try {
                values.add(iter.next())
            } catch (e: RuntimeException) {
                val cause = e.cause
                if (cause is IOException) throw cause
            }
        }
        return values
    }

    override fun readFromXls(stream: InputStream): List<MutableMap<String, String>> {
        return readFromWorkbook(HSSFWorkbook(stream))
    }

    override fun readFromXlsx(stream: InputStream): List<MutableMap<String, String>> {
        return readFromWorkbook(XSSFWorkbook(stream))
    }

    fun readFromWorkbook(workbook: Workbook): List<MutableMap<String, String>> {
        val sheet = workbook.getSheetAt(0)
        val sheetAsList = mutableListOf<MutableMap<String, String>>()

        val headerRow = sheet.getRow(0)
        for (i in 1..sheet.lastRowNum) {
            val currentRow = sheet.getRow(i) ?: continue
            val rowAsMap = mutableMapOf<String, String>()
            for (j in 0..currentRow.lastCellNum) {
                val currentCell = currentRow.getCell(j)
                val formatter = DataFormatter()
                rowAsMap[formatter.formatCellValue(headerRow.getCell(j))] = formatter.formatCellValue(currentCell)
            }
            if (rowAsMap.any { it.value.isNotBlank() }) sheetAsList.add(rowAsMap)
        }
        workbook.close()

        return sheetAsList
    }

    @Throws(IOException::class)
    override fun readFromSimpleCsv(stream: InputStream): List<SampleTsvMapping> {
        val values = emptyList<SampleTsvMapping>().toMutableList()
        val csvSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator('\t')
        val csvMapper = CsvMapper()
        csvMapper.enable(CsvParser.Feature.TRIM_SPACES)
        val iter: MappingIterator<SampleTsvMapping> = csvMapper.readerFor(SampleTsvMapping::class.java).with(csvSchema).readValues(stream)
        while (iter.hasNext()) {
            try {
                values.add(iter.next())
            } catch (e: RuntimeException) {
                val cause = e.cause
                if (cause is IOException) throw cause
            }
        }
        return values
    }

    override fun createTsvFile(submission: Submission): String {
        val samples = submission.samples.sortedBy { it.id }
        val tsvSamples = samples.map { SampleTsvMapping(it) }
        val mapper = CsvMapper()
        mapper.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        val schema: CsvSchema = mapper.schemaFor(SampleTsvMapping::class.java).withColumnSeparator(tab).withoutQuoteChar()
        return mapper.writer(schema.withUseHeader(true)).writeValueAsString(tsvSamples).trimIndent()
    }

    override fun createMetadataTemplate(): String {
        return metaDataColumnRepository.findAll().sortedBy { it.columnOrder }.joinToString("\t") { it.columnName }
    }

    /**
     * Returns file paths for the TSV files of a submission.
     *
     * @param submission Submission for which to get file paths for
     * @return Set of paths, one path corresponding to one project in external submissions.
     */
    @Throws(MissingRuntimeOptionException::class)
    fun getNamesForTsvFiles(submission: Submission): Set<String> {
        val basepath = runtimeOptionsRepository.findByName("tsvBasePath")?.value.orEmpty()
        if (basepath.isEmpty()) {
            throw MissingRuntimeOptionException("RuntimeOption 'tsvBasePath' is not set!")
        }

        return if (submission.isApiSubmission) {
            val ilseID = String.format("%06d", submission.identifier.substring(1).toInt())
            val ilsePrefix = ilseID.substring(0, 3)
            val ilseIdShort = ilseID.toInt().toString()

            val subpath = runtimeOptionsRepository.findByName("tsvInternalSubpath")?.value.orEmpty()
                .replace("<ILSE_PREFIX>", ilsePrefix, false)
                .replace("<ILSE_ID>", ilseID, false)
                .replace("<SUBMISSION_ID>", ilseIdShort, false)
            if (subpath.isEmpty()) {
                throw MissingRuntimeOptionException("RuntimeOption 'tsvInternalSubpath' is not set!")
            }
            mutableSetOf(basepath + subpath)
        } else {
            val subpath = runtimeOptionsRepository.findByName("tsvExternalSubpath")?.value.orEmpty()
                .replace("<DATE>", SimpleDateFormat("yyyy-MM-dd").format(Date()), false)
                .replace("<SUBMISSION_ID>", submission.identifier, false)
            if (subpath.isEmpty()) {
                throw MissingRuntimeOptionException("RuntimeOption 'tsvExternalSubpath' is not set!")
            }
            submission.projects.map { projectName ->
                (basepath + subpath).replace("<PROJECT>", projectName, false)
            }.distinct().toMutableSet()
        }
    }

    override fun createLongTsvFile(submission: Submission, withImportIdentifier: Boolean, withExportNames: Boolean): String {
        if (submission.samples.any { it.files.isEmpty() }) {
            return createTsvFile(submission).trimIndent()
        }
        val headers = emptySet<String>().toMutableSet()
        val rows = emptyList<Map<String, String>>().toMutableList()
        submission.samples.forEach { sample ->
            sample.files.forEach { file ->
                val columns = emptyMap<String, String>().toMutableMap()
                metaDataColumnRepository.findAll().sortedBy { it.columnOrder }.forEach { column ->
                    if (column.reflectionPropertyNameExport.isNotEmpty()) {
                        val className = column.reflectionClassName
                        var propertyName = column.reflectionPropertyNameExport
                        if (propertyName == "importIdentifier" && !withImportIdentifier) {
                            propertyName = "name"
                        }
                        val clazz = Class.forName("de.dkfz.odcf.guide.entity.submissionData.$className").kotlin
                        val property = clazz.memberProperties.find { it.name == propertyName }
                        val value = when (clazz) {
                            File::class -> property?.call(file)
                            Sample::class -> property?.call(sample)
                            TechnicalSample::class -> property?.call(sample.technicalSample)
                            Submission::class -> property?.call(submission)
                            else -> null
                        }
                        columns[if (withExportNames) column.exportName!! else column.columnName] = value?.toString() ?: ""
                    }
                }
                if (!withExportNames) columns["uuid (do not change)"] = "${file.uuid}"
                rows.add(columns)
                headers.addAll(columns.keys)
            }
        }
        val schemaBuilder = CsvSchema.builder()
        for (col in headers) {
            schemaBuilder.addColumn(col)
        }
        val schema = schemaBuilder.build().withLineSeparator("\n")
            .withColumnSeparator(tab)
            .withoutQuoteChar()
        return CsvMapper().writer(schema.withUseHeader(true)).writeValueAsString(rows).trimIndent()
    }

    @Throws(OutputFileNotWritableException::class)
    @ExcludeFromJacocoGeneratedReport
    override fun writeLongTsvFile(submission: Submission, outputAsHtml: Boolean): String {
        val tsvFileNames = getNamesForTsvFiles(submission)
        tsvFileNames.forEach { filepath ->
            val targetDir = Paths.get(filepath).parent
            if (!Files.exists(targetDir)) {
                val permissions = PosixFilePermissions.fromString("rwxrwx---") // this works only on unix systems
                try {
                    if (!Files.exists(targetDir.parent)) {
                        Files.createDirectories(targetDir.parent)
                        Files.setPosixFilePermissions(targetDir.parent, permissions)
                        // set SGID bit in order to hand down same group ID to subfolders as well
                        val process = ProcessBuilder("/bin/bash", "-c", "chmod g+s ${targetDir.parent}").start()
                        val exitCode: Int = process.waitFor()
                        if (exitCode != 0) {
                            throw OutputFileNotWritableException("Cannot set SGID bit for '${targetDir.parent}'. If this is a Windows system, that could be the reason.")
                        }
                    }
                    Files.createDirectory(targetDir)
                    Files.setPosixFilePermissions(targetDir, permissions) // SGID bit will be kept intact
                } catch (e: UnsupportedOperationException) {
                    throw OutputFileNotWritableException("Cannot create directories. If this is a Windows system, that could be the reason.")
                }
            }
            try {
                PrintWriter(filepath).use { out -> out.println(createLongTsvFile(submission)) }
            } catch (e: FileNotFoundException) {
                throw OutputFileNotWritableException("Could not write out metadata table to file $filepath:\n${e.message!!}")
            }
        }
        return if (outputAsHtml) {
            tsvFileNames.joinToString(separator = "<br />")
        } else {
            tsvFileNames.joinToString(separator = "\n")
        }
    }

    override fun readTsvFile(submission: Submission, file: MultipartFile): List<Sample> {
        val sampleTsvMappingObjects = readFromSimpleCsv(file.inputStream)
        val samples = sampleRepository.findBySubmission(submission)

        if (sampleTsvMappingObjects.size != samples.size) {
            throw RowNotFoundException(
                bundle.getString("csvImport.rowSizeExceptionSample")
                    .replace("<IS>", sampleTsvMappingObjects.size.toString())
                    .replace("<EXPECTED>", samples.size.toString())
            )
        }
        val sampleIdentifiers = samples.map { it.name }.sorted()
        val ilseNames = sampleTsvMappingObjects.map { it.sample_name }.sorted()
        if (sampleIdentifiers != ilseNames) {
            throw SampleNamesDontMatchException(
                bundle.getString("csvImport.sampleNameNotMatchException")
                    .replace("<EXPECTED>", sampleIdentifiers.toSet().minus(ilseNames.toSet()).joinToString())
                    .replace("<IS>", ilseNames.toSet().minus(sampleIdentifiers.toSet()).joinToString())
            )
        }

        val newSamples = emptyList<Sample>().toMutableList()
        val projectForSubmission = collectorService.getImportableProjects(submission).map { it.replace(" (closed)", "") }
        sampleTsvMappingObjects.forEach { sampleTsvMappingObject ->
            if (!projectForSubmission.contains(sampleTsvMappingObject.project)) {
                sampleTsvMappingObject.project = ""
            }
            val sample = samples.find { it.name == sampleTsvMappingObject.sample_name }!!
            sample.updateSampleByTsv(sampleTsvMappingObject, seqTypeMappingService.getSeqType(sampleTsvMappingObject.sequencing_type))
            newSamples.add(sample)
        }
        return newSamples
    }

    @ExcludeFromJacocoGeneratedReport
    override fun fileExists(file: String): Boolean {
        return remoteCommandsService.getFromRemote("[[ -f $file ]] && echo 'Found file'").contains("Found file")
    }

    @Throws(IOException::class)
    @ExcludeFromJacocoGeneratedReport
    override fun readTsvFile(path: String): List<MutableMap<String, String>> {
        val response = remoteCommandsService.getFromRemote("cat $path")
        if (response.isEmpty()) {
            throw NoSuchFileException("File is empty $path")
        }
        val targetStream: InputStream = ByteArrayInputStream(response.toByteArray())
        return readFromCsv(targetStream)
    }

    @Throws(ParseException::class)
    override fun convertToEntity(fileGuiDto: FileGuiDto, sample: Sample): File {
        val file = if (fileGuiDto.id == 0) {
            File(sample)
        } else {
            fileRepository.getOne(fileGuiDto.id)
        }
        FileGuiDto::class.memberProperties.forEach { prop ->
            val fileProp = File::class.memberProperties.filterIsInstance<KMutableProperty<*>>().find { it.name == prop.name }
            fileProp?.setter?.call(file, prop.get(fileGuiDto))
        }
        return file
    }

    @Throws(IOException::class)
    override fun convertStringToTSVFile(submissionIdentifier: String, content: String): java.io.File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val tempFile = File("${submissionIdentifier}_$timestamp.tsv")
        tempFile.deleteOnExit()
        tempFile.writeText(content)

        return tempFile
    }
}
