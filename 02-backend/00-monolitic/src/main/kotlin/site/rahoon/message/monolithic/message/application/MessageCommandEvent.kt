package site.rahoon.message.monolithic.message.application

import java.time.LocalDateTime

/**
 * 메시지 명령 이벤트 DTO
 *
 * 사용자별 Redis 토픽을 통해 전파되는 이벤트
 * 도메인 객체를 포함하지 않고 순수 데이터만 전달
 */
sealed class MessageCommandEvent {
    /**
     * 메시지 전송 명령 이벤트
     *
     * @param recipientUserId 수신자 userId. 생성 시 주입 (SPEL 토픽 `/topic/user/{recipientUserId}/messages` 해석용)
     */
    data class Send(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val content: String,
        val createdAt: LocalDateTime,
        val recipientUserId: String,
    ) : MessageCommandEvent() {
        companion object {
            fun from(event: MessageEvent.Created, recipientUserId: String): Send =
                Send(
                    id = event.id,
                    chatRoomId = event.chatRoomId,
                    userId = event.userId,
                    content = event.content,
                    createdAt = event.createdAt,
                    recipientUserId = recipientUserId,
                )
        }
    }
}
