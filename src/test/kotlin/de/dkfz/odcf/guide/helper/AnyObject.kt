package de.dkfz.odcf.guide.helper

import de.dkfz.odcf.guide.entity.storage.Project
import de.dkfz.odcf.guide.entity.submissionData.*
import org.mockito.Mockito

interface AnyObject {

    private fun <T> uninitialized(): T = null as T
    fun <T> anyObject(): T {
        Mockito.anyObject<T>()
        return uninitialized()
    }

    private fun uninitializedSubmission(): Submission = ApiSubmission()
    fun anySubmission(): Submission {
        Mockito.anyObject<Submission>()
        return uninitializedSubmission()
    }

    private fun uninitializedSample(): Sample = Sample()
    fun anySample(): Sample {
        Mockito.anyObject<Sample>()
        return uninitializedSample()
    }

    private fun uninitializedTechnicalSample(): TechnicalSample = TechnicalSample()
    fun anyTechnicalSample(): TechnicalSample {
        Mockito.anyObject<TechnicalSample>()
        return uninitializedTechnicalSample()
    }

    private fun uninitializedFile(): File = File()
    fun anyFile(): File {
        Mockito.anyObject<File>()
        return uninitializedFile()
    }

    private fun uninitializedProject(): Project = Project()
    fun anyOtpCachedProject(): Project {
        Mockito.anyObject<Project>()
        return uninitializedProject()
    }
}
