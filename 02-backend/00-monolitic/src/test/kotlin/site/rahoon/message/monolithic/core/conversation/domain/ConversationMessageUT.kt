package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class ConversationMessageUT {
    @Test
    fun `visitorText creates visible visitor text message`() {
        // Arrange: Prepare visitor message identifiers and content. / 준비: visitor message identifier와 content를 준비한다.
        val content = MessageContent.text("hello")

        // Act: Create a visitor text message. / 실행: visitor text message를 생성한다.
        val message =
            ConversationMessage.visitorText(
                conversationId = "conversation-1",
                channelId = "channel-1",
                visitorId = "visitor-1",
                sequence = 1,
                clientMessageId = "client-1",
                content = content,
            )

        // Assert: Verify message identity and default state. / 검증: message identity와 기본 상태를 검증한다.
        message.id.shouldNotBeBlank()
        message.conversationId shouldBe "conversation-1"
        message.channelId shouldBe "channel-1"
        message.sequence shouldBe 1
        message.senderType shouldBe ConversationMessageSenderType.VISITOR
        message.senderId shouldBe "visitor-1"
        message.clientMessageId shouldBe "client-1"
        message.type shouldBe ConversationMessageType.TEXT
        message.status shouldBe ConversationMessageStatus.VISIBLE
        message.content.value shouldBe "hello"
    }
}
