package site.rahoon.message.monolithic.core.iam.access.infrastructure.persistence.refreshtoken

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.iam.access.application.port.CoreRefreshTokenRepository
import site.rahoon.message.monolithic.core.iam.access.domain.CoreRefreshToken

@Repository
class CoreRefreshTokenRepositoryAdapter(
    private val jpaRepository: CoreRefreshTokenJpaRepository,
) : CoreRefreshTokenRepository {
    /**
     * Saves a refresh token through JPA.
     */
    override fun save(refreshToken: CoreRefreshToken): CoreRefreshToken = jpaRepository.save(refreshToken.toEntity()).toDomain()

    /**
     * Finds a refresh token by token string through JPA.
     */
    override fun findByToken(token: String): CoreRefreshToken? = jpaRepository.findById(token).orElse(null)?.toDomain()

    /**
     * Deletes a refresh token by token string through JPA.
     */
    override fun deleteByToken(token: String) {
        jpaRepository.deleteById(token)
    }

    /**
     * Deletes refresh tokens by session identifier through JPA.
     */
    override fun deleteBySessionId(sessionId: String) {
        jpaRepository.deleteBySessionId(sessionId)
    }

    /**
     * Maps a refresh token domain object to a JPA entity.
     */
    private fun CoreRefreshToken.toEntity(): CoreRefreshTokenEntity =
        CoreRefreshTokenEntity(
            token = token,
            userId = userId,
            sessionId = sessionId,
            expiresAt = expiresAt,
            createdAt = createdAt,
        )

    /**
     * Maps a refresh token JPA entity to a domain object.
     */
    private fun CoreRefreshTokenEntity.toDomain(): CoreRefreshToken =
        CoreRefreshToken(
            token = token,
            userId = userId,
            sessionId = sessionId,
            expiresAt = expiresAt,
            createdAt = createdAt,
        )
}
