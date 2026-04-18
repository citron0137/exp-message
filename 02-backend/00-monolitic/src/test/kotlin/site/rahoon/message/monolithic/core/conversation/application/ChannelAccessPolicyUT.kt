package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class ChannelAccessPolicyUT {
    private val policy = ChannelAccessPolicy()

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

    private fun principal(globalRole: PrincipalGlobalRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = globalRole,
            expiresAt = LocalDateTime.now().plusHours(1),
        )
}
