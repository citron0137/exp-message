package site.rahoon.message.monolithic.test.websocket

import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Controller
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.annotation.WebSocketReply
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import site.rahoon.message.monolithic.common.websocket.reply.WebSocketReplyBody

/**
 * 테스트용 WebSocket(STOMP) 엔드포인트.
 *
 * - `/app/test/ping`: SEND만으로 연결/라우팅 확인 (응답 없음)
 * - `/app/test/echo`: Reply
 * - `/app/test/error`: SEND 시 [DomainException] 발생. [WebSocketMessageExceptionAdvice]에서 전역 처리.
 */
@Controller
class TestWebSocketController {

    @MessageMapping("test/ping")
    fun ping(@Suppress("UNUSED_PARAMETER") message: Message<*>) { // no-op, 연결/라우팅 테스트용
    }

    @MessageMapping("test/echo")
    @WebSocketReply
    fun echo(message: Message<Map<String, Any?>>): WebSocketReplyBody<Map<String, Any?>> {
        val accessor = StompHeaderAccessor.wrap(message)
        val authInfo = accessor.sessionAttributes?.get(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO) as? CommonAuthInfo
        if (authInfo == null) throw RuntimeException("인증정보 없음")
        val sessionId = accessor.sessionId
        val receiptId = accessor.receipt
        return WebSocketReplyBody(
            payload = message.payload,
            receiptId = receiptId,
            requestDestination = "/app/test/echo",
            websocketSessionId = sessionId,
        )
    }

    /**
     * SEND 시 DomainException 발생. [WebSocketMessageExceptionAdvice]에서 전역 처리 (ERROR 프레임 + exception 큐).
     */
    @MessageMapping("test/error")
    fun error(@Suppress("UNUSED_PARAMETER") message: Message<*>) {
        throw DomainException(
            CommonError.CLIENT_ERROR,
            mapOf("reason" to "Test WebSocket error endpoint"),
        )
    }
}


