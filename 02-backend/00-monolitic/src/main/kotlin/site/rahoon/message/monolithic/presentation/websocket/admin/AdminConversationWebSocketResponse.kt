package site.rahoon.message.monolithic.presentation.websocket.admin

import site.rahoon.message.monolithic.core.conversation.application.facade.ConversationMessageResult
import java.time.LocalDateTime

object AdminConversationWebSocketTopics {
    /**
     * Returns the admin inbox topic for a channel.
     */
    fun channelConversations(channelId: String): String = "/topic/admin/channels/$channelId/conversations"

    /**
     * Returns the admin message topic for a conversation.
     */
    fun conversationMessages(
        channelId: String,
        conversationId: String,
    ): String = "/topic/admin/channels/$channelId/conversations/$conversationId/messages"
}

object AdminConversationWebSocketResponse {
    data class ConversationChanged(
        val channelId: String,
        val conversationId: String,
        val reason: String,
        val lastMessageSequence: Long,
        val occurredAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Maps a stored message to an inbox refresh event.
             */
            fun from(
                result: ConversationMessageResult,
                reason: String,
            ): ConversationChanged =
                ConversationChanged(
                    channelId = result.channelId,
                    conversationId = result.conversationId,
                    reason = reason,
                    lastMessageSequence = result.sequence,
                    occurredAt = result.createdAt,
                )
        }
    }

    data class Message(
        val id: String,
        val conversationId: String,
        val channelId: String,
        val sequence: Long,
        val senderType: String,
        val senderId: String,
        val clientMessageId: String,
        val type: String,
        val content: String,
        val status: String,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Maps an application message result to an admin WebSocket response.
             */
            fun from(result: ConversationMessageResult): Message =
                Message(
                    id = result.id,
                    conversationId = result.conversationId,
                    channelId = result.channelId,
                    sequence = result.sequence,
                    senderType = result.senderType.name,
                    senderId = result.senderId,
                    clientMessageId = result.clientMessageId,
                    type = result.type.name,
                    content = result.content,
                    status = result.status.name,
                    createdAt = result.createdAt,
                )
        }
    }
}
