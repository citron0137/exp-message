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
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminConversationFacadeUT {
    private lateinit var channelConversationRepository: ChannelConversationRepository
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var facade: AdminConversationFacade

    @BeforeEach
    fun setUp() {
        channelConversationRepository = mockk()
        channelMembershipRepository = mockk()
        facade =
            AdminConversationFacade(
                channelAccessPolicy = ChannelAccessPolicy(channelMembershipRepository),
                channelConversationRepository = channelConversationRepository,
                channelMembershipRepository = channelMembershipRepository,
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
    ): ChannelMembership =
        ChannelMembership(
            id = id,
            channelId = channelId,
            userId = "agent-1",
            role = role,
            agentStatus = AgentStatus.ONLINE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
}
