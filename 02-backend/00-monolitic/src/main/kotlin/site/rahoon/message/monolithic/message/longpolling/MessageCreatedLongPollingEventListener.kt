package site.rahoon.message.monolithic.message.longpolling

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.controller.MessageResponse

/**
 * [MessageEvent.Created] 수신 시 Long Polling 대기 중인 요청들에게 응답.
 * @Async: REST 201 응답을 Long Polling 응답 완료에 묶지 않기 위해 비동기 처리.
 */
@Component
class MessageCreatedLongPollingEventListener(
    private val longPollingManager: MessageLongPollingManager,
) {
    @Async
    @EventListener
    fun onMessageCreated(event: MessageEvent.Created) {
        val message = event.message
        val payload = MessageResponse.Detail.from(message)
        longPollingManager.send(message.chatRoomId, payload)
    }
}
