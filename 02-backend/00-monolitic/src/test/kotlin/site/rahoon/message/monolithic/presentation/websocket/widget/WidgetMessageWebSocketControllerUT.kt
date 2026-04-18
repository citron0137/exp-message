package site.rahoon.message.monolithic.presentation.websocket.widget

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionAttributeNames
import site.rahoon.message.monolithic.core.conversation.application.facade.ConversationMessageResult
import site.rahoon.message.monolithic.core.conversation.application.facade.SendWidgetVisitorMessageCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetMessageFacade
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageSenderType
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageStatus
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageType
import java.time.LocalDateTime

class WidgetMessageWebSocketControllerUT {
    private lateinit var widgetMessageFacade: WidgetMessageFacade
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var controller: WidgetMessageWebSocketController

    @BeforeEach
    fun setUp() {
        widgetMessageFacade = mockk()
        messagingTemplate = mockk(relaxed = true)
        controller =
            WidgetMessageWebSocketController(
                widgetMessageFacade = widgetMessageFacade,
                messagingTemplate = messagingTemplate,
            )
    }

    @Test
    fun `sendMessage stores message and broadcasts saved result`() {
        // Arrange: Prepare a widget WebSocket session and saved message result. / 준비: widget WebSocket session과 저장된 message result를 준비한다.
        every { widgetMessageFacade.sendVisitorMessage(any()) } returns messageResult()

        // Act: Send a widget WebSocket message. / 실행: widget WebSocket message를 전송한다.
        val response =
            controller.sendMessage(
                accessor = accessor(),
                conversationId = "conversation-1",
                request =
                    WidgetMessageWebSocketRequest.SendMessage(
                        clientMessageId = "client-1",
                        content = "hello",
                    ),
            )

        // Assert: Verify facade command and broadcast topic. / 검증: facade command와 broadcast topic을 검증한다.
        response.sequence shouldBe 1
        response.content shouldBe "hello"
        verify {
            widgetMessageFacade.sendVisitorMessage(
                SendWidgetVisitorMessageCommand(
                    publicKey = "wpk_public",
                    origin = "https://acme.com",
                    visitorSessionToken = "wvs_raw",
                    conversationId = "conversation-1",
                    clientMessageId = "client-1",
                    content = "hello",
                ),
            )
        }
        verify {
            messagingTemplate.convertAndSend(
                "/topic/widget/conversations/conversation-1/messages",
                match<WidgetMessageWebSocketResponse.Message> { it.id == "message-1" },
            )
        }
    }

    private fun accessor(): StompHeaderAccessor {
        val accessor = StompHeaderAccessor.create(StompCommand.SEND)
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
        return accessor
    }

    private fun messageResult(): ConversationMessageResult =
        ConversationMessageResult(
            id = "message-1",
            conversationId = "conversation-1",
            channelId = "channel-1",
            sequence = 1,
            senderType = ConversationMessageSenderType.VISITOR,
            senderId = "visitor-1",
            clientMessageId = "client-1",
            type = ConversationMessageType.TEXT,
            content = "hello",
            status = ConversationMessageStatus.VISIBLE,
            createdAt = LocalDateTime.now(),
        )
}
