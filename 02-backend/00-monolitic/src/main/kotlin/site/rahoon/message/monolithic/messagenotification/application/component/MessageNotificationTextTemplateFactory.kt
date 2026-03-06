package site.rahoon.message.monolithic.messagenotification.application.component

import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 메시지 기반 Notification의 이메일 제목/본문 생성 컴포넌트
 */
@Component
class MessageNotificationTextTemplateFactory {
    fun buildSubject(chatRoomName: String): String = "'$chatRoomName'에서 새로운 메시지 도착"

    fun buildContent(
        senderNickname: String,
        messageContent: String,
    ): String = "$senderNickname: $messageContent"

    fun buildEmailSubject(chatRoomName: String): String = "[R-Message] '$chatRoomName'에서 새 메시지가 도착했습니다"

    fun buildEmailContent(
        chatRoomName: String,
        senderNickname: String,
        messageContent: String,
        messageCreatedAt: LocalDateTime,
        recipientNickname: String,
    ): String =
        """
        안녕하세요, $recipientNickname 님.

        새로운 메시지가 도착했습니다.

        - 채팅방: $chatRoomName
        - 발신자: $senderNickname
        - 내용: $messageContent
        - 생성 시각: $messageCreatedAt

        R-Message에서 확인해 주세요.
        """.trimIndent()
}
