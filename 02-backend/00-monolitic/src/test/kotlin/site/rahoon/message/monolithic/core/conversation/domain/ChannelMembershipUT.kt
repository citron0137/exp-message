package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class ChannelMembershipUT {
    @Test
    fun `createChannelAdmin creates offline channel admin membership`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val channelId = "channel-1"
        val userId = "user-1"

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val membership =
            ChannelMembership.createChannelAdmin(
                channelId = channelId,
                userId = userId,
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        membership.id.shouldNotBeBlank()
        membership.channelId shouldBe channelId
        membership.userId shouldBe userId
        membership.role shouldBe ChannelMembershipRole.CHANNEL_ADMIN
        membership.agentStatus shouldBe AgentStatus.OFFLINE
        membership.createdAt shouldBe membership.updatedAt
    }
}
