package site.rahoon.message.monolithic.message.websocket

import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.controller.MessageResponse

/**
 * [MessageEvent.Created] 수신 시 /topic/chat-rooms/{id}/messages 로 브로드캐스트.
 * @Async: REST 201 응답을 브로드캐스트 완료에 묶지 않기 위해 비동기 처리.
 */
@Component
class MessageCreatedEventListener(
    private val simpMessagingTemplate: SimpMessagingTemplate,
) {
    @Async
    @EventListener
    fun onMessageCreated(event: MessageEvent.Created) {
        val message = event.message
        val payload = MessageResponse.Detail.from(message)
        simpMessagingTemplate.convertAndSend(
            "/topic/chat-rooms/${message.chatRoomId}/messages",
            payload,
        )
    }
}
