package site.rahoon.message.monolithic.core.conversation.exception

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.shared.error.BoundedContext
import site.rahoon.message.monolithic.core.shared.error.CoreException
import site.rahoon.message.monolithic.core.shared.error.ErrorCategory

class ConversationExceptionContractUT {
    @Test
    fun `conversation errors expose conversation core error contract`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val errors = ConversationError.entries

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val categories = errors.map { it.category }.toSet()

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        errors.forEach {
            it.boundedContext shouldBe BoundedContext.CONVERSATION
            it.code.shouldNotBeBlank()
            it.userMessage.shouldNotBeBlank()
            it.developerMessage.shouldNotBeBlank()
        }
        categories shouldContain ErrorCategory.NOT_FOUND
        categories shouldContain ErrorCategory.FORBIDDEN
    }

    @Test
    fun `conversation exceptions preserve core error and details`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val details = mapOf("channelId" to "channel-1")

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception: CoreException =
            ConversationException(
                error = ConversationError.CHANNEL_NOT_FOUND,
                details = details,
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_NOT_FOUND
        exception.details shouldContain ("channelId" to "channel-1")
        exception.message shouldBe ConversationError.CHANNEL_NOT_FOUND.developerMessage
    }
}
