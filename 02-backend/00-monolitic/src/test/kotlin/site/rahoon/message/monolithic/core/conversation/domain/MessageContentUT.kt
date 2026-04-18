package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

class MessageContentUT {
    @Test
    fun `text trims message content`() {
        // Arrange: Prepare raw message text with surrounding spaces. / 준비: 앞뒤 공백이 있는 message text를 준비한다.
        val rawContent = "  hello  "

        // Act: Create text message content. / 실행: text message content를 생성한다.
        val content = MessageContent.text(rawContent)

        // Assert: Verify normalized content. / 검증: 정규화된 content를 검증한다.
        content.value shouldBe "hello"
    }

    @Test
    fun `text rejects blank message content`() {
        // Arrange: Prepare blank message text. / 준비: 비어 있는 message text를 준비한다.
        val rawContent = "   "

        // Act: Try to create text message content. / 실행: text message content 생성을 시도한다.
        val exception = shouldThrow<ConversationException> { MessageContent.text(rawContent) }

        // Assert: Verify blank content is rejected. / 검증: 빈 content가 거부되는지 검증한다.
        exception.error shouldBe ConversationError.INVALID_MESSAGE_CONTENT
    }
}
