package site.rahoon.message.__monolitic.authtoken.domain

import java.time.LocalDateTime

/**
 * 인증 토큰 도메인 엔티티
 * JWT 토큰의 정보를 담는 객체
 */
data class AuthToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * 새로운 인증 토큰을 생성합니다.
         */
        fun create(
            accessToken: String,
            refreshToken: String? = null,
            expiresAt: LocalDateTime
        ): AuthToken {
            return AuthToken(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt,
                createdAt = LocalDateTime.now()
            )
        }
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }
}

