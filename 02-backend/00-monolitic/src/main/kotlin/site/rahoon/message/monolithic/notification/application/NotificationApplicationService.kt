package site.rahoon.message.monolithic.notification.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.notification.domain.Notification
import site.rahoon.message.monolithic.notificationemailjob.application.NotificationEmailJobApplicationService
import site.rahoon.message.monolithic.notificationemailjob.application.NotificationEmailJobCriteria
import site.rahoon.message.monolithic.user.domain.UserDomainService

/**
 * 공통 Notification Application Service
 *
 * - 다양한 도메인에서 Notification 생성/전송을 위해 사용하는 진입점
 * - Notification 객체와 EMAIL 채널용 Job을 생성한 뒤 전송한다.
 */
@Service
class NotificationApplicationService(
    private val userDomainService: UserDomainService,
    private val notificationEmailJobApplicationService: NotificationEmailJobApplicationService,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Notification 생성 및 전송
     */
    fun create(criteria: NotificationCriteria.Create) {
        // 받는 사람이 없는 경우 Exit
        if (criteria.recipientUserIds.isEmpty()) {
            log.debug { "${"수신자 정보가 없어 Notification을 생성하지 않습니다. criteria={}"} $criteria" }
            return
        }
        val users = userDomainService.getByIds(criteria.recipientUserIds)

        // 1) 유저 기준으로 Notification 생성
        val notifications = users.map { user ->
            val subject = applyRecipientPlaceholders(criteria.subject, user.nickname)
            val content = applyRecipientPlaceholders(criteria.content, user.nickname)
            val notification =
                Notification.create(
                    recipientUserId = user.id,
                    subject = subject,
                    content = content,
                )
            user to notification
        }

        // 2) 이메일이 있는 경우 이메일 전송 요청
        if (criteria.email != null) {
            val notificationEmailJobs = notifications.map { (user, notification) ->
                val subject = applyRecipientPlaceholders(criteria.email.subject, user.nickname)
                val content = applyRecipientPlaceholders(criteria.email.content, user.nickname)
                val emailJobCriteria = NotificationEmailJobCriteria.Create(
                    notificationId = notification.id,
                    email = user.email,
                    subject = subject,
                    content = content,
                )
                emailJobCriteria
            }
            notificationEmailJobApplicationService.createAll(notificationEmailJobs)
        }
    }

    private fun applyRecipientPlaceholders(
        template: String,
        recipientNickname: String,
    ): String = template.replace("{recipientNickname}", recipientNickname)
}
