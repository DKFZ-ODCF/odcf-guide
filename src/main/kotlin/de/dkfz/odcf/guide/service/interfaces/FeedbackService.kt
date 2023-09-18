package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.Feedback

interface FeedbackService {

    /**
     * Creates and saves a new Feedback Object of the feedback a user can give after a submission has been finished.
     *
     * @param feedbackSmiley String of the given feedback rating ("happy", "neutral", "sad").
     * @param message Content of the message of the feedback.
     * @param submissionId The ID to the submission that has just been finished and is therefore associated to this Feedback object.
     * @return Created Feedback Object
     */
    fun saveFeedback(feedbackSmiley: String, message: String, submissionId: String): Feedback

    /**
     * Sends a mail containing all the information about the feedback a user has just given.
     *
     * @param name Name of the user who gave the feedback.
     * @param mail Mail address of the user who gave the feedback.
     * @param rating String of the given feedback rating ("happy", "neutral", "sad").
     * @param message Content of the message of the feedback.
     * @param submissionId The ID to the submission that has just been finished and is therefore associated to this Feedback object.
     */
    fun sendFeedbackMail(name: String, mail: String, rating: Feedback.Rating, message: String, submissionId: String?)

    /**
     * Calculates the average of all the currently available feedback ratings.
     *
     * @return String corresponding to the average feedback rating ("happy", "sad", "neutral", "noRating").
     */
    fun calculateAverage(): String
}
