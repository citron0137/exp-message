package site.rahoon.message.monolithic.message.websocket

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.controller.MessageResponse
import site.rahoon.message.monolithic.message.domain.Message
import java.time.LocalDateTime
import java.util.UUID

/**
 * MessageCreatedEventListener 단위 테스트
 */
class MessageCreatedEventListenerUT {
    @Test
    fun `Created 이벤트 수신 시 convertAndSend로 해당 topic에 MessageResponse Detail 전송`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()
        val message =
            Message(
                id = UUID.randomUUID().toString(),
                chatRoomId = chatRoomId,
                userId = UUID.randomUUID().toString(),
                content = "웹소켓 테스트",
                createdAt = LocalDateTime.now(),
            )
        val event = MessageEvent.Created(message)

        val destSlot = slot<String>()
        val payloadSlot = slot<Any>()
        val simpMessagingTemplate = mockk<org.springframework.messaging.simp.SimpMessagingTemplate>(relaxed = true)
        every {
            simpMessagingTemplate.convertAndSend(capture(destSlot), capture(payloadSlot))
        } returns Unit

        val listener = MessageCreatedEventListener(simpMessagingTemplate)

        // when
        listener.onMessageCreated(event)

        // then
        destSlot.captured shouldBe "/topic/chat-rooms/$chatRoomId/messages"
        val payload = payloadSlot.captured as MessageResponse.Detail
        payload.id shouldBe message.id
        payload.chatRoomId shouldBe chatRoomId
        payload.userId shouldBe message.userId
        payload.content shouldBe message.content

        verify(exactly = 1) {
            simpMessagingTemplate.convertAndSend(any<String>(), any<Any>())
        }
    }
}
