package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class ChannelConversationUT {
    @Test
    fun `start creates pending channel conversation`() {
        // Arrange: Prepare channel and visitor identifiers. / 준비: channel과 visitor identifier를 준비한다.
        val channelId = "channel-1"
        val visitorId = "visitor-1"

        // Act: Start a channel conversation. / 실행: channel conversation을 시작한다.
        val conversation = ChannelConversation.start(channelId, visitorId)

        // Assert: Verify pending conversation state. / 검증: pending conversation 상태를 검증한다.
        conversation.id.shouldNotBeBlank()
        conversation.channelId shouldBe channelId
        conversation.visitorId shouldBe visitorId
        conversation.status shouldBe ChannelConversationStatus.PENDING
        conversation.closedAt.shouldBeNull()
    }

    @Test
    fun `markClosed returns closed conversation`() {
        // Arrange: Prepare a pending conversation. / 준비: pending conversation을 준비한다.
        val conversation = ChannelConversation.start("channel-1", "visitor-1")

        // Act: Close the conversation. / 실행: conversation을 닫는다.
        val closed = conversation.markClosed()

        // Assert: Verify closed conversation state. / 검증: 닫힌 conversation 상태를 검증한다.
        closed.status shouldBe ChannelConversationStatus.CLOSED
        closed.closedAt shouldBe closed.updatedAt
    }

    @Test
    fun `reactivateAsPending changes dormant to pending`() {
        // Arrange: Prepare a dormant conversation. / 준비: dormant conversation을 준비한다.
        val conversation = ChannelConversation.start("channel-1", "visitor-1").markDormant()

        // Act: Reactivate the conversation as pending. / 실행: conversation을 pending으로 재활성화한다.
        val reactivated = conversation.reactivateAsPending()

        // Assert: Verify dormant conversations become pending. / 검증: dormant conversation이 pending으로 변경되는지 검증한다.
        reactivated.status shouldBe ChannelConversationStatus.PENDING
        reactivated.closedAt.shouldBeNull()
    }

    @Test
    fun `policy methods distinguish reusable viewable and message-acceptable states`() {
        // Arrange: Prepare conversations in every lifecycle state. / 준비: 모든 lifecycle 상태의 conversation을 준비한다.
        val pending = ChannelConversation.start("channel-1", "visitor-1")
        val open = pending.markOpen()
        val dormant = pending.markDormant()
        val closed = pending.markClosed()

        // Act: Evaluate visitor lifecycle policies. / 실행: visitor lifecycle policy를 평가한다.
        val reusable = listOf(pending, open, dormant, closed).map { it.canReuseForVisitorEntry() }
        val messageAcceptable = listOf(pending, open, dormant, closed).map { it.canAcceptVisitorMessage() }
        val viewable = listOf(pending, open, dormant, closed).map { it.canBeViewedByVisitor() }

        // Assert: Verify closed is excluded and dormant needs reactivation before messages. / 검증: closed 제외와 dormant message 전 재활성화 필요성을 검증한다.
        reusable shouldBe listOf(true, true, true, false)
        messageAcceptable shouldBe listOf(true, true, false, false)
        viewable shouldBe listOf(true, true, true, false)
    }
}
