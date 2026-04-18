package site.rahoon.message.monolithic.core.conversation.application.service

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenHasher
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

@Service
class VisitorSessionPolicy(
    private val visitorSessionRepository: VisitorSessionRepository,
    private val visitorSessionTokenHasher: VisitorSessionTokenHasher,
) {
    /**
     * Requires a non-expired visitor session for the requested channel.
     */
    fun requireValidSession(
        rawToken: String,
        channelId: String,
    ): VisitorSession {
        val tokenHash = visitorSessionTokenHasher.hash(rawToken)
        val session = requireExistingSession(tokenHash)
        requireSessionChannel(session, channelId)
        requireNotExpired(session)
        return session
    }

    private fun requireExistingSession(tokenHash: String): VisitorSession =
        visitorSessionRepository.findByTokenHash(tokenHash)
            ?: throw ConversationException(ConversationError.VISITOR_SESSION_NOT_FOUND)

    private fun requireSessionChannel(
        session: VisitorSession,
        channelId: String,
    ) {
        if (session.channelId != channelId) {
            throw ConversationException(ConversationError.VISITOR_SESSION_NOT_FOUND)
        }
    }

    private fun requireNotExpired(session: VisitorSession) {
        if (session.isExpired(LocalDateTime.now())) {
            throw ConversationException(
                error = ConversationError.VISITOR_SESSION_EXPIRED,
                details = mapOf("sessionId" to session.id),
            )
        }
    }
}
