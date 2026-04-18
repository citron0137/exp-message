package site.rahoon.message.monolithic.presentation.websocket.admin

import org.springframework.core.Ordered
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.auth.CommonAuthRole
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionAuthInfoRegistry
import site.rahoon.message.monolithic.core.iam.access.application.facade.AccessFacade
import java.security.Principal

@Component
class AdminWebSocketConnectInterceptor(
    private val accessFacade: AccessFacade,
    private val sessionAuthInfoRegistry: WebSocketSessionAuthInfoRegistry,
) : ChannelInterceptor {
    /**
     * Verifies core admin access tokens before the legacy WebSocket auth interceptor runs.
     */
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.CONNECT) return message
        if (accessor.sessionAttributes?.containsKey(AdminWebSocketSession.ATTR_PRINCIPAL) == true) {
            return message
        }

        val token =
            accessor.getFirstNativeHeader("Authorization")?.takeIf { it.isNotBlank() }
                ?: accessor.sessionAttributes
                    ?.get(WebSocketAuthHandshakeHandler.ATTR_TOKEN)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                ?: return message

        val principal =
            try {
                accessFacade.verifyAccessToken(token)
            } catch (e: Exception) {
                return message
            }

        /*
         * The shared WebSocket stack predates core IAM and expects CommonAuthInfo for
         * session expiry/heartbeat bookkeeping. We keep the actual authorization decision
         * in AdminWebSocketSession as an AuthenticatedPrincipal, and also write the shared
         * auth shape so existing expiry infrastructure can close the connection when the
         * core access token expires. Mapping PLATFORM_ADMIN to ADMIN and CHANNEL_USER to
         * USER is intentionally coarse; admin topic authorization below still uses the
         * core principal and channel membership policy as the source of truth.
         */
        val commonAuthInfo =
            CommonAuthInfo(
                userId = principal.userId,
                sessionId = principal.sessionId,
                expiresAt = principal.expiresAt,
                role = if (principal.isPlatformAdmin()) CommonAuthRole.ADMIN else CommonAuthRole.USER,
            )
        (accessor.sessionAttributes as? MutableMap<String, Any>)?.set(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO, commonAuthInfo)
        AdminWebSocketSession.store(accessor, principal)
        accessor.user =
            Principal {
                principal.userId
            }
        accessor.sessionId?.let { sessionAuthInfoRegistry.register(it, commonAuthInfo) }
        return message
    }

    companion object {
        const val ORDER = Ordered.HIGHEST_PRECEDENCE + 15
    }
}
