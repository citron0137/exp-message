package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ConversationVisitorAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

class ConversationVisitorAccessPolicyUT {
    private lateinit var channelConversationRepository: ChannelConversationRepository
    private lateinit var policy: ConversationVisitorAccessPolicy

    @BeforeEach
    fun setUp() {
        channelConversationRepository = mockk()
        policy = ConversationVisitorAccessPolicy(channelConversationRepository)
    }

    @Test
    fun `requireAppendableConversation returns pending owned conversation`() {
        // Arrange: Prepare an owned pending conversation. / 준비: session이 소유한 pending conversation을 준비한다.
        val conversation = ChannelConversation.start("channel-1", "visitor-1")
        every { channelConversationRepository.findById(conversation.id) } returns conversation

        // Act: Require appendable conversation access. / 실행: append 가능한 conversation 접근을 요구한다.
        val result = policy.requireAppendableConversation(conversation.id, visitorSession())

        // Assert: Verify the owned pending conversation is returned. / 검증: 소유한 pending conversation이 반환되는지 검증한다.
        result shouldBe conversation
    }

    @Test
    fun `requireAppendableConversation rejects closed conversation`() {
        // Arrange: Prepare a closed owned conversation. / 준비: session이 소유한 closed conversation을 준비한다.
        val conversation = ChannelConversation.start("channel-1", "visitor-1").markClosed()
        every { channelConversationRepository.findById(conversation.id) } returns conversation

        // Act: Try to require appendable conversation access. / 실행: append 가능한 conversation 접근 요구를 시도한다.
        val exception = shouldThrow<ConversationException> { policy.requireAppendableConversation(conversation.id, visitorSession()) }

        // Assert: Verify closed conversations cannot accept visitor messages. / 검증: closed conversation은 visitor message를 받을 수 없음을 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_NOT_APPENDABLE
    }

    @Test
    fun `requireReadableConversation rejects conversation owned by another visitor`() {
        // Arrange: Prepare a conversation owned by another visitor. / 준비: 다른 visitor가 소유한 conversation을 준비한다.
        val conversation = ChannelConversation.start("channel-1", "visitor-2")
        every { channelConversationRepository.findById(conversation.id) } returns conversation

        // Act: Try to require readable conversation access. / 실행: readable conversation 접근 요구를 시도한다.
        val exception = shouldThrow<ConversationException> { policy.requireReadableConversation(conversation.id, visitorSession()) }

        // Assert: Verify ownership mismatch is hidden as not found. / 검증: 소유자 불일치가 not found로 숨겨지는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_CONVERSATION_NOT_FOUND
    }

    private fun visitorSession(): VisitorSession =
        VisitorSession.create(
            visitorId = "visitor-1",
            channelId = "channel-1",
            tokenHash = "hashed-token",
            expiresAt = LocalDateTime.now().plusDays(1),
        )
}
