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
        membership.status shouldBe ChannelMembershipStatus.ACTIVE
        membership.canBeAssigned() shouldBe true
        membership.createdAt shouldBe membership.updatedAt
    }

    @Test
    fun `create creates active membership for requested role`() {
        // Arrange: Prepare channel, user, and role identifiers. / 준비: channel, user, role identifier를 준비한다.
        val channelId = "channel-1"
        val userId = "user-1"

        // Act: Create an agent membership. / 실행: agent membership을 생성한다.
        val membership =
            ChannelMembership.create(
                channelId = channelId,
                userId = userId,
                role = ChannelMembershipRole.AGENT,
            )

        // Assert: Verify requested role and active status. / 검증: 요청한 role과 active status를 검증한다.
        membership.role shouldBe ChannelMembershipRole.AGENT
        membership.status shouldBe ChannelMembershipStatus.ACTIVE
        membership.agentStatus shouldBe AgentStatus.OFFLINE
    }
}
