package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.conversationquery

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "cv_channel_conversations")
class AdminConversationQueryEntity(
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
