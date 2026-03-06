package site.rahoon.message.monolithic.messagenotification.application

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.monolithic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.monolithic.messagenotification.application.component.MessageNotificationTextTemplateFactory
import site.rahoon.message.monolithic.notification.application.NotificationApplicationService
import site.rahoon.message.monolithic.notification.application.NotificationCriteria
import site.rahoon.message.monolithic.user.domain.UserDomainService

/**
 * "메시지 기반 알림(MessageNotification)" 도메인의 Application Service
 *
 * - MessageEvent.Created를 기반으로 Notification을 생성하고
 * - EMAIL 채널용 NotificationJob을 만들어 전송한다.
 */
@Service
class MessageNotificationApplicationService(
    private val chatRoomDomainService: ChatRoomDomainService,
    private val chatRoomMemberApplicationService: ChatRoomMemberApplicationService,
    private val userDomainService: UserDomainService,
    private val notificationApplicationService: NotificationApplicationService,
    private val messageNotificationTextTemplateFactory: MessageNotificationTextTemplateFactory,
) {
    /**
     * 메시지 생성 기반 알림 생성 및 이메일 전송
     */
    fun handleMessageCreated(criteria: MessageNotificationCriteria.Create) {
        // 채팅 방 및 송신자 조회
        val chatRoom = chatRoomDomainService.getById(criteria.chatRoomId)
        val sender = userDomainService.getById(criteria.senderUserId)

        // 수신자 id 조회
        val members = chatRoomMemberApplicationService.getByChatRoomId(criteria.chatRoomId)
        if (members.isEmpty()) return
        val recipientUserIds = members.map { it.userId }.filter { it != criteria.senderUserId }.distinct()
        if (recipientUserIds.isEmpty()) return

        // 생성
        val subjectTemplate = messageNotificationTextTemplateFactory.buildSubject(chatRoomName = chatRoom.name)
        val contentTemplate = messageNotificationTextTemplateFactory.buildContent(sender.nickname, criteria.content)
        val emailSubjectTemplate = messageNotificationTextTemplateFactory.buildEmailSubject(chatRoomName = chatRoom.name)
        val emailContentTemplate =
            messageNotificationTextTemplateFactory.buildEmailContent(
                chatRoomName = chatRoom.name,
                senderNickname = sender.nickname,
                messageContent = criteria.content,
                messageCreatedAt = criteria.createdAt,
                recipientNickname = "{recipientNickname}",
            )

        val notificationCriteria =
            NotificationCriteria.Create(
                recipientUserIds = recipientUserIds,
                subject = subjectTemplate,
                content = contentTemplate,
                email = NotificationCriteria.EmailTemplate(emailSubjectTemplate, emailContentTemplate),
            )

        // 요청
        notificationApplicationService.create(notificationCriteria)
    }
}
