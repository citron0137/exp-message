package site.rahoon.message.__monolitic.authtoken.domain

import java.time.LocalDateTime

/** JWT 기반 stateless access token */
data class AccessToken(
    val token: String,
    val expiresAt: LocalDateTime,
    val userId: String,
    val sessionId: String
) {
    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return now.isAfter(expiresAt)
    }
}

/** DB 저장 stateful refresh token */
data class RefreshToken(
    val token: String,
    val expiresAt: LocalDateTime,
    val userId: String,
    val sessionId: String,
    val createdAt: LocalDateTime
) {
    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return now.isAfter(expiresAt)
    }
}

/** AccessToken과 RefreshToken을 조합한 인증 결과 */
data class AuthToken(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken?
) {
    companion object {
        fun create(
            accessToken: AccessToken,
            refreshToken: RefreshToken? = null
        ): AuthToken {
            return AuthToken(
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        }
    }
}

