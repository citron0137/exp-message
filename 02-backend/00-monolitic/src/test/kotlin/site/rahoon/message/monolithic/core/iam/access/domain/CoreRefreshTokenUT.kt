package site.rahoon.message.monolithic.core.iam.access.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CoreRefreshTokenUT {
    @Test
    fun `isExpired returns false before expiresAt`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val now = LocalDateTime.of(2026, 4, 18, 10, 0)
        val token = refreshToken(expiresAt = now.plusMinutes(1))

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result = token.isExpired(now)

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result shouldBe false
    }

    @Test
    fun `isExpired returns true at expiresAt`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val now = LocalDateTime.of(2026, 4, 18, 10, 0)
        val token = refreshToken(expiresAt = now)

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result = token.isExpired(now)

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result shouldBe true
    }

    @Test
    fun `isExpired returns true after expiresAt`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val now = LocalDateTime.of(2026, 4, 18, 10, 0)
        val token = refreshToken(expiresAt = now.minusSeconds(1))

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result = token.isExpired(now)

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result shouldBe true
    }

    private fun refreshToken(expiresAt: LocalDateTime): CoreRefreshToken =
        CoreRefreshToken(
            token = "refresh-token",
            userId = "user-1",
            sessionId = "session-1",
            expiresAt = expiresAt,
            createdAt = expiresAt.minusDays(1),
        )
}
