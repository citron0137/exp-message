package site.rahoon.message.monolithic.core.iam.access.domain

import java.time.LocalDateTime

data class CoreRefreshToken(
    val token: String,
    val userId: String,
    val sessionId: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    /**
     * Returns true when this refresh token is already expired.
     */
    fun isExpired(now: LocalDateTime): Boolean = !expiresAt.isAfter(now)
}
