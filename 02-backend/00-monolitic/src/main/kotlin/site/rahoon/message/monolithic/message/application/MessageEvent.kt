package site.rahoon.message.monolithic.message.application

import site.rahoon.message.monolithic.message.domain.Message

/**
 * 메시지 도메인 관련 애플리케이션 이벤트.
 */
sealed interface MessageEvent {
    /**
     * 메시지 생성 시 발행.
     * WebSocket 등에서 구독하여 /topic/chat-rooms/{id}/messages 로 브로드캐스트할 때 사용.
     */
    data class Created(
        val message: Message,
    ) : MessageEvent
}
