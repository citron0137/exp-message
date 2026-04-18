package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminChannelMembershipFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeChannelMembershipRoleCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeChannelMembershipStatusCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateChannelMembershipCommand
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMemberIdentity
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMemberIdentityPort
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminChannelMembershipFacadeUT {
    private lateinit var channelRepository: ChannelRepository
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var channelMemberIdentityPort: ChannelMemberIdentityPort
    private lateinit var facade: AdminChannelMembershipFacade

    @BeforeEach
    fun setUp() {
        channelRepository = mockk()
        channelMembershipRepository = mockk()
        channelMemberIdentityPort = mockk()
        facade =
            AdminChannelMembershipFacade(
                channelAccessPolicy = ChannelAccessPolicy(channelMembershipRepository),
                channelRepository = channelRepository,
                channelMembershipRepository = channelMembershipRepository,
                channelMemberIdentityPort = channelMemberIdentityPort,
            )
    }

    @Test
    fun `createMembership creates agent membership for channel admin`() {
        // Arrange: Prepare a channel admin actor and a new agent identity. / 준비: channel admin actor와 새 agent identity를 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN, userId = "user-1")
        every { channelRepository.findById("channel-1") } returns Channel.create("Acme").copy(id = "channel-1")
        every { channelMemberIdentityPort.createOrLoadChannelMember(any()) } returns
            ChannelMemberIdentity(
                userId = "agent-1",
                email = "agent@example.com",
                nickname = "Agent",
                temporaryPassword = "temporary-password",
                created = true,
            )
        every { channelMembershipRepository.existsByChannelIdAndUserId("channel-1", "agent-1") } returns false
        every { channelMembershipRepository.save(any()) } answers { firstArg() }

        // Act: Create the agent membership. / 실행: agent membership을 생성한다.
        val result = facade.createMembership(command(actor, ChannelMembershipRole.AGENT))

        // Assert: Verify membership and one-time identity password. / 검증: membership과 1회성 identity password를 검증한다.
        result.membership.channelId shouldBe "channel-1"
        result.membership.userId shouldBe "agent-1"
        result.membership.role shouldBe ChannelMembershipRole.AGENT
        result.membership.status shouldBe ChannelMembershipStatus.ACTIVE
        result.identity.temporaryPassword shouldBe "temporary-password"
        result.identity.created shouldBe true
        verify { channelMembershipRepository.save(match { it.role == ChannelMembershipRole.AGENT }) }
    }

    @Test
    fun `createMembership rejects channel admin creating another channel admin`() {
        // Arrange: Prepare a channel admin actor requesting CHANNEL_ADMIN role. / 준비: CHANNEL_ADMIN role을 요청하는 channel admin actor를 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN, userId = "user-1")

        // Act: Try to create another channel admin. / 실행: 다른 channel admin 생성을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.createMembership(command(actor, ChannelMembershipRole.CHANNEL_ADMIN))
            }

        // Assert: Verify role escalation is blocked before identity side effects. / 검증: identity side effect 전에 role escalation이 차단되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_MEMBERSHIP_ROLE_NOT_ALLOWED
        verify(exactly = 0) { channelMemberIdentityPort.createOrLoadChannelMember(any()) }
        verify(exactly = 0) { channelMembershipRepository.save(any()) }
    }

    @Test
    fun `createMembership allows platform admin to create channel admin`() {
        // Arrange: Prepare a platform admin and existing identity. / 준비: platform admin과 기존 identity를 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.findById("channel-1") } returns Channel.create("Acme").copy(id = "channel-1")
        every { channelMemberIdentityPort.createOrLoadChannelMember(any()) } returns
            ChannelMemberIdentity(
                userId = "admin-2",
                email = "admin2@example.com",
                nickname = "Admin 2",
                temporaryPassword = null,
                created = false,
            )
        every { channelMembershipRepository.existsByChannelIdAndUserId("channel-1", "admin-2") } returns false
        every { channelMembershipRepository.save(any()) } answers { firstArg() }

        // Act: Create the channel admin membership. / 실행: channel admin membership을 생성한다.
        val result = facade.createMembership(command(actor, ChannelMembershipRole.CHANNEL_ADMIN))

        // Assert: Verify platform admin can grant CHANNEL_ADMIN. / 검증: platform admin이 CHANNEL_ADMIN을 부여할 수 있는지 검증한다.
        result.membership.role shouldBe ChannelMembershipRole.CHANNEL_ADMIN
        result.identity.created shouldBe false
        result.identity.temporaryPassword shouldBe null
    }

    @Test
    fun `createMembership rejects duplicate channel user membership`() {
        // Arrange: Prepare an existing channel membership for the loaded identity. / 준비: 불러온 identity에 기존 channel membership이 있는 상태를 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.findById("channel-1") } returns Channel.create("Acme").copy(id = "channel-1")
        every { channelMemberIdentityPort.createOrLoadChannelMember(any()) } returns
            ChannelMemberIdentity(
                userId = "agent-1",
                email = "agent@example.com",
                nickname = "Agent",
                temporaryPassword = null,
                created = false,
            )
        every { channelMembershipRepository.existsByChannelIdAndUserId("channel-1", "agent-1") } returns true

        // Act: Try to create a duplicate membership. / 실행: 중복 membership 생성을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.createMembership(command(actor, ChannelMembershipRole.AGENT))
            }

        // Assert: Verify duplicate membership is rejected. / 검증: 중복 membership이 거부되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_MEMBERSHIP_ALREADY_EXISTS
        verify(exactly = 0) { channelMembershipRepository.save(any()) }
    }

    @Test
    fun `disable disables agent membership for channel admin`() {
        // Arrange: Prepare a channel admin actor and target agent membership. / 준비: channel admin actor와 대상 agent membership을 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findById("agent-membership") } returns
            membership("agent-membership", ChannelMembershipRole.AGENT)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN, userId = "user-1")
        every { channelMembershipRepository.save(any()) } answers { firstArg() }

        // Act: Disable the agent membership. / 실행: agent membership을 비활성화한다.
        val result =
            facade.disable(
                ChangeChannelMembershipStatusCommand(
                    actor = actor,
                    channelId = "channel-1",
                    membershipId = "agent-membership",
                ),
            )

        // Assert: Verify channel admin can disable agent memberships. / 검증: channel admin이 agent membership을 비활성화할 수 있는지 검증한다.
        result.status shouldBe ChannelMembershipStatus.DISABLED
        verify { channelMembershipRepository.save(match { it.status == ChannelMembershipStatus.DISABLED }) }
    }

    @Test
    fun `disable rejects channel admin disabling themselves`() {
        // Arrange: Prepare a channel admin actor targeting their own membership. / 준비: 자기 자신의 membership을 대상으로 하는 channel admin actor를 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findById("admin-membership") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN, userId = "user-1")
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN, userId = "user-1")

        // Act: Try to disable the actor's own membership. / 실행: actor 자신의 membership 비활성화를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.disable(
                    ChangeChannelMembershipStatusCommand(
                        actor = actor,
                        channelId = "channel-1",
                        membershipId = "admin-membership",
                    ),
                )
            }

        // Assert: Verify self-disable is rejected. / 검증: 자기 자신의 비활성화가 거부되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_MEMBERSHIP_SELF_CHANGE_NOT_ALLOWED
        verify(exactly = 0) { channelMembershipRepository.save(any()) }
    }

    @Test
    fun `disable rejects removing the last active channel admin`() {
        // Arrange: Prepare a platform admin disabling the last active channel admin. / 준비: 마지막 active channel admin을 비활성화하는 platform admin을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelMembershipRepository.findById("admin-membership") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN)
        every {
            channelMembershipRepository.countByChannelIdAndRoleAndStatus(
                "channel-1",
                ChannelMembershipRole.CHANNEL_ADMIN,
                ChannelMembershipStatus.ACTIVE,
            )
        } returns 1

        // Act: Try to disable the last active channel admin. / 실행: 마지막 active channel admin 비활성화를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.disable(
                    ChangeChannelMembershipStatusCommand(
                        actor = actor,
                        channelId = "channel-1",
                        membershipId = "admin-membership",
                    ),
                )
            }

        // Assert: Verify every channel must retain one active admin. / 검증: channel마다 active admin 1명이 유지되어야 하는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_MEMBERSHIP_LAST_ADMIN_REQUIRED
    }

    @Test
    fun `changeRole allows platform admin to demote channel admin when another active admin remains`() {
        // Arrange: Prepare a platform admin and at least two active channel admins. / 준비: platform admin과 2명 이상의 active channel admin을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelMembershipRepository.findById("admin-membership") } returns
            membership("admin-membership", ChannelMembershipRole.CHANNEL_ADMIN)
        every {
            channelMembershipRepository.countByChannelIdAndRoleAndStatus(
                "channel-1",
                ChannelMembershipRole.CHANNEL_ADMIN,
                ChannelMembershipStatus.ACTIVE,
            )
        } returns 2
        every { channelMembershipRepository.save(any()) } answers { firstArg() }

        // Act: Demote the channel admin to agent. / 실행: channel admin을 agent로 강등한다.
        val result =
            facade.changeRole(
                ChangeChannelMembershipRoleCommand(
                    actor = actor,
                    channelId = "channel-1",
                    membershipId = "admin-membership",
                    role = ChannelMembershipRole.AGENT,
                ),
            )

        // Assert: Verify platform admin can change roles while preserving the last admin invariant. / 검증: 마지막 admin invariant를 지키면 platform admin이 role을 변경할 수 있는지 검증한다.
        result.role shouldBe ChannelMembershipRole.AGENT
    }

    @Test
    fun `changeRole rejects channel admin role mutation`() {
        // Arrange: Prepare a channel admin trying to promote an agent. / 준비: agent 승격을 시도하는 channel admin을 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findById("agent-membership") } returns
            membership("agent-membership", ChannelMembershipRole.AGENT)

        // Act: Try to change the role as channel admin. / 실행: channel admin으로 role 변경을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.changeRole(
                    ChangeChannelMembershipRoleCommand(
                        actor = actor,
                        channelId = "channel-1",
                        membershipId = "agent-membership",
                        role = ChannelMembershipRole.CHANNEL_ADMIN,
                    ),
                )
            }

        // Assert: Verify role mutation is platform-admin-only. / 검증: role mutation은 platform admin 전용인지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_MEMBERSHIP_ROLE_CHANGE_NOT_ALLOWED
    }

    private fun command(
        actor: AuthenticatedPrincipal,
        role: ChannelMembershipRole,
    ): CreateChannelMembershipCommand =
        CreateChannelMembershipCommand(
            actor = actor,
            channelId = "channel-1",
            email = "agent@example.com",
            nickname = "Agent",
            role = role,
        )

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
