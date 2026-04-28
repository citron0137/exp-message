package site.rahoon.message.monolithic.core.conversation.domain

import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

class MessageContent private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 4000

        /**
         * Creates validated text message content.
         */
        fun text(rawValue: String): MessageContent {
            val normalized = rawValue.trim()
            if (normalized.isBlank() || normalized.length > MAX_LENGTH) {
                throw ConversationException(
                    error = ConversationError.INVALID_MESSAGE_CONTENT,
                    details = mapOf("maxLength" to MAX_LENGTH.toString()),
                )
            }
            return MessageContent(normalized)
        }
    }
}
