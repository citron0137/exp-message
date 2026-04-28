package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessage
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageSenderType

interface ConversationMessageRepository {
    /**
     * Saves a conversation message.
     */
    fun save(message: ConversationMessage): ConversationMessage

    /**
     * Finds a message by idempotency key.
     */
    fun findByIdempotencyKey(
        conversationId: String,
        senderType: ConversationMessageSenderType,
        senderId: String,
        clientMessageId: String,
    ): ConversationMessage?

    /**
     * Finds visible messages after a conversation sequence.
     */
    fun findVisibleAfterSequence(
        conversationId: String,
        afterSequence: Long,
        limit: Int,
    ): List<ConversationMessage>
}
