package site.rahoon.message.monolithic.core.conversation.exception

import site.rahoon.message.monolithic.core.shared.error.CoreException

class ConversationException(
    error: ConversationError,
    details: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : CoreException(error = error, details = details, cause = cause)
