package site.rahoon.message.monolithic.presentation.websocket.widget

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionAttributeNames
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccess
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.domain.Origin
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import java.time.LocalDateTime

class WidgetWebSocketConnectInterceptorUT {
    private lateinit var widgetAccessPolicy: WidgetAccessPolicy
    private lateinit var visitorSessionPolicy: VisitorSessionPolicy
    private lateinit var interceptor: WidgetWebSocketConnectInterceptor
    private val channel = mockk<MessageChannel>(relaxed = true)

    @BeforeEach
    fun setUp() {
        widgetAccessPolicy = mockk()
        visitorSessionPolicy = mockk()
        interceptor =
            WidgetWebSocketConnectInterceptor(
                widgetAccessPolicy = widgetAccessPolicy,
                visitorSessionPolicy = visitorSessionPolicy,
            )
    }

    @Test
    fun `preSend stores widget session on connect with handshake credentials`() {
        // Arrange: Prepare widget credentials saved during handshake. / 준비: handshake 중 저장된 widget credential을 준비한다.
        val sessionAttributes =
            mutableMapOf<String, Any>(
                WebSocketSessionAttributeNames.WIDGET_PUBLIC_KEY to "wpk_public",
                WebSocketSessionAttributeNames.WIDGET_VISITOR_SESSION_TOKEN to "wvs_raw",
                WebSocketSessionAttributeNames.WIDGET_ORIGIN to "https://acme.com",
            )
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } returns visitorSession()
        val message = connectMessage(sessionAttributes)

        // Act: Process STOMP CONNECT. / 실행: STOMP CONNECT를 처리한다.
        val result = interceptor.preSend(message, channel)

        // Assert: Verify widget session is stored for later frames. / 검증: 이후 frame에서 사용할 widget session이 저장되는지 검증한다.
        result shouldBe message
        val widgetSession = sessionAttributes[WebSocketSessionAttributeNames.WIDGET_SESSION] as WidgetWebSocketSession
        widgetSession.publicKey shouldBe "wpk_public"
        widgetSession.channelId shouldBe "channel-1"
        widgetSession.visitorId shouldBe "visitor-1"
    }

    private fun connectMessage(sessionAttributes: MutableMap<String, Any>): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        accessor.setHeader(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, sessionAttributes)
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    private fun widgetAccess(): WidgetAccess =
        WidgetAccess(
            channel =
                Channel(
                    id = "channel-1",
                    name = "Acme",
                    status = ChannelStatus.ACTIVE,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            integration =
                ChannelIntegration.createWidget(
                    channelId = "channel-1",
                    publicKey = "wpk_public",
                    secretHash = "secret-hash",
                    allowedOrigins = AllowedOrigins.of(listOf("*")),
                ),
            origin = Origin("https://acme.com"),
        )

    private fun visitorSession(): VisitorSession =
        VisitorSession.create(
            visitorId = "visitor-1",
            channelId = "channel-1",
            tokenHash = "hashed-token",
            expiresAt = LocalDateTime.now().plusDays(1),
        )
}
