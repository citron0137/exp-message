package site.rahoon.message.monolithic.presentation.websocket.admin

import org.slf4j.LoggerFactory
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
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import java.security.Principal

@Component
class AdminWebSocketConnectInterceptor(
    private val accessFacade: AccessFacade,
    private val sessionAuthInfoRegistry: WebSocketSessionAuthInfoRegistry,
) : ChannelInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Verifies core admin access tokens before the legacy WebSocket auth interceptor runs.
     */
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (shouldVerifyCoreToken(accessor)) {
            resolveToken(accessor)
                ?.let { verifyCorePrincipalOrNull(it, accessor) }
                ?.let { storePrincipal(accessor, it) }
        }
        return message
    }

    private fun shouldVerifyCoreToken(accessor: StompHeaderAccessor): Boolean =
        accessor.command == StompCommand.CONNECT &&
            accessor.sessionAttributes?.containsKey(AdminWebSocketSession.ATTR_PRINCIPAL) != true

    private fun resolveToken(accessor: StompHeaderAccessor): String? =
        accessor.getFirstNativeHeader("Authorization")?.takeIf { it.isNotBlank() }
            ?: accessor.sessionAttributes
                ?.get(WebSocketAuthHandshakeHandler.ATTR_TOKEN)
                ?.toString()
                ?.takeIf { it.isNotBlank() }

    private fun verifyCorePrincipalOrNull(
        token: String,
        accessor: StompHeaderAccessor,
    ) = runCatching { accessFacade.verifyAccessToken(token) }
        .onFailure {
            log.debug(
                "Core admin WebSocket token rejected; falling back to legacy auth. sessionId={}",
                accessor.sessionId,
                it,
            )
        }.getOrNull()

    private fun storePrincipal(
        accessor: StompHeaderAccessor,
        principal: AuthenticatedPrincipal,
    ) {
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
        (accessor.sessionAttributes as? MutableMap<String, Any>)
            ?.set(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO, commonAuthInfo)
        AdminWebSocketSession.store(accessor, principal)
        accessor.user =
            Principal {
                principal.userId
            }
        accessor.sessionId?.let { sessionAuthInfoRegistry.register(it, commonAuthInfo) }
    }

    companion object {
        const val ORDER = Ordered.HIGHEST_PRECEDENCE + 15
    }
}
