package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.conversationmessage

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "cv_conversation_messages",
    indexes = [
        Index(name = "idx_cv_conversation_messages_channel_id", columnList = "channel_id"),
        Index(name = "idx_cv_conversation_messages_conversation_sequence", columnList = "conversation_id,sequence"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cv_conversation_messages_sequence", columnNames = ["conversation_id", "sequence"]),
        UniqueConstraint(
            name = "uk_cv_conversation_messages_idempotency",
            columnNames = ["conversation_id", "sender_type", "sender_id", "client_message_id"],
        ),
    ],
)
class ConversationMessageEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "conversation_id", nullable = false, length = 36)
    var conversationId: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "sequence", nullable = false)
    var sequence: Long,
    @Column(name = "sender_type", nullable = false, length = 40)
    var senderType: String,
    @Column(name = "sender_id", nullable = false, length = 36)
    var senderId: String,
    @Column(name = "client_message_id", nullable = false, length = 100)
    var clientMessageId: String,
    @Column(name = "type", nullable = false, length = 40)
    var type: String,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(name = "status", nullable = false, length = 40)
    var status: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
) {
    constructor() : this("", "", "", 0, "", "", "", "", "", "", LocalDateTime.now())
}
