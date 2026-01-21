package site.rahoon.message.monolithic.message.sse

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.controller.MessageResponse

/**
 * [MessageEvent.Created] 수신 시 SSE를 통해 채팅방 구독자들에게 브로드캐스트.
 * @Async: REST 201 응답을 브로드캐스트 완료에 묶지 않기 위해 비동기 처리.
 */
@Component
class MessageCreatedSseEventListener(
    private val sseEmitterManager: MessageSseEmitterManager,
) {
    @Async
    @EventListener
    fun onMessageCreated(event: MessageEvent.Created) {
        val message = event.message
        val payload = MessageResponse.Detail.from(message)
        sseEmitterManager.send(message.chatRoomId, payload)
    }
}
