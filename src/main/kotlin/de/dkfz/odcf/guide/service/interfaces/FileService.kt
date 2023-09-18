package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.dtoObjects.FileGuiDto
import de.dkfz.odcf.guide.entity.submissionData.File
import de.dkfz.odcf.guide.entity.submissionData.Sample
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.OutputFileNotWritableException
import de.dkfz.odcf.guide.helperObjects.SampleTsvMapping
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.nio.file.NoSuchFileException

interface FileService {

    /**
     * Reads the content from a CSV file.
     *
     * @param stream Output from running the `cat` command on a CSV file.
     * @return List<MutableMap<String, String>>
     *     each list element is a row of the CSV file that contains a header title of the table mapped to its value.
     */
    @Throws(IOException::class)
    fun readFromCsv(stream: InputStream): List<MutableMap<String, String>>

    fun readFromXlsx(stream: InputStream): List<MutableMap<String, String>>

    fun readFromXls(stream: InputStream): List<MutableMap<String, String>>

    /**
     * Reads the content from a CSV file and saves the sample information in SampleTsvMapping objects.
     *
     * @param stream Output from running the `cat` command on a CSV file.
     * @return List of SampleTsvMapping objects.
     */
    fun readFromSimpleCsv(stream: InputStream): List<SampleTsvMapping>

    /**
     * Creates TSV file of a simple submission without extended technical sample metadata.
     *
     * @param submission Submission object from which the TSV file is created
     * @return Tab seperated content of the submission as a String
     */
    fun createTsvFile(submission: Submission): String

    /**
     * Creates metadata template of all the properties to be used as the header of the TSV file.
     *
     * @return Tab seperated names of all the properties in the metaDataColumnRepository.
     */
    fun createMetadataTemplate(): String

    /**
     * Creates TSV file of an extended submission with technical sample metadata and unknown values.
     *
     * @param submission Submission object from which the TSV file is created.
     * @param withImportIdentifier whether to use the property "importIdentifier" or "name".
     * @param withExportNames whether to use the export names of the metadata columns.
     * @return Tab seperated content of the submission as a String.
     */
    fun createLongTsvFile(submission: Submission, withImportIdentifier: Boolean = true, withExportNames: Boolean = true): String

    /**
     * Creates directories for the output and writes out the extended TSV files of a submission on the file system.
     *
     * @param submission Submission object from which the TSV file is created.
     * @param outputAsHtml whether the return content will use `<br />` as line breaks.
     * @return The paths to the created files as a String seperated by line breaks.
     */
    @Throws(OutputFileNotWritableException::class)
    fun writeLongTsvFile(submission: Submission, outputAsHtml: Boolean = false): String

    /**
     * Reads TSV file, compares the data with the already existing data of a submission and overwrites the samples with new information.
     *
     * @param submission already existing submission to be updated
     * @param file the TSV file that is being uploaded
     * @throws RowNotFoundException if the file has more rows than the samples belonging to the submission.
     * @throws SampleNamesDontMatchException if the sample names do not match.
     *
     * @return sample objects with new data
     */
    fun readTsvFile(submission: Submission, file: MultipartFile): List<Sample>

    /**
     * Checks whether a file exists on the remote file system.
     *
     * @throws IOException if there are problems with querying the remote file system
     * @param file String of the path to the file.
     */
    @Throws(IOException::class)
    fun fileExists(file: String): Boolean

    /**
     * Reads TSV file from a file path on the remote file system. Triggers [readFromCsv] to read the content.
     *
     * @throws IOException if there are problems with querying the remote file system
     * @throws NoSuchFileException if the file from the given file path is empty
     * @param path to the TSV file
     *
     * @return List<MutableMap<String, String>> each list element is a row of the CSV file that contains a header title of the table mapped to its value.
     */
    @Throws(IOException::class)
    fun readTsvFile(path: String): List<MutableMap<String, String>>

    /**
     * Converts FileGuiDto object to File entity. If the `id` of the
     * FileGuiDto object is `0`, a new File is created, otherwise the
     * File object with the corresponding `id` is used.
     *
     * @param fileGuiDto to be converted
     * @param sample the corresponding sample to which the File belongs.
     * @return File entity
     */
    fun convertToEntity(fileGuiDto: FileGuiDto, sample: Sample): File

    /**
     * Writes String content into a temporary File with the filename `{submissionIdentifier}_{timestamp}.tsv`
     *
     * @throws IOException if there are problems with creating a temporary file
     * @param submissionIdentifier submissionIdentifier for the filename
     * @param content String content of the File to be written
     * @return a temporary file that can be used as an attachment for a mail
     */
    @Throws(IOException::class)
    fun convertStringToTSVFile(submissionIdentifier: String, content: String): java.io.File
}
