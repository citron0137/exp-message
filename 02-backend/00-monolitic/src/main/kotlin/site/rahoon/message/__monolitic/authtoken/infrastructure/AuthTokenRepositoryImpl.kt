package site.rahoon.message.__monolitic.authtoken.infrastructure

import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenRepository
import site.rahoon.message.__monolitic.authtoken.domain.RefreshTokenSession
import java.time.LocalDateTime

@Repository
class AuthTokenRepositoryImpl(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository
) : AuthTokenRepository {

    override fun saveRefreshToken(userId: String, sessionId: String, refreshToken: String, expiresAt: LocalDateTime) {
        refreshTokenJpaRepository.save(
            RefreshTokenEntity(
                refreshToken = refreshToken,
                userId = userId,
                sessionId = sessionId,
                expiresAt = expiresAt,
                createdAt = LocalDateTime.now()
            )
        )
    }

    override fun findSessionByRefreshToken(refreshToken: String): RefreshTokenSession? {
        val entity = refreshTokenJpaRepository.findById(refreshToken).orElse(null) ?: return null
        if (LocalDateTime.now().isAfter(entity.expiresAt)) {
            refreshTokenJpaRepository.deleteById(refreshToken)
            return null
        }
        return RefreshTokenSession(
            userId = entity.userId,
            sessionId = entity.sessionId
        )
    }

    override fun deleteRefreshToken(refreshToken: String) {
        refreshTokenJpaRepository.deleteById(refreshToken)
    }

    override fun deleteAllRefreshTokensBySessionId(sessionId: String) {
        refreshTokenJpaRepository.deleteAllBySessionId(sessionId)
    }

    override fun deleteAllRefreshTokensByUserId(userId: String) {
        refreshTokenJpaRepository.deleteAllByUserId(userId)
    }
}

