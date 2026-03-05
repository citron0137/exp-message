package site.rahoon.message.monolithic.messagenotification.application

import java.time.LocalDateTime

/**
 * MessageNotification Application 계층에서 사용하는 Criteria
 */
object MessageNotificationCriteria {
    /**
     * Message 기반 MessageNotification 생성용 Create Criteria
     */
    data class Create(
        val messageId: String,
        val chatRoomId: String,
        val senderUserId: String,
        val content: String,
        val createdAt: LocalDateTime,
    )
}
