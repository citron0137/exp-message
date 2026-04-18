package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.query.AdminWorkspaceQueryService
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminWorkspaceQueryServiceUT {
    private lateinit var channelRepository: ChannelRepository
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var queryService: AdminWorkspaceQueryService

    @BeforeEach
    fun setUp() {
        channelRepository = mockk()
        channelMembershipRepository = mockk()
        queryService =
            AdminWorkspaceQueryService(
                channelRepository = channelRepository,
                channelMembershipRepository = channelMembershipRepository,
            )
    }

    @Test
    fun `listMyChannels returns every channel for platform admin`() {
        // Arrange: Prepare a platform admin and two channels. / 준비: platform admin과 두 channel을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.findAll() } returns listOf(channel("channel-1"), channel("channel-2"))

        // Act: List workspace channels. / 실행: workspace channel을 조회한다.
        val result = queryService.listMyChannels(actor)

        // Assert: Verify all channels are returned without membership. / 검증: 모든 channel이 membership 없이 반환되는지 검증한다.
        result.shouldHaveSize(2)
        result[0].membership shouldBe null
        verify(exactly = 0) { channelMembershipRepository.findByUserId(any()) }
    }

    @Test
    fun `listMyChannels returns membership channels for channel user`() {
        // Arrange: Prepare a channel user with one membership. / 준비: 하나의 membership을 가진 channel user를 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByUserId("user-1") } returns listOf(membership("membership-1", "channel-1"))
        every { channelRepository.findById("channel-1") } returns channel("channel-1")

        // Act: List workspace channels. / 실행: workspace channel을 조회한다.
        val result = queryService.listMyChannels(actor)

        // Assert: Verify only membership channels are returned. / 검증: membership이 있는 channel만 반환되는지 검증한다.
        result.shouldHaveSize(1)
        result[0].channel.id shouldBe "channel-1"
        result[0].membership?.id shouldBe "membership-1"
    }

    private fun principal(globalRole: PrincipalGlobalRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = globalRole,
            expiresAt = LocalDateTime.now().plusHours(1),
        )

    private fun channel(id: String): Channel =
        Channel(
            id = id,
            name = "Channel $id",
            status = ChannelStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun membership(
        id: String,
        channelId: String,
    ): ChannelMembership =
        ChannelMembership(
            id = id,
            channelId = channelId,
            userId = "user-1",
            role = ChannelMembershipRole.AGENT,
            agentStatus = AgentStatus.ONLINE,
            status = ChannelMembershipStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
}
