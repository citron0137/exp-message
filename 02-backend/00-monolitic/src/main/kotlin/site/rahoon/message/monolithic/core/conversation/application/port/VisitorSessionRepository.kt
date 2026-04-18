package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession

interface VisitorSessionRepository {
    /**
     * Saves a visitor session.
     */
    fun save(session: VisitorSession): VisitorSession

    /**
     * Finds a visitor session by token hash.
     */
    fun findByTokenHash(tokenHash: String): VisitorSession?
}
