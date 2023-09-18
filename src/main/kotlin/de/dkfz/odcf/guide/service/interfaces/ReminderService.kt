package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.submissionData.Submission

interface ReminderService {

    /**
     * Sends a reminder mail that a given submission is open in the GUIDE and the technical data from ILSe has been received,
     * if the property for the sending of reminders (`application.mails.reminders.sendmail`) is set to `true`
     * in the YML file for the run configurations.
     *
     * @param submission Submission for which the technical data has been received
     */
    fun sendDataReceivedReminderMail(submission: Submission)
}
