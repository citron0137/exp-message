package site.rahoon.message.monolithic.test.websocket

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Controller
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketSend
import site.rahoon.message.monolithic.common.websocket.config.WebSocketAuthHandshakeHandler

/**
 * 테스트용 WebSocket(STOMP) 엔드포인트.
 *
 * - `/app/test/ping`: SEND만으로 연결/라우팅 확인 (응답 없음)
 * - `/app/test/echo`: SEND body를 [TestEchoEvent]로 발행 → [WebsocketSend]로 `/topic/user/{userId}/test/echo` 전달.
 *   클라이언트는 구독 시 `/topic/user/{본인 userId}/test/echo` 사용.
 * - `/app/test/error`: SEND 시 [DomainException] 발생. ERROR 프레임/에러 핸들러 테스트용.
 */
@Controller
class TestWebSocketController(
    private val eventPublisher: ApplicationEventPublisher,
) {

    @MessageMapping("test/ping")
    fun ping(@Suppress("UNUSED_PARAMETER") message: Message<*>) {
        // no-op, 연결/라우팅 테스트용
    }

    @MessageMapping("test/echo")
    fun echo(message: Message<*>) {
        val accessor = StompHeaderAccessor.wrap(message)
        val authInfo =
            accessor.sessionAttributes?.get(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO) as? CommonAuthInfo
                ?: return
        val payload = (message.payload as? String).orEmpty()
        eventPublisher.publishEvent(TestEchoEvent(userId = authInfo.userId, payload = payload))
    }

    @EventListener
    @WebsocketSend("/topic/user/{userId}/test/echo")
    fun onTestEcho(event: TestEchoEvent): TestEchoPayload =
        TestEchoPayload(userId = event.userId, echo = event.payload)

    /**
     * SEND 시 DomainException 발생. WebSocketStompErrorHandler / ERROR 프레임 테스트용.
     */
    @MessageMapping("test/error")
    fun error(@Suppress("UNUSED_PARAMETER") message: Message<*>) {
        throw DomainException(
            CommonError.CLIENT_ERROR,
            mapOf("reason" to "Test WebSocket error endpoint"),
        )
    }
}

/** test/echo 이벤트. 발행 시 [TestWebSocketController.onTestEcho]가 수신해 [WebsocketSend]로 전달한다. */
data class TestEchoEvent(
    val userId: String,
    val payload: String,
)

/** [WebsocketSend] 반환 DTO. userId로 토픽 경로 해석, 전체가 메시지 body로 전송된다. */
data class TestEchoPayload(
    val userId: String,
    val echo: String,
)
