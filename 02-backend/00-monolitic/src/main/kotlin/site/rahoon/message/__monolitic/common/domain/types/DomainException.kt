package site.rahoon.message.__monolitic.common.domain.types

class DomainException(
    val error: DomainError,
    val details: Map<String, Any>? = null,
    cause: Throwable? = null
) : RuntimeException(
    details?.let { 
        val detailsStr = it.entries.joinToString(", ") { (k, v) -> "$k=$v" }
        "${error.message}: $detailsStr"
    } ?: error.message,
    cause
)

