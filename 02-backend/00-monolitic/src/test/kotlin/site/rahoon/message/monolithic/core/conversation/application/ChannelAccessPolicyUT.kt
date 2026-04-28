package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class ChannelAccessPolicyUT {
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var policy: ChannelAccessPolicy

    @BeforeEach
    fun setUp() {
        channelMembershipRepository = mockk()
        policy = ChannelAccessPolicy(channelMembershipRepository)
    }

    @Test
    fun `requirePlatformAdmin passes for platform admin`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val principal = principal(PrincipalGlobalRole.PLATFORM_ADMIN)

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        policy.requirePlatformAdmin(principal)

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        principal.isPlatformAdmin() shouldBe true
    }

    @Test
    fun `requirePlatformAdmin throws for channel user`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val principal = principal(PrincipalGlobalRole.CHANNEL_USER)

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception =
            shouldThrow<ConversationException> {
                policy.requirePlatformAdmin(principal)
            }

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe ConversationError.PLATFORM_ADMIN_REQUIRED
    }

    @Test
    fun `requireChannelRead passes for platform admin without membership lookup`() {
        // Arrange: Prepare a platform admin principal. / 준비: platform admin principal을 준비한다.
        val principal = principal(PrincipalGlobalRole.PLATFORM_ADMIN)

        // Act: Require channel read access. / 실행: channel read access를 요구한다.
        policy.requireChannelRead(principal, "channel-1")

        // Assert: Verify platform admin bypasses membership lookup. / 검증: platform admin은 membership 조회 없이 통과하는지 검증한다.
        verify(exactly = 0) { channelMembershipRepository.findByChannelIdAndUserId(any(), any()) }
    }

    @Test
    fun `requireChannelRead passes for channel member`() {
        // Arrange: Prepare a channel user with channel membership. / 준비: channel membership이 있는 channel user를 준비한다.
        val principal = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns
            ChannelMembership.createChannelAdmin("channel-1", "user-1")

        // Act: Require channel read access. / 실행: channel read access를 요구한다.
        policy.requireChannelRead(principal, "channel-1")

        // Assert: Verify membership grants read access. / 검증: membership이 read access를 허용하는지 검증한다.
        verify { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") }
    }

    @Test
    fun `requireChannelRead throws for channel user without membership`() {
        // Arrange: Prepare a channel user without channel membership. / 준비: channel membership이 없는 channel user를 준비한다.
        val principal = principal(PrincipalGlobalRole.CHANNEL_USER)
        every { channelMembershipRepository.findByChannelIdAndUserId("channel-1", "user-1") } returns null

        // Act: Require channel read access. / 실행: channel read access를 요구한다.
        val exception =
            shouldThrow<ConversationException> {
                policy.requireChannelRead(principal, "channel-1")
            }

        // Assert: Verify membership is required for channel users. / 검증: channel user에게 membership이 필요한지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_ACCESS_DENIED
        exception.details["channelId"] shouldBe "channel-1"
        exception.details["userId"] shouldBe "user-1"
    }

    private fun principal(globalRole: PrincipalGlobalRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = globalRole,
            expiresAt = LocalDateTime.now().plusHours(1),
        )
}
