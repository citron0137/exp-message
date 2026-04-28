package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.conversationmessage

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessage
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageSenderType
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageStatus
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageType
import site.rahoon.message.monolithic.core.conversation.domain.MessageContent

@Repository
class ConversationMessageRepositoryAdapter(
    private val jpaRepository: ConversationMessageJpaRepository,
) : ConversationMessageRepository {
    /**
     * Saves a conversation message through JPA.
     */
    override fun save(message: ConversationMessage): ConversationMessage = jpaRepository.save(message.toEntity()).toDomain()

    /**
     * Finds a message by idempotency key through JPA.
     */
    override fun findByIdempotencyKey(
        conversationId: String,
        senderType: ConversationMessageSenderType,
        senderId: String,
        clientMessageId: String,
    ): ConversationMessage? =
        jpaRepository
            .findByConversationIdAndSenderTypeAndSenderIdAndClientMessageId(
                conversationId = conversationId,
                senderType = senderType.name,
                senderId = senderId,
                clientMessageId = clientMessageId,
            )?.toDomain()

    /**
     * Finds visible messages after a sequence through JPA.
     */
    override fun findVisibleAfterSequence(
        conversationId: String,
        afterSequence: Long,
        limit: Int,
    ): List<ConversationMessage> =
        jpaRepository
            .findByConversationIdAndStatusAndSequenceGreaterThanOrderBySequenceAsc(
                conversationId = conversationId,
                status = ConversationMessageStatus.VISIBLE.name,
                sequence = afterSequence,
                pageable = PageRequest.of(0, limit),
            ).map { it.toDomain() }

    /**
     * Maps a conversation message domain object to a JPA entity.
     */
    private fun ConversationMessage.toEntity(): ConversationMessageEntity =
        ConversationMessageEntity(
            id = id,
            conversationId = conversationId,
            channelId = channelId,
            sequence = sequence,
            senderType = senderType.name,
            senderId = senderId,
            clientMessageId = clientMessageId,
            type = type.name,
            content = content.value,
            status = status.name,
            createdAt = createdAt,
        )

    /**
     * Maps a conversation message JPA entity to a domain object.
     */
    private fun ConversationMessageEntity.toDomain(): ConversationMessage =
        ConversationMessage(
            id = id,
            conversationId = conversationId,
            channelId = channelId,
            sequence = sequence,
            senderType = ConversationMessageSenderType.valueOf(senderType),
            senderId = senderId,
            clientMessageId = clientMessageId,
            type = ConversationMessageType.valueOf(type),
            content = MessageContent.text(content),
            status = ConversationMessageStatus.valueOf(status),
            createdAt = createdAt,
        )
}
