package site.rahoon.message.monolithic.presentation.websocket.admin

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationReader
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

@Component
class AdminWebSocketSubscribeInterceptor(
    private val channelAccessPolicy: ChannelAccessPolicy,
    private val adminConversationReader: AdminConversationReader,
) : ChannelInterceptor {
    /**
     * Verifies admin conversation topic subscriptions.
     */
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.SUBSCRIBE) return message
        val destination = accessor.destination ?: return message
        val principal = AdminWebSocketSession.require(accessor)

        val inboxMatch = ADMIN_CHANNEL_CONVERSATIONS_TOPIC.find(destination)
        if (inboxMatch != null) {
            channelAccessPolicy.requireChannelRead(principal, inboxMatch.groupValues[1])
            return message
        }

        val messageMatch = ADMIN_CONVERSATION_MESSAGES_TOPIC.find(destination)
        if (messageMatch != null) {
            val channelId = messageMatch.groupValues[1]
            val conversationId = messageMatch.groupValues[2]
            channelAccessPolicy.requireChannelRead(principal, channelId)
            if (!adminConversationReader.existsConversation(channelId, conversationId)) {
                throw ConversationException(
                    error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                    details = mapOf("channelId" to channelId, "conversationId" to conversationId),
                )
            }
        }
        return message
    }

    companion object {
        val ADMIN_CHANNEL_CONVERSATIONS_TOPIC = Regex("^/topic/admin/channels/([^/]+)/conversations$")
        val ADMIN_CONVERSATION_MESSAGES_TOPIC = Regex("^/topic/admin/channels/([^/]+)/conversations/([^/]+)/messages$")
    }
}
