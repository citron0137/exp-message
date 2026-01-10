package site.rahoon.message.__monolitic.authtoken.infrastructure

import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenRepository
import java.time.LocalDateTime

@Repository
class AuthTokenRepositoryImpl(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository
) : AuthTokenRepository {

    override fun saveRefreshToken(userId: String, refreshToken: String, expiresAt: LocalDateTime) {
        refreshTokenJpaRepository.save(
            RefreshTokenEntity(
                refreshToken = refreshToken,
                userId = userId,
                expiresAt = expiresAt,
                createdAt = LocalDateTime.now()
            )
        )
    }

    override fun findUserIdByRefreshToken(refreshToken: String): String? {
        val entity = refreshTokenJpaRepository.findById(refreshToken).orElse(null) ?: return null
        if (LocalDateTime.now().isAfter(entity.expiresAt)) {
            refreshTokenJpaRepository.deleteById(refreshToken)
            return null
        }
        return entity.userId
    }

    override fun deleteRefreshToken(refreshToken: String) {
        refreshTokenJpaRepository.deleteById(refreshToken)
    }

    override fun deleteAllRefreshTokensByUserId(userId: String) {
        refreshTokenJpaRepository.deleteAllByUserId(userId)
    }
}

