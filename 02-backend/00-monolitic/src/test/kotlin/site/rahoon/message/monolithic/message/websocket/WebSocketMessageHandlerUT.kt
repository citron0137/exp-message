package site.rahoon.message.monolithic.message.websocket

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate
import site.rahoon.message.monolithic.message.application.MessageEvent
import java.time.LocalDateTime
import java.util.UUID

/**
 * WebSocketMessageHandler 단위 테스트
 */
class WebSocketMessageHandlerUT {
    @Test
    fun `sendMessage 호출 시 올바른 destination으로 이벤트 전송`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()
        val event =
            MessageEvent.Created(
                id = UUID.randomUUID().toString(),
                chatRoomId = chatRoomId,
                userId = UUID.randomUUID().toString(),
                content = "웹소켓 테스트",
                createdAt = LocalDateTime.now(),
            )

        val destSlot = slot<String>()
        val payloadSlot = slot<Any>()
        val simpMessagingTemplate = mockk<SimpMessagingTemplate>(relaxed = true)
        every {
            simpMessagingTemplate.convertAndSend(capture(destSlot), capture(payloadSlot))
        } returns Unit

        val handler = WebSocketMessageHandler(simpMessagingTemplate)

        // when
        handler.onCreated(event)

        // then
        destSlot.captured shouldBe "/topic/chat-rooms/$chatRoomId/messages"
        val payload = payloadSlot.captured as MessageEvent.Created
        payload.id shouldBe event.id
        payload.chatRoomId shouldBe chatRoomId
        payload.userId shouldBe event.userId
        payload.content shouldBe event.content

        verify(exactly = 1) {
            simpMessagingTemplate.convertAndSend(any<String>(), any<Any>())
        }
    }
}
