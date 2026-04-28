package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenHasher
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

class VisitorSessionPolicyUT {
    private lateinit var visitorSessionRepository: VisitorSessionRepository
    private lateinit var visitorSessionTokenHasher: VisitorSessionTokenHasher
    private lateinit var policy: VisitorSessionPolicy

    @BeforeEach
    fun setUp() {
        visitorSessionRepository = mockk()
        visitorSessionTokenHasher = mockk()
        policy =
            VisitorSessionPolicy(
                visitorSessionRepository = visitorSessionRepository,
                visitorSessionTokenHasher = visitorSessionTokenHasher,
            )
    }

    @Test
    fun `requireValidSession returns non expired channel session`() {
        // Arrange: Prepare a valid visitor session for the channel. / 준비: channel에 속한 유효한 visitor session을 준비한다.
        val session = visitorSession(expiresAt = LocalDateTime.now().plusDays(1))
        every { visitorSessionTokenHasher.hash("wvs_raw") } returns "hashed-token"
        every { visitorSessionRepository.findByTokenHash("hashed-token") } returns session

        // Act: Require a valid visitor session. / 실행: 유효한 visitor session을 요구한다.
        val result = policy.requireValidSession("wvs_raw", "channel-1")

        // Assert: Verify the valid session is returned. / 검증: 유효한 session이 반환되는지 검증한다.
        result shouldBe session
    }

    @Test
    fun `requireValidSession rejects expired session`() {
        // Arrange: Prepare an expired visitor session. / 준비: 만료된 visitor session을 준비한다.
        val session = visitorSession(expiresAt = LocalDateTime.now().minusSeconds(1))
        every { visitorSessionTokenHasher.hash("wvs_raw") } returns "hashed-token"
        every { visitorSessionRepository.findByTokenHash("hashed-token") } returns session

        // Act: Try to require a valid visitor session. / 실행: 유효한 visitor session 요구를 시도한다.
        val exception = shouldThrow<ConversationException> { policy.requireValidSession("wvs_raw", "channel-1") }

        // Assert: Verify expired sessions are rejected. / 검증: 만료된 session이 거부되는지 검증한다.
        exception.error shouldBe ConversationError.VISITOR_SESSION_EXPIRED
    }

    private fun visitorSession(expiresAt: LocalDateTime): VisitorSession =
        VisitorSession.create(
            visitorId = "visitor-1",
            channelId = "channel-1",
            tokenHash = "hashed-token",
            expiresAt = expiresAt,
        )
}
