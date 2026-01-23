package site.rahoon.message.monolithic.message.websocket

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
class MessageWebSocketHandler (
    private val messageWebsocketController: MessageWebsocketController,
): MessageEventSubscriber {
    override fun onCreated(event: MessageEvent.Created) {
        messageWebsocketController.onCreated(event.chatRoomId, event)
    }
}
