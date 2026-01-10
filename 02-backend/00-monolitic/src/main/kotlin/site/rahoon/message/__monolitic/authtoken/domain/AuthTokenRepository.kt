package site.rahoon.message.__monolitic.authtoken.domain

interface AuthTokenRepository {
    /**
     * 리프레시 토큰을 저장합니다.
     */
    fun saveRefreshToken(userId: String, refreshToken: String, expiresAt: java.time.LocalDateTime)

    /**
     * 리프레시 토큰으로 사용자 ID를 조회합니다.
     */
    fun findUserIdByRefreshToken(refreshToken: String): String?

    /**
     * 리프레시 토큰을 삭제합니다.
     */
    fun deleteRefreshToken(refreshToken: String)

    /**
     * 사용자의 모든 리프레시 토큰을 삭제합니다.
     */
    fun deleteAllRefreshTokensByUserId(userId: String)
}

