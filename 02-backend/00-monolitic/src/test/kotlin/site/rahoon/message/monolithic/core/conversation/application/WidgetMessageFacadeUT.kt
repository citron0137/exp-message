package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.facade.SendWidgetVisitorMessageCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetMessageFacade
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ConversationVisitorAccessPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccess
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessage
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageSenderType
import site.rahoon.message.monolithic.core.conversation.domain.MessageContent
import site.rahoon.message.monolithic.core.conversation.domain.Origin
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import java.time.LocalDateTime

class WidgetMessageFacadeUT {
    private lateinit var widgetAccessPolicy: WidgetAccessPolicy
    private lateinit var visitorSessionPolicy: VisitorSessionPolicy
    private lateinit var conversationVisitorAccessPolicy: ConversationVisitorAccessPolicy
    private lateinit var channelConversationRepository: ChannelConversationRepository
    private lateinit var conversationMessageRepository: ConversationMessageRepository
    private lateinit var visitorSessionRepository: VisitorSessionRepository
    private lateinit var facade: WidgetMessageFacade

    @BeforeEach
    fun setUp() {
        widgetAccessPolicy = mockk()
        visitorSessionPolicy = mockk()
        conversationVisitorAccessPolicy = mockk()
        channelConversationRepository = mockk()
        conversationMessageRepository = mockk()
        visitorSessionRepository = mockk()
        facade =
            WidgetMessageFacade(
                widgetAccessPolicy = widgetAccessPolicy,
                visitorSessionPolicy = visitorSessionPolicy,
                conversationVisitorAccessPolicy = conversationVisitorAccessPolicy,
                channelConversationRepository = channelConversationRepository,
                conversationMessageRepository = conversationMessageRepository,
                visitorSessionRepository = visitorSessionRepository,
            )
    }

    @Test
    fun `sendVisitorMessage stores message with next conversation sequence`() {
        // Arrange: Prepare widget access, visitor session, and appendable conversation. / 준비: widget access, visitor session, append 가능한 conversation을 준비한다.
        val session = visitorSession()
        val conversation = ChannelConversation.start("channel-1", "visitor-1")
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } returns session
        every {
            conversationMessageRepository.findByIdempotencyKey(
                "conversation-1",
                ConversationMessageSenderType.VISITOR,
                "visitor-1",
                "client-1",
            )
        } returns null
        every { conversationVisitorAccessPolicy.requireAppendableConversation("conversation-1", session) } returns
            conversation.copy(id = "conversation-1")
        every { channelConversationRepository.save(any()) } answers { firstArg() }
        every { conversationMessageRepository.save(any()) } answers { firstArg() }
        every { visitorSessionRepository.save(any()) } answers { firstArg() }

        // Act: Send a visitor message. / 실행: visitor message를 전송한다.
        val result = facade.sendVisitorMessage(command())

        // Assert: Verify message sequence and persistence calls. / 검증: message sequence와 저장 호출을 검증한다.
        result.sequence shouldBe 1
        result.content shouldBe "hello"
        verify { channelConversationRepository.save(match { it.lastMessageSequence == 1L && it.lastMessageAt != null }) }
        verify { conversationMessageRepository.save(match { it.sequence == 1L && it.senderId == "visitor-1" }) }
    }

    @Test
    fun `sendVisitorMessage returns existing message for duplicate client message id`() {
        // Arrange: Prepare an existing message with the same idempotency key. / 준비: 같은 idempotency key를 가진 기존 message를 준비한다.
        val session = visitorSession()
        val existingMessage =
            ConversationMessage.visitorText(
                conversationId = "conversation-1",
                channelId = "channel-1",
                visitorId = "visitor-1",
                sequence = 7,
                clientMessageId = "client-1",
                content = MessageContent.text("hello"),
            )
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } returns session
        every { conversationVisitorAccessPolicy.requireAppendableConversation("conversation-1", session) } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")
        every {
            conversationMessageRepository.findByIdempotencyKey(
                "conversation-1",
                ConversationMessageSenderType.VISITOR,
                "visitor-1",
                "client-1",
            )
        } returns existingMessage
        every { visitorSessionRepository.save(any()) } answers { firstArg() }

        // Act: Send the duplicate visitor message. / 실행: 중복 visitor message를 전송한다.
        val result = facade.sendVisitorMessage(command())

        // Assert: Verify existing message is returned without sequence mutation. / 검증: sequence 변경 없이 기존 message가 반환되는지 검증한다.
        result.id shouldBe existingMessage.id
        result.sequence shouldBe 7
        verify(exactly = 0) { channelConversationRepository.save(any()) }
        verify(exactly = 0) { conversationMessageRepository.save(any()) }
    }

    private fun command(): SendWidgetVisitorMessageCommand =
        SendWidgetVisitorMessageCommand(
            publicKey = "wpk_public",
            origin = "https://acme.com",
            visitorSessionToken = "wvs_raw",
            conversationId = "conversation-1",
            clientMessageId = "client-1",
            content = "hello",
        )

    private fun visitorSession(): VisitorSession =
        VisitorSession.create(
            visitorId = "visitor-1",
            channelId = "channel-1",
            tokenHash = "hashed-token",
            expiresAt = LocalDateTime.now().plusDays(1),
        )

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
}
