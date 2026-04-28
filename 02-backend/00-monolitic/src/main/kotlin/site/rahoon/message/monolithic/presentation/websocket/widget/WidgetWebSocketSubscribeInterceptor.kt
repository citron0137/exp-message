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
        if (accessor.command == StompCommand.SUBSCRIBE) {
            validateSubscription(accessor)
        }
        return message
    }

    private fun validateSubscription(accessor: StompHeaderAccessor) {
        val conversationId = resolveConversationId(accessor) ?: return
        val session = WidgetWebSocketSessionAccessor.require(accessor)
        conversationVisitorAccessPolicy.requireReadableConversation(
            conversationId = conversationId,
            channelId = session.channelId,
            visitorId = session.visitorId,
        )
    }

    private fun resolveConversationId(accessor: StompHeaderAccessor): String? =
        accessor.destination
            ?.let { WIDGET_CONVERSATION_TOPIC.find(it) }
            ?.groupValues
            ?.get(1)

    companion object {
        val WIDGET_CONVERSATION_TOPIC = Regex("^/topic/widget/conversations/([^/]+)/messages$")
    }
}
