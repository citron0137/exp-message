package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class ConversationMessage(
    val id: String,
    val conversationId: String,
    val channelId: String,
    val sequence: Long,
    val senderType: ConversationMessageSenderType,
    val senderId: String,
    val clientMessageId: String,
    val type: ConversationMessageType,
    val content: MessageContent,
    val status: ConversationMessageStatus,
    val createdAt: LocalDateTime,
) {
    companion object {
        /**
         * Creates a visible visitor text message.
         */
        fun visitorText(
            conversationId: String,
            channelId: String,
            visitorId: String,
            sequence: Long,
            clientMessageId: String,
            content: MessageContent,
        ): ConversationMessage =
            ConversationMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                channelId = channelId,
                sequence = sequence,
                senderType = ConversationMessageSenderType.VISITOR,
                senderId = visitorId,
                clientMessageId = clientMessageId,
                type = ConversationMessageType.TEXT,
                content = content,
                status = ConversationMessageStatus.VISIBLE,
                createdAt = LocalDateTime.now(),
            )
    }
}
