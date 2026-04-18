package site.rahoon.message.monolithic.presentation.websocket.widget

import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionAttributeNames
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

object WidgetWebSocketSessionAccessor {
    /**
     * Returns a widget session from STOMP session attributes.
     */
    fun get(accessor: StompHeaderAccessor): WidgetWebSocketSession? =
        accessor.sessionAttributes?.get(WebSocketSessionAttributeNames.WIDGET_SESSION) as? WidgetWebSocketSession

    /**
     * Requires a widget session from STOMP session attributes.
     */
    fun require(accessor: StompHeaderAccessor): WidgetWebSocketSession =
        get(accessor)
            ?: throw ConversationException(
                error = ConversationError.VISITOR_SESSION_NOT_FOUND,
                details = mapOf("reason" to "Widget WebSocket session is missing."),
            )
}
