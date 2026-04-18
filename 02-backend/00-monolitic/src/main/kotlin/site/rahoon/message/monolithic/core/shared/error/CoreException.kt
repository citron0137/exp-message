package site.rahoon.message.monolithic.core.shared.error

open class CoreException(
    val error: CoreError,
    val details: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(error.developerMessage, cause)
