package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.query.AdminChannelMembershipQueryService
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminChannelMembershipQueryServiceUT {
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var queryService: AdminChannelMembershipQueryService

    @BeforeEach
    fun setUp() {
        channelMembershipRepository = mockk()
        queryService =
            AdminChannelMembershipQueryService(
                channelAccessPolicy = ChannelAccessPolicy(channelMembershipRepository),
                channelMembershipRepository = channelMembershipRepository,
            )
    }

    @Test
    fun `listByChannel applies role and status filters`() {
        // Arrange: Prepare a channel admin actor and filtered memberships. / 준비: channel admin actor와 filter된 memberships를 준비한다.
        val actor = principal()
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN, userId = "user-1")
        every {
            channelMembershipRepository.findByChannelIdAndFilters(
                "channel-1",
                ChannelMembershipRole.AGENT,
                ChannelMembershipStatus.ACTIVE,
            )
        } returns listOf(membership("agent-membership", ChannelMembershipRole.AGENT))

        // Act: List memberships with role and status filters. / 실행: role과 status filter로 membership을 조회한다.
        val result =
            queryService.listByChannel(
                actor = actor,
                channelId = "channel-1",
                role = ChannelMembershipRole.AGENT,
                status = ChannelMembershipStatus.ACTIVE,
            )

        // Assert: Verify filtered memberships are returned. / 검증: filter된 membership이 반환되는지 검증한다.
        result.shouldHaveSize(1)
        result[0].role shouldBe ChannelMembershipRole.AGENT
        verify {
            channelMembershipRepository.findByChannelIdAndFilters(
                "channel-1",
                ChannelMembershipRole.AGENT,
                ChannelMembershipStatus.ACTIVE,
            )
        }
    }

    private fun principal(): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = PrincipalGlobalRole.CHANNEL_USER,
            expiresAt = LocalDateTime.now().plusHours(1),
        )

    private fun membership(
        id: String,
        role: ChannelMembershipRole,
        userId: String = "agent-1",
    ): ChannelMembership =
        ChannelMembership(
            id = id,
            channelId = "channel-1",
            userId = userId,
            role = role,
            agentStatus = AgentStatus.ONLINE,
            status = ChannelMembershipStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
}
