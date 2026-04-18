package site.rahoon.message.monolithic.presentation.websocket.widget

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

@Component
class WidgetWebSocketSessionExpiryInterceptor : ChannelInterceptor {
    /**
     * Rejects inbound widget frames after the visitor session expires.
     */
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command == StompCommand.CONNECT) return message
        val session = WidgetWebSocketSessionAccessor.get(accessor) ?: return message
        if (!session.expiresAt.isAfter(LocalDateTime.now())) {
            throw ConversationException(
                error = ConversationError.VISITOR_SESSION_EXPIRED,
                details = mapOf("visitorSessionId" to session.visitorSessionId),
            )
        }
        return message
    }
}
