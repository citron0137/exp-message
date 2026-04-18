package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.membership

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "cv_channel_memberships",
    indexes = [
        Index(name = "idx_cv_channel_memberships_channel_id", columnList = "channel_id"),
        Index(name = "idx_cv_channel_memberships_user_id", columnList = "user_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cv_channel_memberships_channel_user", columnNames = ["channel_id", "user_id"]),
    ],
)
class ChannelMembershipEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String,
    @Column(name = "role", nullable = false, length = 40)
    var role: String,
    @Column(name = "agent_status", nullable = false, length = 40)
    var agentStatus: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) {
    constructor() : this("", "", "", "", "", LocalDateTime.now(), LocalDateTime.now())
}
