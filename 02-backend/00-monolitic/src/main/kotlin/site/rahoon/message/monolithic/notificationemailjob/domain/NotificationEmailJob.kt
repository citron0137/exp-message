package site.rahoon.message.monolithic.notificationemailjob.domain

import java.time.LocalDateTime

/**
 * EMAIL 채널 전용 알림 전송 작업 (Job) 모델
 *
 * Notification은 유저/어드민이 조회하는 단위의 "알림 자체"이고,
 * NotificationEmailJob은 EMAIL 채널 기준 전송 상태를 표현한다.
 *
 * STEP 1에서는 EMAIL 채널만 사용하며, 영속화 없이 메모리 상 모델로만 존재한다.
 * 이후 Push / SMS / Webhook 등을 위한 별도 Job 모델을 추가할 수 있다.
 */
data class NotificationEmailJob(
    val notificationId: String,
    val email: String,
    val subject: String,
    val content: String,
    val attempt: Int,
    val maxAttempts: Int,
    val lastErrorMessage: String? = null,
    val lastTriedAt: LocalDateTime? = null,
)
