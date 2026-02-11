package site.rahoon.message.monolithic.common.websocket.config.auth

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.AuthTokenResolver
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.config.expiry.WebSocketSessionAuthInfoRegistry
import java.security.Principal

/**
 * STOMP CONNECT 수신 시 토큰을 검증하고 세션 속성에 [CommonAuthInfo]를 넣습니다.
 *
 * - CONNECT가 아니면 통과.
 * - 토큰: CONNECT 프레임 헤더(Authorization) 또는 Handshake 시 세션에 넣어둔 값([WebSocketAuthHandshakeHandler.ATTR_TOKEN]).
 * - 검증 성공: 세션 속성 [ATTR_AUTH_INFO]에 [CommonAuthInfo] 설정.
 * - 토큰 없음/검증 실패: [DomainException](CommonError.UNAUTHORIZED) → [WebSocketExceptionStompSubProtocolErrorHandler]가 ERROR 프레임 반환.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
class WebSocketConnectInterceptor(
    private val authTokenResolver: AuthTokenResolver,
    private val sessionAuthInfoRegistry: WebSocketSessionAuthInfoRegistry,
) : ChannelInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    @Nullable
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.CONNECT) return message

        val tokenFromHeader = accessor.getFirstNativeHeader("Authorization")?.takeIf { it.isNotBlank() }
        val tokenFromSession = accessor.sessionAttributes?.get(WebSocketAuthHandshakeHandler.ATTR_TOKEN)?.toString()?.takeIf { it.isNotBlank() }
        val token =
            tokenFromHeader
                ?: tokenFromSession
                ?: run {
                    log.warn(
                        "CONNECT 실패: 토큰 없음, sessionId={}, sessionAttributesKeys={}",
                        accessor.sessionId,
                        accessor.sessionAttributes?.keys?.toList(),
                    )
                    throw DomainException(CommonError.UNAUTHORIZED, mapOf("reason" to "Authorization required"))
                }

        val authInfo =
            try {
                authTokenResolver.verify(token)
            } catch (e: Exception) {
                log.warn("CONNECT 실패: 토큰 검증 실패, sessionId={}, cause={}", accessor.sessionId, e.message)
                throw DomainException(CommonError.UNAUTHORIZED, mapOf("reason" to (e.message ?: "Invalid token")), e)
            }

        (accessor.sessionAttributes as? MutableMap<String, Any>)?.set(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO, authInfo)
        accessor.user = object : Principal {
            override fun getName(): String = authInfo.userId
        }
        accessor.sessionId?.let { sessionAuthInfoRegistry.register(it, authInfo) }
        log.debug("CONNECT 성공: userId={}, sessionId={}", authInfo.userId, accessor.sessionId)
        return message
    }
}
