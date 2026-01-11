package site.rahoon.message.__monolitic.authtoken.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_refresh_tokens_session_id", columnList = "session_id")
    ]
)
class RefreshTokenEntity(
    @Id
    @Column(name = "refresh_token", length = 512)
    var refreshToken: String,

    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String,

    @Column(name = "session_id", nullable = false, length = 64)
    var sessionId: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime
) {
    constructor() : this("", "", "", LocalDateTime.now(), LocalDateTime.now())
}

