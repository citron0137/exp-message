package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.query.AdminInboxCursor
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

class AdminInboxCursorUT {
    @Test
    fun `encode and decode preserves activity and id`() {
        // Arrange: Prepare an admin inbox cursor. / 준비: admin inbox cursor를 준비한다.
        val cursor =
            AdminInboxCursor(
                activityAt = LocalDateTime.parse("2026-04-18T10:00:00"),
                id = "conversation-1",
            )

        // Act: Encode and decode the cursor. / 실행: cursor를 encode 후 decode한다.
        val decoded = AdminInboxCursor.decode(cursor.encode())

        // Assert: Verify cursor fields are preserved. / 검증: cursor 필드가 보존되는지 검증한다.
        decoded shouldBe cursor
    }

    @Test
    fun `decode throws for invalid cursor`() {
        // Arrange: Prepare an invalid cursor. / 준비: 올바르지 않은 cursor를 준비한다.
        val rawCursor = "invalid-cursor"

        // Act: Decode the invalid cursor. / 실행: 올바르지 않은 cursor decode를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                AdminInboxCursor.decode(rawCursor)
            }

        // Assert: Verify invalid cursor error is returned. / 검증: invalid cursor error가 반환되는지 검증한다.
        exception.error shouldBe ConversationError.INVALID_ADMIN_INBOX_CURSOR
    }
}
