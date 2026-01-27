package site.rahoon.message.monolithic.message.websocket

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import site.rahoon.message.monolithic.common.websocket.WebsocketSend
import site.rahoon.message.monolithic.message.application.MessageCommandEvent
import site.rahoon.message.monolithic.message.application.MessageCommandEventRelayPort

/**
 * WebSocket 메시지 전달 핸들러
 *
 * 순수하게 WebSocket을 통한 메시지 전달만 담당
 * 비즈니스 로직은 포함하지 않음
 */
@Component
class MessageWebSocketController(
    private val messageCommandEventRelayPort: MessageCommandEventRelayPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * WebSocket 연결될 때 호출
     */
    @Async
    @EventListener
    fun onConnect(event: SessionConnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val principal = accessor.user
        val userId = principal?.name

        if (userId != null) {
            log.info("WebSocket 연결됨 - UserId: $userId, SessionId: ${accessor.sessionId}")
            // TODO 중복 방지로직
            messageCommandEventRelayPort.subscribe(userId)
        } else {
            log.warn("WebSocket 연결 시 Principal이 없습니다 - SessionId: ${accessor.sessionId}")
        }
    }

    /**
     * WebSocket 끊어질때 호출
     */
    @Async
    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val principal = accessor.user
        val userId = principal?.name

        if (userId != null) {
            log.info("WebSocket 연결 해제됨 - UserId: $userId, SessionId: ${accessor.sessionId}")
            messageCommandEventRelayPort.unsubscribe(userId)
        } else {
            log.warn("WebSocket 연결 해제 시 Principal이 없습니다 - SessionId: ${accessor.sessionId}")
        }
    }

    @EventListener
    @WebsocketSend("/topic/chat-rooms/{chatRoomId}/messages")
    fun sendCreatedMessage(event: MessageCommandEvent.Send): MessageWsSend.Detail {
        return MessageWsSend.Detail.from(event)
    }
}
