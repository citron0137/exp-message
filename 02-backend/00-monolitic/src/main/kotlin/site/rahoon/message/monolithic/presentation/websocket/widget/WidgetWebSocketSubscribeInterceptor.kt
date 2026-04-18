package site.rahoon.message.monolithic.presentation.websocket.widget

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.service.ConversationVisitorAccessPolicy

@Component
class WidgetWebSocketSubscribeInterceptor(
    private val conversationVisitorAccessPolicy: ConversationVisitorAccessPolicy,
) : ChannelInterceptor {
    /**
     * Verifies widget conversation topic subscriptions.
     */
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.SUBSCRIBE) return message
        val destination = accessor.destination ?: return message
        val conversationId = WIDGET_CONVERSATION_TOPIC.find(destination)?.groupValues?.get(1) ?: return message
        val session = WidgetWebSocketSessionAccessor.require(accessor)
        conversationVisitorAccessPolicy.requireReadableConversation(
            conversationId = conversationId,
            channelId = session.channelId,
            visitorId = session.visitorId,
        )
        return message
    }

    companion object {
        val WIDGET_CONVERSATION_TOPIC = Regex("^/topic/widget/conversations/([^/]+)/messages$")
    }
}
