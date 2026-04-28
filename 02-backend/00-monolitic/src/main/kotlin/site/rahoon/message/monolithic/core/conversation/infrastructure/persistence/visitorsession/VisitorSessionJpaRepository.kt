package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.visitorsession

import org.springframework.data.jpa.repository.JpaRepository

interface VisitorSessionJpaRepository : JpaRepository<VisitorSessionEntity, String> {
    /**
     * Finds a visitor session by token hash.
     */
    fun findByTokenHash(tokenHash: String): VisitorSessionEntity?
}
