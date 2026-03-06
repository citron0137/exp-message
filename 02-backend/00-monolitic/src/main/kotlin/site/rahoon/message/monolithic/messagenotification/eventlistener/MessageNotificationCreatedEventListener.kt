package site.rahoon.message.monolithic.messagenotification.eventlistener

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.messagenotification.application.MessageNotificationApplicationService
import site.rahoon.message.monolithic.messagenotification.application.MessageNotificationCriteria

/**
 * "메시지 기반 알림(NotificationMessage)" 도메인의 로컬 이벤트 리스너
 *
 * - Message 모듈에서 발행한 MessageEvent.Created를 비동기/트랜잭션 이후에 수신한다.
 * - MessageNotificationApplicationService를 통해 Notification 생성 및 이메일 전송을 수행한다.
 */
@Component
class MessageNotificationCreatedEventListener(
    private val messageNotificationApplicationService: MessageNotificationApplicationService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onMessageCreated(event: MessageEvent.Created) {
        val criteria =
            MessageNotificationCriteria.Create(
                messageId = event.id,
                chatRoomId = event.chatRoomId,
                senderUserId = event.userId,
                content = event.content,
                createdAt = event.createdAt,
            )
        messageNotificationApplicationService.handleMessageCreated(criteria)
    }
}
