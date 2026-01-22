package site.rahoon.message.monolithic.message.websocket

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.application.MessageEventSubscriber

/**
 * WebSocket 메시지 전달 핸들러
 *
 * 순수하게 WebSocket을 통한 메시지 전달만 담당
 * 비즈니스 로직은 포함하지 않음
 */
@Component
class WebSocketMessageHandler(
    private val simpMessagingTemplate: SimpMessagingTemplate,
): MessageEventSubscriber {

    override fun onCreated(event: MessageEvent.Created) {
        sendMessage(event)
    }

    /**
     * 메시지 생성 이벤트를 WebSocket으로 전송
     */
    private fun sendMessage(event: MessageEvent.Created) {
        val destination = "/topic/chat-rooms/${event.chatRoomId}/messages"
        simpMessagingTemplate.convertAndSend(destination, event)
    }

}
