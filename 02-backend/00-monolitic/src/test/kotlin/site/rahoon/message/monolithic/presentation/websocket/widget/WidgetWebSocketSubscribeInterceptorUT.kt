package site.rahoon.message.monolithic.presentation.websocket.widget

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionAttributeNames
import site.rahoon.message.monolithic.core.conversation.application.service.ConversationVisitorAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import java.time.LocalDateTime

class WidgetWebSocketSubscribeInterceptorUT {
    private lateinit var conversationVisitorAccessPolicy: ConversationVisitorAccessPolicy
    private lateinit var interceptor: WidgetWebSocketSubscribeInterceptor
    private val channel = mockk<MessageChannel>(relaxed = true)

    @BeforeEach
    fun setUp() {
        conversationVisitorAccessPolicy = mockk()
        interceptor = WidgetWebSocketSubscribeInterceptor(conversationVisitorAccessPolicy)
    }

    @Test
    fun `preSend verifies widget conversation topic subscription`() {
        // Arrange: Prepare a widget session and conversation topic. / 준비: widget session과 conversation topic을 준비한다.
        val message = subscribeMessage("/topic/widget/conversations/conversation-1/messages")
        every {
            conversationVisitorAccessPolicy.requireReadableConversation(
                conversationId = "conversation-1",
                channelId = "channel-1",
                visitorId = "visitor-1",
            )
        } returns ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")

        // Act: Process STOMP SUBSCRIBE. / 실행: STOMP SUBSCRIBE를 처리한다.
        val result = interceptor.preSend(message, channel)

        // Assert: Verify subscription is allowed after policy check. / 검증: policy 검증 후 subscribe가 허용되는지 검증한다.
        result shouldBe message
        verify {
            conversationVisitorAccessPolicy.requireReadableConversation(
                conversationId = "conversation-1",
                channelId = "channel-1",
                visitorId = "visitor-1",
            )
        }
    }

    private fun subscribeMessage(destination: String): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = destination
        accessor.setHeader(
            SimpMessageHeaderAccessor.SESSION_ATTRIBUTES,
            mutableMapOf<String, Any>(
                WebSocketSessionAttributeNames.WIDGET_SESSION to
                    WidgetWebSocketSession(
                        publicKey = "wpk_public",
                        origin = "https://acme.com",
                        visitorSessionToken = "wvs_raw",
                        channelId = "channel-1",
                        visitorId = "visitor-1",
                        visitorSessionId = "session-1",
                        expiresAt = LocalDateTime.now().plusDays(1),
                    ),
            ),
        )
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }
}
