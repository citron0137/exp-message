package site.rahoon.message.monolithic.core.iam.identity.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class IdentityUserUT {
    @Test
    fun `create creates identity user with generated id and timestamps`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val email = "admin@example.com"
        val passwordHash = "hashed-password"
        val nickname = "Admin"

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val user =
            IdentityUser.create(
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                globalRole = GlobalRole.PLATFORM_ADMIN,
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        user.id.shouldNotBeBlank()
        user.email shouldBe email
        user.passwordHash shouldBe passwordHash
        user.nickname shouldBe nickname
        user.globalRole shouldBe GlobalRole.PLATFORM_ADMIN
        user.createdAt shouldBe user.updatedAt
    }
}
