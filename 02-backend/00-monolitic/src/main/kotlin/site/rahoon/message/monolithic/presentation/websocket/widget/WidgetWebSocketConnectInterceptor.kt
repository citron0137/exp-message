package site.rahoon.message.monolithic.presentation.websocket.widget

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionAttributeNames
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

@Component
class WidgetWebSocketConnectInterceptor(
    private val widgetAccessPolicy: WidgetAccessPolicy,
    private val visitorSessionPolicy: VisitorSessionPolicy,
) : ChannelInterceptor {
    /**
     * Authenticates widget visitors on STOMP CONNECT.
     */
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command == StompCommand.CONNECT) {
            resolveCredentials(accessor)?.let { connectWidgetSession(accessor, it) }
        }
        return message
    }

    private fun connectWidgetSession(
        accessor: StompHeaderAccessor,
        credentials: WidgetWebSocketCredentials,
    ) {
        val access = widgetAccessPolicy.requireAccessibleWidget(credentials.publicKey, credentials.origin)
        val session = visitorSessionPolicy.requireValidSession(credentials.visitorSessionToken, access.channel.id)
        val widgetSession =
            WidgetWebSocketSession(
                publicKey = credentials.publicKey,
                origin = credentials.origin,
                visitorSessionToken = credentials.visitorSessionToken,
                channelId = access.channel.id,
                visitorId = session.visitorId,
                visitorSessionId = session.id,
                expiresAt = session.expiresAt,
            )
        (accessor.sessionAttributes as? MutableMap<String, Any>)
            ?.set(WebSocketSessionAttributeNames.WIDGET_SESSION, widgetSession)
    }

    /**
     * Resolves widget credentials from CONNECT headers and handshake attributes.
     */
    private fun resolveCredentials(accessor: StompHeaderAccessor): WidgetWebSocketCredentials? {
        val publicKey =
            accessor.getFirstNativeHeader("publicKey")?.takeIf { it.isNotBlank() }
                ?: accessor.sessionAttributes
                    ?.get(WebSocketSessionAttributeNames.WIDGET_PUBLIC_KEY)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
        val visitorSessionToken =
            accessor.getFirstNativeHeader("visitorSessionToken")?.takeIf { it.isNotBlank() }
                ?: accessor.sessionAttributes
                    ?.get(WebSocketSessionAttributeNames.WIDGET_VISITOR_SESSION_TOKEN)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
        if (publicKey == null && visitorSessionToken == null) {
            return null
        }
        val origin =
            accessor.getFirstNativeHeader("origin")?.takeIf { it.isNotBlank() }
                ?: accessor.getFirstNativeHeader("Origin")?.takeIf { it.isNotBlank() }
                ?: accessor.sessionAttributes
                    ?.get(WebSocketSessionAttributeNames.WIDGET_ORIGIN)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
        if (publicKey == null || visitorSessionToken == null || origin == null) {
            throw ConversationException(
                error = ConversationError.VISITOR_SESSION_NOT_FOUND,
                details = mapOf("reason" to "Widget WebSocket credentials are incomplete."),
            )
        }
        return WidgetWebSocketCredentials(
            publicKey = publicKey,
            origin = origin,
            visitorSessionToken = visitorSessionToken,
        )
    }
}

private data class WidgetWebSocketCredentials(
    val publicKey: String,
    val origin: String,
    val visitorSessionToken: String,
)
