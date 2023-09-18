package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.parser.Parser
import de.dkfz.odcf.guide.entity.submissionData.Submission
import de.dkfz.odcf.guide.exceptions.ParserException
import de.dkfz.odcf.guide.helperObjects.ParserForm
import kotlin.jvm.Throws

interface ParserService {

    /**
     * Saves a new parser with multiple parser fields (e.g: sample type, patient ID) who in turn have multiple parser components.
     *
     * @param parserForm HTML Form containing all the form data.
     * @throws ParserException if there already exists a parser for the project.
     * @throws ParserException if the parserForm doesn't contain a parser or if no parser fields were found.
     * @return Newly saved Parser
     */
    @Throws(ParserException::class)
    fun saveParser(parserForm: ParserForm): Parser

    /**
     * Applies a parser to a parse identifier of the samples in a submission.
     * Only works if there exists a parser for the selected project(s) and if the parse identifier is written in the correct style.
     *
     * Saves the parsed output into the corresponding properties of the samples.
     *
     * @param submission Submission in which the parser is being used.
     * @throws ParserException if the list of errors is not empty.
     */
    fun applyParser(submission: Submission)
}
