package site.rahoon.message.monolithic.notification.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * 알림 도메인 모델 (유저/어드민이 조회하는 단위)
 *
 * STEP 1에서는 영속화하지 않고, 논리적인 "알림 자체"만 표현한다.
 * 실제 채널별 전송 상태는 [NotificationJob]이 담당한다.
 */
data class Notification(
    val id: String,
    val recipientUserId: String,
    val subject: String,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        /**
         * 채널에 독립적인 알림 생성 팩토리.
         *
         * 어떤 채널로 전송할지는 [NotificationJob]에서 결정한다.
         */
        fun create(
            recipientUserId: String,
            subject: String,
            content: String,
        ): Notification {
            val now = LocalDateTime.now()
            return Notification(
                id = UUID.randomUUID().toString(),
                recipientUserId = recipientUserId,
                subject = subject,
                content = content,
                createdAt = now,
            )
        }
    }
}
