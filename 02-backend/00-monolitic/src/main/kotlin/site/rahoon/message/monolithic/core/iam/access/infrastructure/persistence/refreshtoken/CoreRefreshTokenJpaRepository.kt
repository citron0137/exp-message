package site.rahoon.message.monolithic.core.iam.access.infrastructure.persistence.refreshtoken

import org.springframework.data.jpa.repository.JpaRepository

interface CoreRefreshTokenJpaRepository : JpaRepository<CoreRefreshTokenEntity, String> {
    /**
     * Deletes refresh token entities by session identifier.
     */
    fun deleteBySessionId(sessionId: String)
}
