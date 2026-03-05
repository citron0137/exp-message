package site.rahoon.message.monolithic.notificationemailjob.application.component

import site.rahoon.message.monolithic.notificationemailjob.domain.NotificationEmailJob

/**
 * EMAIL 알림 전송 포트
 *
 * 구현체에서 재시도, 백오프, 서킷브레이커 등의 정책을 적용한다.
 */
fun interface NotificationEmailSender {
    /**
     * 이메일 전송.
     *
     * STEP 1에서는 fire-and-forget 방식으로 동작하며,
     * 재시도/백오프/서킷브레이커 로직은 구현체 내부에서 처리한다.
     */
    fun send(job: NotificationEmailJob)
}
