package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.conversationmessage

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ConversationMessageJpaRepository : JpaRepository<ConversationMessageEntity, String> {
    /**
     * Finds a message by its idempotency key.
     */
    fun findByConversationIdAndSenderTypeAndSenderIdAndClientMessageId(
        conversationId: String,
        senderType: String,
        senderId: String,
        clientMessageId: String,
    ): ConversationMessageEntity?

    /**
     * Finds visible messages after the requested sequence.
     */
    fun findByConversationIdAndStatusAndSequenceGreaterThanOrderBySequenceAsc(
        conversationId: String,
        status: String,
        sequence: Long,
        pageable: Pageable,
    ): List<ConversationMessageEntity>
}
