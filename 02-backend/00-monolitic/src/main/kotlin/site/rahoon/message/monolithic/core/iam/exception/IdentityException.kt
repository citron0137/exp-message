package site.rahoon.message.monolithic.core.iam.exception

import site.rahoon.message.monolithic.core.shared.error.CoreException

class IdentityException(
    error: IdentityError,
    details: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : CoreException(error = error, details = details, cause = cause)
