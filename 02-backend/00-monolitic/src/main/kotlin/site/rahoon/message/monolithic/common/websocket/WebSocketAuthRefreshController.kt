package site.rahoon.message.monolithic.common.websocket

import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Controller
import site.rahoon.message.monolithic.common.auth.AuthTokenResolver
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.config.WebSocketAuthHandshakeHandler

/**
 * WebSocket 연결 유지 중 토큰 갱신용 엔드포인트.
 *
 * - destination: `/app/auth/refresh`
 * - 클라이언트: STOMP SEND 시 헤더에 새 액세스 토큰(Authorization) 전달
 * - 서버: 토큰 검증 후 해당 세션의 [WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO] 갱신. 연결 유지.
 * - 검증 실패: [DomainException](CommonError.UNAUTHORIZED) → [WebSocketStompErrorHandler]가 ERROR 프레임 payload에 code·message 전달
 */
@Controller
class WebSocketAuthRefreshController(
    private val authTokenResolver: AuthTokenResolver,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("auth/refresh")
    fun refresh(message: Message<*>) {
        val accessor = StompHeaderAccessor.wrap(message)
        val token =
            accessor.getFirstNativeHeader("Authorization")?.takeIf { it.isNotBlank() }
                ?: run {
                    log.warn("auth/refresh 실패: 토큰 없음, sessionId={}", accessor.sessionId)
                    throw DomainException(CommonError.UNAUTHORIZED, mapOf("reason" to "Authorization required"))
                }

        val authInfo: CommonAuthInfo =
            try {
                authTokenResolver.verify(token)
            } catch (e: Exception) {
                log.warn("auth/refresh 실패: 토큰 검증 실패, sessionId={}, cause={}", accessor.sessionId, e.message)
                throw DomainException(CommonError.UNAUTHORIZED, mapOf("reason" to (e.message ?: "Invalid token")), e)
            }

        (accessor.sessionAttributes as? MutableMap<String, Any>)?.set(
            WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO,
            authInfo,
        )
        log.debug("auth/refresh 성공: userId={}, sessionId={}", authInfo.userId, accessor.sessionId)
    }
}
