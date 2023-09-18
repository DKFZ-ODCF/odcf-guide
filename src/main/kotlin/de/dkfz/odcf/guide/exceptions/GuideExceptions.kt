package de.dkfz.odcf.guide.exceptions

import de.dkfz.odcf.guide.entity.submissionData.Submission

enum class ApiType {
    ILSe,
    OTP,
    ITCF,
    OTRS
}

open class GuideRuntimeException(message: String) : Exception(message)

class MissingPropertyException(propertyName: String) : GuideRuntimeException("property not given '$propertyName'")

class JsonExtractorException(message: String) : GuideRuntimeException(message)

class GuideMergerException(message: String) : GuideRuntimeException(message)

class SubmissionNotFinishedException(message: String) : GuideRuntimeException(message)

class ExternalApiReadException(message: String, apiType: ApiType) : GuideRuntimeException("$apiType: $message")

class MissingRuntimeOptionException(message: String) : GuideRuntimeException(message)

class OutputFileNotWritableException(message: String) : GuideRuntimeException(message)

class ColumnNotFoundException(message: String) : GuideRuntimeException(message)

class RowNotFoundException(message: String) : GuideRuntimeException(message)

class FastQFileNameRejectedException(message: String) : GuideRuntimeException(message)

class SampleNamesDontMatchException(message: String) : GuideRuntimeException(message)

class Md5SumsDontMatchException(message: String) : GuideRuntimeException(message)

class Md5SumFoundInDifferentSubmission(
    message: String,
    val submissions: List<Submission>,
    val countMd5InSubmissions: Map<Submission, Int>
) : GuideRuntimeException(message)

class UserNotFoundException(message: String) : GuideRuntimeException(message)

class DuplicatedImportAliasException(message: String) : GuideRuntimeException(message)

class ParserException(message: String) : GuideRuntimeException(message)

class SubmissionNotFoundException(message: String) : GuideRuntimeException(message)

class RuntimeOptionsNotFoundException(optionName: String) : GuideRuntimeException("runtime options '$optionName' was not found in DB")

class JobAlreadySubmittedException(message: String) : GuideRuntimeException(message)
