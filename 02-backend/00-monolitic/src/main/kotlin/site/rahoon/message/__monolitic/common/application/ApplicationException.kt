package site.rahoon.message.__monolitic.common.application

class ApplicationException(
    val errorCode: String,
    val errorMessage: String,
    val details: String? = null
) : RuntimeException(
    details?.let { "$errorMessage: $it" } ?: errorMessage
)

