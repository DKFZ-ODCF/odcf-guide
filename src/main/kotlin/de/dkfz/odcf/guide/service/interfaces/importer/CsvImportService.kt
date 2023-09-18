package de.dkfz.odcf.guide.service.interfaces.importer

import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.ColumnNotFoundException
import de.dkfz.odcf.guide.exceptions.GuideRuntimeException
import de.dkfz.odcf.guide.exceptions.Md5SumFoundInDifferentSubmission
import org.springframework.dao.DuplicateKeyException
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.*
import javax.management.relation.RelationException

interface CsvImportService {

    /**
     * Imports data from a CSV file and saves it to a new Submission object.
     * Also triggers the sending of a mail that the submission has been uploaded.
     *
     * @param file CSV file to be imported
     * @param ticket The OTRS ticket number to the submission
     * @param email E-Mail of the submitter
     * @param ignoreMd5Check Whether to ignore the checking for duplicated MD5 sums in the files.
     *
     * @throws DuplicateKeyException If a submission with the same identifier already exists.
     * @throws IOException If there were problems with reading the CSV file, like e.g. a wrong file format.
     * @throws RelationException If one of the filename properties in the CSV file points to multiple associated samples.
     * @throws ColumnNotFoundException If the columns for the fastq file name
     *     (or the UUID, if this isn't the initial import of the submission) are not found in the CSV file that is being uploaded
     * @throws Md5SumFoundInDifferentSubmission If the md5 sum associated to a file is also found in an already existing submission
     * @throws GuideRuntimeException If something went wrong during the process of importing, e.g. the renaming of the columns in the CSV file
     *
     * @return Newly imported Submission object
     */
    @Throws(
        DuplicateKeyException::class,
        IOException::class,
        RelationException::class,
        ColumnNotFoundException::class,
        GuideRuntimeException::class
    )
    fun import(file: MultipartFile, ticket: String, email: String, customName: String = "", comment: String = "", ignoreMd5Check: Boolean): Submission

    /**
     * Triggers the import and saving of additional information to a given ILSe API submission.
     *
     * @param ilse ILSe Identifier of the submission
     * @return The submission mapped to a set of warnings if something is inconsistent.
     */
    /*fun importAdditional(ilse: Int): Pair<Submission, Set<String>>*/

    /**
     * Creates a new uploaded submission and saves its information.
     *
     * @param ticketNumber The OTRS ticket number to the submission
     * @param email E-Mail of the submitter
     * @return The newly saved submission
     */
    fun saveSubmission(ticketNumber: String, email: String, customName: String = "", comment: String = ""): Submission

    /**
     * Saves the files and samples from a given CSV file for a specific submission.
     *
     * @param submission Submission to which the files and samples belong.
     * @param csvRows All the rows of the CSV file to be imported.
     * @param override Whether to override an already existing submission.
     * @param initialUpload Whether this is the first upload of the submission.
     * @return The newly saved submission
     */
    fun saveFilesAndSamples(
        submission: Submission,
        csvRows: List<Map<String, String>>?,
        override: Boolean = false,
        initialUpload: Boolean = false,
    ): Submission
}
