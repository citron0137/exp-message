package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.channelconversation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "cv_channel_conversations",
    indexes = [
        Index(name = "idx_cv_channel_conversations_channel_id", columnList = "channel_id"),
        Index(name = "idx_cv_channel_conversations_visitor_id", columnList = "visitor_id"),
        Index(name = "idx_cv_channel_conversations_status", columnList = "status"),
        Index(name = "idx_cv_channel_conversations_channel_status", columnList = "channel_id,status"),
    ],
)
class ChannelConversationEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "visitor_id", nullable = false, length = 36)
    var visitorId: String,
    @Column(name = "status", nullable = false, length = 40)
    var status: String,
    @Column(name = "assignee_membership_id", length = 36)
    var assigneeMembershipId: String?,
    @Column(name = "last_message_sequence", nullable = false)
    var lastMessageSequence: Long,
    @Column(name = "last_message_at")
    var lastMessageAt: LocalDateTime?,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
    @Column(name = "closed_at")
    var closedAt: LocalDateTime?,
) {
    constructor() : this("", "", "", "", null, 0, null, LocalDateTime.now(), LocalDateTime.now(), null)
}
