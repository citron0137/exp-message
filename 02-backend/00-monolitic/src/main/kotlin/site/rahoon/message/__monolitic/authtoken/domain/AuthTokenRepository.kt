package site.rahoon.message.__monolitic.authtoken.domain

interface AuthTokenRepository {
    /** 리프레시 토큰을 저장합니다. (세션 단위) */
    fun saveRefreshToken(userId: String, sessionId: String, refreshToken: String, expiresAt: java.time.LocalDateTime)

    /** 리프레시 토큰으로 사용자/세션을 조회합니다. */
    fun findSessionByRefreshToken(refreshToken: String): RefreshTokenSession?

    /** 리프레시 토큰을 삭제합니다. */
    fun deleteRefreshToken(refreshToken: String)

    /** 특정 세션의 모든 리프레시 토큰을 삭제합니다. (세션 로그아웃) */
    fun deleteAllRefreshTokensBySessionId(sessionId: String)

    /** 사용자의 모든 리프레시 토큰을 삭제합니다. (전체 로그아웃) */
    fun deleteAllRefreshTokensByUserId(userId: String)
}

data class RefreshTokenSession(
    val userId: String,
    val sessionId: String
)

