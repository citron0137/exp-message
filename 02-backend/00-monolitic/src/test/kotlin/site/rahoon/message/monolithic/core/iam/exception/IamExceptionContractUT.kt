package site.rahoon.message.monolithic.core.iam.exception

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.shared.error.BoundedContext
import site.rahoon.message.monolithic.core.shared.error.CoreException
import site.rahoon.message.monolithic.core.shared.error.ErrorCategory

class IamExceptionContractUT {
    @Test
    fun `access errors expose IAM core error contract`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val errors = AccessError.entries

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val categories = errors.map { it.category }.toSet()

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        errors.forEach {
            it.boundedContext shouldBe BoundedContext.IAM
            it.code.shouldNotBeBlank()
            it.userMessage.shouldNotBeBlank()
            it.developerMessage.shouldNotBeBlank()
        }
        categories shouldContain ErrorCategory.UNAUTHORIZED
        categories shouldContain ErrorCategory.FORBIDDEN
    }

    @Test
    fun `identity errors expose IAM core error contract`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val errors = IdentityError.entries

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val categories = errors.map { it.category }.toSet()

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        errors.forEach {
            it.boundedContext shouldBe BoundedContext.IAM
            it.code.shouldNotBeBlank()
            it.userMessage.shouldNotBeBlank()
            it.developerMessage.shouldNotBeBlank()
        }
        categories shouldContain ErrorCategory.NOT_FOUND
        categories shouldContain ErrorCategory.CONFLICT
        categories shouldContain ErrorCategory.BAD_REQUEST
    }

    @Test
    fun `IAM exceptions preserve core error and details`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val details = mapOf("email" to "admin@example.com")

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception: CoreException =
            AccessException(
                error = AccessError.INVALID_CREDENTIALS,
                details = details,
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe AccessError.INVALID_CREDENTIALS
        exception.details shouldContain ("email" to "admin@example.com")
        exception.message shouldBe AccessError.INVALID_CREDENTIALS.developerMessage
    }
}
