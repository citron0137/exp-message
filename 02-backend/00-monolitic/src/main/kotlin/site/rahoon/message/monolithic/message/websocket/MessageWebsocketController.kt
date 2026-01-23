package site.rahoon.message.monolithic.message.websocket

import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.message.application.MessageEvent

/**
 * WebSocket 메시지 전달 핸들러
 *
 * 순수하게 WebSocket을 통한 메시지 전달만 담당
 * 비즈니스 로직은 포함하지 않음
 */
@Component
class MessageWebsocketController {

    @SendTo("/topic/chat-rooms/{chatRoomId}/messages")
    fun onCreated(
        @DestinationVariable chatRoomId: String,
        @Payload event: MessageEvent.Created
    ):MessageEvent.Created{
        return event
    }
}
