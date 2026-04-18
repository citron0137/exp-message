package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationDetailRow
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationListRow
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationReader
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationDetailQuery
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationListQuery
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationMessagesQuery
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationQueryService
import site.rahoon.message.monolithic.core.conversation.application.query.AdminInboxCursor
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessage
import site.rahoon.message.monolithic.core.conversation.domain.MessageContent
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminConversationQueryServiceUT {
    private lateinit var channelAccessPolicy: ChannelAccessPolicy
    private lateinit var adminConversationReader: AdminConversationReader
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var conversationMessageRepository: ConversationMessageRepository
    private lateinit var queryService: AdminConversationQueryService

    @BeforeEach
    fun setUp() {
        channelAccessPolicy = mockk(relaxed = true)
        adminConversationReader = mockk()
        channelMembershipRepository = mockk()
        conversationMessageRepository = mockk()
        queryService =
            AdminConversationQueryService(
                channelAccessPolicy = channelAccessPolicy,
                adminConversationReader = adminConversationReader,
                channelMembershipRepository = channelMembershipRepository,
                conversationMessageRepository = conversationMessageRepository,
            )
    }

    @Test
    fun `listConversations returns limited admin inbox page with hasMore`() {
        // Arrange: Prepare one extra conversation row for pagination. / 준비: pagination 확인을 위해 추가 conversation row를 준비한다.
        val actor = principal()
        every {
            adminConversationReader.listConversations(
                channelId = "channel-1",
                status = null,
                assigneeMembershipId = null,
                unassignedOnly = false,
                cursorActivityAt = null,
                cursorId = null,
                limit = 3,
            )
        } returns
            listOf(
                listRow("conversation-3", 3),
                listRow("conversation-2", 2),
                listRow("conversation-1", 1),
            )

        // Act: List conversations with limit two. / 실행: limit 2로 conversations를 조회한다.
        val result =
            queryService.listConversations(
                AdminConversationListQuery(
                    actor = actor,
                    channelId = "channel-1",
                    status = null,
                    assigneeMembershipId = null,
                    limit = 2,
                ),
            )

        // Assert: Verify read access and extra row pagination. / 검증: read access와 extra row pagination을 검증한다.
        result.items.map { it.id } shouldBe listOf("conversation-3", "conversation-2")
        result.nextCursor shouldBe AdminInboxCursor.from(result.items.last()).encode()
        result.hasMore shouldBe true
        verify { channelAccessPolicy.requireChannelRead(actor, "channel-1") }
    }

    @Test
    fun `listConversations applies cursor and assignee filter`() {
        // Arrange: Prepare a cursor and same-channel assignee membership. / 준비: cursor와 같은 channel의 assignee membership을 준비한다.
        val actor = principal()
        val cursor = AdminInboxCursor(activityAt = LocalDateTime.parse("2026-04-18T10:00:00"), id = "conversation-10").encode()
        every { channelMembershipRepository.findById("membership-1") } returns membership("membership-1")
        every {
            adminConversationReader.listConversations(
                channelId = "channel-1",
                status = ChannelConversationStatus.OPEN.name,
                assigneeMembershipId = "membership-1",
                unassignedOnly = false,
                cursorActivityAt = LocalDateTime.parse("2026-04-18T10:00:00"),
                cursorId = "conversation-10",
                limit = 3,
            )
        } returns listOf(listRow("conversation-2", 2), listRow("conversation-1", 1))

        // Act: List conversations with cursor and assignee filter. / 실행: cursor와 assignee filter로 conversations를 조회한다.
        val result =
            queryService.listConversations(
                AdminConversationListQuery(
                    actor = actor,
                    channelId = "channel-1",
                    status = ChannelConversationStatus.OPEN,
                    assigneeMembershipId = "membership-1",
                    cursor = cursor,
                    limit = 2,
                ),
            )

        // Assert: Verify cursor and filter produce a final page. / 검증: cursor와 filter가 마지막 page를 만드는지 검증한다.
        result.items.map { it.id } shouldBe listOf("conversation-2", "conversation-1")
        result.nextCursor shouldBe null
        result.hasMore shouldBe false
    }

    @Test
    fun `listConversations rejects conflicting assignee filters`() {
        // Arrange: Prepare conflicting assignee filters. / 준비: 충돌하는 assignee filter를 준비한다.
        val actor = principal()

        // Act: List conversations with both assignee and unassigned filters. / 실행: assignee와 unassigned filter를 동시에 사용한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.listConversations(
                    AdminConversationListQuery(
                        actor = actor,
                        channelId = "channel-1",
                        status = null,
                        assigneeMembershipId = "membership-1",
                        unassigned = true,
                        limit = 2,
                    ),
                )
            }

        // Assert: Verify conflicting filters are rejected. / 검증: 충돌하는 filter가 거부되는지 검증한다.
        exception.error shouldBe ConversationError.INVALID_ADMIN_INBOX_FILTER
    }

    @Test
    fun `getConversation throws when conversation is missing`() {
        // Arrange: Prepare a missing conversation detail. / 준비: 존재하지 않는 conversation detail을 준비한다.
        val actor = principal()
        every { adminConversationReader.findConversationDetail("channel-1", "missing-conversation") } returns null

        // Act: Get the missing conversation. / 실행: 존재하지 않는 conversation을 조회한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.getConversation(
                    AdminConversationDetailQuery(
                        actor = actor,
                        channelId = "channel-1",
                        conversationId = "missing-conversation",
                    ),
                )
            }

        // Assert: Verify a not found error is returned. / 검증: not found error가 반환되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_NOT_FOUND
        exception.details["conversationId"] shouldBe "missing-conversation"
    }

    @Test
    fun `getConversation returns admin inbox detail`() {
        // Arrange: Prepare a conversation detail row. / 준비: conversation detail row를 준비한다.
        val actor = principal()
        every { adminConversationReader.findConversationDetail("channel-1", "conversation-1") } returns
            detailRow("conversation-1")

        // Act: Get the conversation detail. / 실행: conversation detail을 조회한다.
        val result =
            queryService.getConversation(
                AdminConversationDetailQuery(
                    actor = actor,
                    channelId = "channel-1",
                    conversationId = "conversation-1",
                ),
            )

        // Assert: Verify mapped conversation and visitor fields. / 검증: 매핑된 conversation과 visitor 필드를 검증한다.
        result.id shouldBe "conversation-1"
        result.visitor.id shouldBe "visitor-1"
        result.status shouldBe ChannelConversationStatus.OPEN
    }

    @Test
    fun `listMessages returns limited message page with hasMore`() {
        // Arrange: Prepare an existing conversation and one extra message. / 준비: 존재하는 conversation과 추가 message 1개를 준비한다.
        val actor = principal()
        every { adminConversationReader.existsConversation("channel-1", "conversation-1") } returns true
        every { conversationMessageRepository.findVisibleAfterSequence("conversation-1", 0, 3) } returns
            listOf(message(1), message(2), message(3))

        // Act: List messages with limit two. / 실행: limit 2로 messages를 조회한다.
        val result =
            queryService.listMessages(
                AdminConversationMessagesQuery(
                    actor = actor,
                    channelId = "channel-1",
                    conversationId = "conversation-1",
                    afterSequence = 0,
                    limit = 2,
                ),
            )

        // Assert: Verify the extra message is used only for hasMore. / 검증: 추가 message가 hasMore 판단에만 사용되는지 검증한다.
        result.messages.map { it.sequence } shouldBe listOf(1L, 2L)
        result.nextAfterSequence shouldBe 2
        result.hasMore shouldBe true
    }

    @Test
    fun `listMessages throws when conversation does not belong to channel`() {
        // Arrange: Prepare a missing channel conversation relation. / 준비: channel에 속하지 않는 conversation 관계를 준비한다.
        val actor = principal()
        every { adminConversationReader.existsConversation("channel-1", "conversation-1") } returns false

        // Act: List messages for the missing relation. / 실행: 존재하지 않는 관계의 messages를 조회한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.listMessages(
                    AdminConversationMessagesQuery(
                        actor = actor,
                        channelId = "channel-1",
                        conversationId = "conversation-1",
                    ),
                )
            }

        // Assert: Verify messages are not queried for missing conversations. / 검증: 존재하지 않는 conversation은 messages를 조회하지 않는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_NOT_FOUND
        verify(exactly = 0) { conversationMessageRepository.findVisibleAfterSequence(any(), any(), any()) }
    }

    private fun principal(): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = PrincipalGlobalRole.CHANNEL_USER,
            expiresAt = LocalDateTime.now().plusHours(1),
        )

    private fun listRow(
        conversationId: String,
        sequence: Long,
    ): AdminConversationListRow =
        AdminConversationListRow(
            id = conversationId,
            channelId = "channel-1",
            visitorId = "visitor-1",
            status = ChannelConversationStatus.OPEN.name,
            activityAt = LocalDateTime.parse("2026-04-18T10:00:00").plusMinutes(sequence),
            lastMessageSequence = sequence,
            lastMessageAt = LocalDateTime.parse("2026-04-18T10:00:00").plusMinutes(sequence),
            createdAt = LocalDateTime.now().minusMinutes(2),
            updatedAt = LocalDateTime.now(),
            closedAt = null,
            visitorExternalId = "external-1",
            visitorDisplayName = "Visitor",
            visitorEmail = "visitor@example.com",
            assigneeMembershipId = "membership-1",
            assigneeUserId = "agent-1",
            assigneeRole = "AGENT",
            assigneeAgentStatus = "ONLINE",
            lastMessageId = "message-$sequence",
            lastMessageSequenceValue = sequence,
            lastMessageSenderType = "VISITOR",
            lastMessageContent = "message $sequence",
            lastMessageCreatedAt = LocalDateTime.now(),
        )

    private fun membership(
        id: String,
        channelId: String = "channel-1",
    ): ChannelMembership =
        ChannelMembership(
            id = id,
            channelId = channelId,
            userId = "agent-1",
            role = ChannelMembershipRole.AGENT,
            agentStatus = AgentStatus.ONLINE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun detailRow(conversationId: String): AdminConversationDetailRow =
        AdminConversationDetailRow(
            id = conversationId,
            channelId = "channel-1",
            visitorId = "visitor-1",
            status = ChannelConversationStatus.OPEN.name,
            lastMessageSequence = 1,
            lastMessageAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now().minusMinutes(2),
            updatedAt = LocalDateTime.now(),
            closedAt = null,
            visitorExternalId = "external-1",
            visitorDisplayName = "Visitor",
            visitorEmail = "visitor@example.com",
            assigneeMembershipId = "membership-1",
            assigneeUserId = "agent-1",
            assigneeRole = "AGENT",
            assigneeAgentStatus = "ONLINE",
        )

    private fun message(sequence: Long): ConversationMessage =
        ConversationMessage.visitorText(
            conversationId = "conversation-1",
            channelId = "channel-1",
            visitorId = "visitor-1",
            sequence = sequence,
            clientMessageId = "client-$sequence",
            content = MessageContent.text("message $sequence"),
        )
}
