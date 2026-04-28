package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class VisitorSession(
    val id: String,
    val visitorId: String,
    val channelId: String,
    val tokenHash: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val lastSeenAt: LocalDateTime,
) {
    /**
     * Returns true when this session is already expired.
     */
    fun isExpired(now: LocalDateTime): Boolean = !expiresAt.isAfter(now)

    /**
     * Returns a copy with an updated last-seen timestamp.
     */
    fun touch(now: LocalDateTime = LocalDateTime.now()): VisitorSession = copy(lastSeenAt = now)

    companion object {
        /**
         * Creates a visitor session with a hashed token.
         */
        fun create(
            visitorId: String,
            channelId: String,
            tokenHash: String,
            expiresAt: LocalDateTime,
        ): VisitorSession {
            val now = LocalDateTime.now()
            return VisitorSession(
                id = UUID.randomUUID().toString(),
                visitorId = visitorId,
                channelId = channelId,
                tokenHash = tokenHash,
                expiresAt = expiresAt,
                createdAt = now,
                lastSeenAt = now,
            )
        }
    }
}
