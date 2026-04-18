package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.visitorsession

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "cv_visitor_sessions",
    indexes = [
        Index(name = "idx_cv_visitor_sessions_visitor_id", columnList = "visitor_id"),
        Index(name = "idx_cv_visitor_sessions_channel_id", columnList = "channel_id"),
        Index(name = "idx_cv_visitor_sessions_expires_at", columnList = "expires_at"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cv_visitor_sessions_token_hash", columnNames = ["token_hash"]),
    ],
)
class VisitorSessionEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "visitor_id", nullable = false, length = 36)
    var visitorId: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: LocalDateTime,
) {
    constructor() : this("", "", "", "", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now())
}
