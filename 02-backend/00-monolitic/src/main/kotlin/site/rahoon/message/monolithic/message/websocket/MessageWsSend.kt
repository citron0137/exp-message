package site.rahoon.message.monolithic.message.websocket

import java.time.LocalDateTime

/**
 * WebSocket을 통해 전송되는 메시지 DTO
 */
object MessageWsSend {
    data class Detail(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val content: String,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun from(event: site.rahoon.message.monolithic.message.application.MessageCommandEvent.Send): Detail =
                Detail(
                    id = event.id,
                    chatRoomId = event.chatRoomId,
                    userId = event.userId,
                    content = event.content,
                    createdAt = event.createdAt,
                )
        }
    }
}
