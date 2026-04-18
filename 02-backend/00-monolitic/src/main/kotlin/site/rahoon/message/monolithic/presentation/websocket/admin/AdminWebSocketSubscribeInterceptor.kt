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
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

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
        if (accessor.command == StompCommand.SUBSCRIBE) {
            validateSubscription(accessor)
        }
        return message
    }

    private fun validateSubscription(accessor: StompHeaderAccessor) {
        val destination = accessor.destination ?: return
        val principal = AdminWebSocketSession.require(accessor)

        ADMIN_CHANNEL_CONVERSATIONS_TOPIC.find(destination)?.let {
            channelAccessPolicy.requireChannelRead(principal, it.groupValues[1])
        } ?: ADMIN_CONVERSATION_MESSAGES_TOPIC.find(destination)?.let {
            validateConversationMessageSubscription(principal, it.groupValues[1], it.groupValues[2])
        }
    }

    private fun validateConversationMessageSubscription(
        principal: AuthenticatedPrincipal,
        channelId: String,
        conversationId: String,
    ) {
        channelAccessPolicy.requireChannelRead(principal, channelId)
        if (!adminConversationReader.existsConversation(channelId, conversationId)) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                details = mapOf("channelId" to channelId, "conversationId" to conversationId),
            )
        }
    }

    companion object {
        val ADMIN_CHANNEL_CONVERSATIONS_TOPIC = Regex("^/topic/admin/channels/([^/]+)/conversations$")
        val ADMIN_CONVERSATION_MESSAGES_TOPIC = Regex("^/topic/admin/channels/([^/]+)/conversations/([^/]+)/messages$")
    }
}
