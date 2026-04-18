package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminConversationFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeConversationAssigneeCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeConversationStatusCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.SendAdminConversationReplyCommand
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageSenderType
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminConversationFacadeUT {
    private lateinit var channelConversationRepository: ChannelConversationRepository
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var conversationMessageRepository: ConversationMessageRepository
    private lateinit var facade: AdminConversationFacade

    @BeforeEach
    fun setUp() {
        channelConversationRepository = mockk()
        channelMembershipRepository = mockk()
        conversationMessageRepository = mockk()
        facade =
            AdminConversationFacade(
                channelAccessPolicy = ChannelAccessPolicy(channelMembershipRepository),
                channelConversationRepository = channelConversationRepository,
                channelMembershipRepository = channelMembershipRepository,
                conversationMessageRepository = conversationMessageRepository,
            )
    }

    @Test
    fun `changeStatus opens pending conversation for channel admin`() {
        // Arrange: Prepare a channel admin and pending conversation. / 준비: channel admin과 pending conversation을 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("membership-admin", ChannelMembershipRole.CHANNEL_ADMIN)
        every { channelConversationRepository.findById("conversation-1") } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")
        every { channelConversationRepository.save(any()) } answers { firstArg() }

        // Act: Change the conversation status to open. / 실행: conversation status를 open으로 변경한다.
        val result =
            facade.changeStatus(
                ChangeConversationStatusCommand(
                    actor = actor,
                    channelId = "channel-1",
                    conversationId = "conversation-1",
                    status = ChannelConversationStatus.OPEN,
                ),
            )

        // Assert: Verify status change is saved. / 검증: status 변경이 저장되는지 검증한다.
        result.status shouldBe ChannelConversationStatus.OPEN
        verify { channelConversationRepository.save(match { it.status == ChannelConversationStatus.OPEN }) }
    }

    @Test
    fun `changeStatus rejects closed conversation transition`() {
        // Arrange: Prepare a platform admin and closed conversation. / 준비: platform admin과 closed conversation을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelConversationRepository.findById("conversation-1") } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1").markClosed()

        // Act: Try to reopen the closed conversation. / 실행: closed conversation 재개를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.changeStatus(
                    ChangeConversationStatusCommand(
                        actor = actor,
                        channelId = "channel-1",
                        conversationId = "conversation-1",
                        status = ChannelConversationStatus.OPEN,
                    ),
                )
            }

        // Assert: Verify closed conversations cannot change status. / 검증: closed conversation은 status 변경이 불가한지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_STATUS_CHANGE_NOT_ALLOWED
    }

    @Test
    fun `changeAssignee assigns conversation to channel membership`() {
        // Arrange: Prepare an assignable agent membership. / 준비: 할당 가능한 agent membership을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelConversationRepository.findById("conversation-1") } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")
        every { channelMembershipRepository.findById("membership-agent") } returns
            membership("membership-agent", ChannelMembershipRole.AGENT)
        every { channelConversationRepository.save(any()) } answers { firstArg() }

        // Act: Assign the conversation. / 실행: conversation을 할당한다.
        val result =
            facade.changeAssignee(
                ChangeConversationAssigneeCommand(
                    actor = actor,
                    channelId = "channel-1",
                    conversationId = "conversation-1",
                    assigneeMembershipId = "membership-agent",
                ),
            )

        // Assert: Verify assignee membership is stored. / 검증: assignee membership이 저장되는지 검증한다.
        result.assigneeMembershipId shouldBe "membership-agent"
        verify { channelConversationRepository.save(match { it.assigneeMembershipId == "membership-agent" }) }
    }

    @Test
    fun `changeAssignee rejects membership from another channel`() {
        // Arrange: Prepare a membership owned by another channel. / 준비: 다른 channel 소유 membership을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelConversationRepository.findById("conversation-1") } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")
        every { channelMembershipRepository.findById("membership-other") } returns
            membership("membership-other", ChannelMembershipRole.AGENT, channelId = "channel-2")

        // Act: Try to assign the other channel membership. / 실행: 다른 channel membership 할당을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.changeAssignee(
                    ChangeConversationAssigneeCommand(
                        actor = actor,
                        channelId = "channel-1",
                        conversationId = "conversation-1",
                        assigneeMembershipId = "membership-other",
                    ),
                )
            }

        // Assert: Verify cross-channel assignment is rejected. / 검증: cross-channel assignment가 거부되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_MEMBERSHIP_NOT_ASSIGNABLE
    }

    @Test
    fun `changeAssignee rejects disabled membership`() {
        // Arrange: Prepare a disabled membership in the same channel. / 준비: 같은 channel의 disabled membership을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelConversationRepository.findById("conversation-1") } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")
        every { channelMembershipRepository.findById("membership-disabled") } returns
            membership(
                id = "membership-disabled",
                role = ChannelMembershipRole.AGENT,
                status = ChannelMembershipStatus.DISABLED,
            )

        // Act: Try to assign the disabled membership. / 실행: disabled membership 할당을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.changeAssignee(
                    ChangeConversationAssigneeCommand(
                        actor = actor,
                        channelId = "channel-1",
                        conversationId = "conversation-1",
                        assigneeMembershipId = "membership-disabled",
                    ),
                )
            }

        // Assert: Verify disabled memberships are not assignable. / 검증: disabled membership은 할당할 수 없는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_MEMBERSHIP_NOT_ASSIGNABLE
    }

    @Test
    fun `sendReply stores agent message and opens conversation`() {
        // Arrange: Prepare an active channel admin membership and pending conversation.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("membership-admin", ChannelMembershipRole.CHANNEL_ADMIN, userId = "user-1")
        every { channelConversationRepository.findById("conversation-1") } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1")
        every {
            conversationMessageRepository.findByIdempotencyKey(
                "conversation-1",
                ConversationMessageSenderType.AGENT,
                "membership-admin",
                "client-1",
            )
        } returns null
        every { conversationMessageRepository.save(any()) } answers { firstArg() }
        every { channelConversationRepository.save(any()) } answers { firstArg() }

        // Act: Send an admin reply. / 실행: admin reply를 전송한다.
        val result = facade.sendReply(replyCommand(actor))

        // Assert: Verify the reply is stored as an agent message. / 검증: reply가 agent message로 저장되는지 검증한다.
        result.senderType shouldBe ConversationMessageSenderType.AGENT
        result.senderId shouldBe "membership-admin"
        result.sequence shouldBe 1
        verify {
            channelConversationRepository.save(
                match {
                    it.status == ChannelConversationStatus.OPEN &&
                        it.lastMessageSequence == 1L &&
                        it.lastMessageAt != null
                },
            )
        }
    }

    @Test
    fun `sendReply rejects platform admin without channel membership`() {
        // Arrange: Prepare a platform admin without explicit channel membership. / 준비: 명시적인 channel membership이 없는 platform admin을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns null

        // Act: Try to send a reply without channel membership. / 실행: channel membership 없이 reply 전송을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.sendReply(replyCommand(actor))
            }

        // Assert: Verify platform authority alone is not enough to speak as an operator. / 검증: platform 권한만으로 operator 발화가 불가한지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_REPLY_NOT_ALLOWED
    }

    @Test
    fun `sendReply rejects disabled membership`() {
        // Arrange: Prepare a disabled agent membership. / 준비: disabled agent membership을 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership(
                id = "membership-agent",
                role = ChannelMembershipRole.AGENT,
                userId = "user-1",
                status = ChannelMembershipStatus.DISABLED,
            )

        // Act: Try to send a reply with the disabled membership. / 실행: disabled membership으로 reply 전송을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.sendReply(replyCommand(actor))
            }

        // Assert: Verify disabled memberships cannot send replies. / 검증: disabled membership은 reply를 보낼 수 없는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_REPLY_NOT_ALLOWED
    }

    @Test
    fun `sendReply rejects closed conversation`() {
        // Arrange: Prepare an active agent membership and closed conversation. / 준비: active agent membership과 closed conversation을 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("membership-agent", ChannelMembershipRole.AGENT, userId = "user-1")
        every { channelConversationRepository.findById("conversation-1") } returns
            ChannelConversation.start("channel-1", "visitor-1").copy(id = "conversation-1").markClosed()

        // Act: Try to reply to a closed conversation. / 실행: closed conversation에 reply 전송을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.sendReply(replyCommand(actor))
            }

        // Assert: Verify closed conversations remain immutable for replies. / 검증: closed conversation에는 reply가 불가한지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_REPLY_NOT_ALLOWED
    }

    private fun principal(globalRole: PrincipalGlobalRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = globalRole,
            expiresAt = LocalDateTime.now().plusHours(1),
        )

    private fun membership(
        id: String,
        role: ChannelMembershipRole,
        channelId: String = "channel-1",
        userId: String = "agent-1",
        status: ChannelMembershipStatus = ChannelMembershipStatus.ACTIVE,
    ): ChannelMembership =
        ChannelMembership(
            id = id,
            channelId = channelId,
            userId = userId,
            role = role,
            agentStatus = AgentStatus.ONLINE,
            status = status,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun replyCommand(actor: AuthenticatedPrincipal): SendAdminConversationReplyCommand =
        SendAdminConversationReplyCommand(
            actor = actor,
            channelId = "channel-1",
            conversationId = "conversation-1",
            clientMessageId = "client-1",
            content = "hello",
        )
}
