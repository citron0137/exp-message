package site.rahoon.message.monolithic.message.application

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import site.rahoon.message.monolithic.chatroommember.application.ChatRoomMemberApplicationService

/**
 * 메시지 이벤트 디스패처
 *
 * MessageEvent.Created를 받아서 채팅방 멤버들에게 MessageCommandEvent.Send를 전달
 */
@Component
class MessageCommandEventDispatcher(
    private val messageCommandEventRelayPort: MessageCommandEventRelayPort,
    private val chatRoomMemberApplicationService: ChatRoomMemberApplicationService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun sendToUsers(event: MessageEvent.Created) {
        val sendCommandEvent = MessageCommandEvent.Send.from(event)
        val userIds = chatRoomMemberApplicationService.getByChatRoomId(event.chatRoomId).map { it.userId }
        messageCommandEventRelayPort.sendToUsers(userIds, sendCommandEvent)
    }
}
