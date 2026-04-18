package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetMessageListQuery
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetMessageQueryService
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
import site.rahoon.message.monolithic.core.conversation.domain.MessageContent
import site.rahoon.message.monolithic.core.conversation.domain.Origin
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import java.time.LocalDateTime

class WidgetMessageQueryServiceUT {
    private lateinit var widgetAccessPolicy: WidgetAccessPolicy
    private lateinit var visitorSessionPolicy: VisitorSessionPolicy
    private lateinit var conversationVisitorAccessPolicy: ConversationVisitorAccessPolicy
    private lateinit var conversationMessageRepository: ConversationMessageRepository
    private lateinit var queryService: WidgetMessageQueryService

    @BeforeEach
    fun setUp() {
        widgetAccessPolicy = mockk()
        visitorSessionPolicy = mockk()
        conversationVisitorAccessPolicy = mockk()
        conversationMessageRepository = mockk()
        queryService =
            WidgetMessageQueryService(
                widgetAccessPolicy = widgetAccessPolicy,
                visitorSessionPolicy = visitorSessionPolicy,
                conversationVisitorAccessPolicy = conversationVisitorAccessPolicy,
                conversationMessageRepository = conversationMessageRepository,
            )
    }

    @Test
    fun `listMessages returns cursor page with hasMore`() {
        // Arrange: Prepare readable conversation and one extra message. / 준비: 조회 가능한 conversation과 추가 message 1개를 준비한다.
        val session = visitorSession()
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } returns session
        every { conversationVisitorAccessPolicy.requireReadableConversation("conversation-1", session) } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")
        every { conversationMessageRepository.findVisibleAfterSequence("conversation-1", 0, 3) } returns
            listOf(message(1), message(2), message(3))

        // Act: List widget messages with limit two. / 실행: limit 2로 widget message를 조회한다.
        val result =
            queryService.listMessages(
                WidgetMessageListQuery(
                    publicKey = "wpk_public",
                    origin = "https://acme.com",
                    visitorSessionToken = "wvs_raw",
                    conversationId = "conversation-1",
                    afterSequence = 0,
                    limit = 2,
                ),
            )

        // Assert: Verify cursor result excludes the extra message. / 검증: cursor 결과가 추가 message를 제외하는지 검증한다.
        result.messages.map { it.sequence } shouldBe listOf(1L, 2L)
        result.nextAfterSequence shouldBe 2
        result.hasMore shouldBe true
    }

    private fun message(sequence: Long): ConversationMessage =
        ConversationMessage.visitorText(
            conversationId = "conversation-1",
            channelId = "channel-1",
            visitorId = "visitor-1",
            sequence = sequence,
            clientMessageId = "client-$sequence",
            content = MessageContent.text("message $sequence"),
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
