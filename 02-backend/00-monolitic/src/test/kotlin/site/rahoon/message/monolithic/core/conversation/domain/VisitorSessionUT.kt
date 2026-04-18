package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VisitorSessionUT {
    @Test
    fun `create creates visitor session with generated id`() {
        // Arrange: Prepare visitor session values. / 준비: visitor session 값을 준비한다.
        val expiresAt = LocalDateTime.of(2026, 4, 18, 10, 0)

        // Act: Create a visitor session. / 실행: visitor session을 생성한다.
        val session =
            VisitorSession.create(
                visitorId = "visitor-1",
                channelId = "channel-1",
                tokenHash = "hash",
                expiresAt = expiresAt,
            )

        // Assert: Verify generated id and session values. / 검증: 생성된 id와 session 값을 검증한다.
        session.id.shouldNotBeBlank()
        session.visitorId shouldBe "visitor-1"
        session.channelId shouldBe "channel-1"
        session.tokenHash shouldBe "hash"
        session.expiresAt shouldBe expiresAt
    }

    @Test
    fun `isExpired returns true at expiresAt`() {
        // Arrange: Prepare a session that expires now. / 준비: 현재 시점에 만료되는 session을 준비한다.
        val now = LocalDateTime.of(2026, 4, 18, 10, 0)
        val session = VisitorSession.create("visitor-1", "channel-1", "hash", now)

        // Act: Check session expiration. / 실행: session 만료 여부를 확인한다.
        val result = session.isExpired(now)

        // Assert: Verify the session is expired. / 검증: session이 만료되었는지 검증한다.
        result shouldBe true
    }
}
