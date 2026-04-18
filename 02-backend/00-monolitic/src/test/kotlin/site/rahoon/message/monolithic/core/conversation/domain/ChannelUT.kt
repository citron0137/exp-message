package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class ChannelUT {
    @Test
    fun `create creates active channel with generated id and timestamps`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val name = "Acme"

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val channel = Channel.create(name)

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        channel.id.shouldNotBeBlank()
        channel.name shouldBe name
        channel.status shouldBe ChannelStatus.ACTIVE
        channel.createdAt shouldBe channel.updatedAt
    }
}
