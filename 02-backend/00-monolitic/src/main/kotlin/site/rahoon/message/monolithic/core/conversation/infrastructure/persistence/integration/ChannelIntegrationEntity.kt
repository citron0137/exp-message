package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.integration

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "cv_channel_integrations",
    indexes = [
        Index(name = "idx_cv_channel_integrations_channel_id", columnList = "channel_id"),
        Index(name = "idx_cv_channel_integrations_channel_type_status", columnList = "channel_id,type,status"),
        Index(name = "idx_cv_channel_integrations_status", columnList = "status"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cv_channel_integrations_public_key", columnNames = ["public_key"]),
    ],
)
class ChannelIntegrationEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "type", nullable = false, length = 40)
    var type: String,
    @Column(name = "public_key", nullable = false, length = 100)
    var publicKey: String,
    @Column(name = "secret_hash", nullable = false, length = 255)
    var secretHash: String,
    @Column(name = "status", nullable = false, length = 40)
    var status: String,
    @Column(name = "allowed_origins", nullable = false, columnDefinition = "JSON")
    var allowedOrigins: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) {
    constructor() : this("", "", "", "", "", "", "[]", LocalDateTime.now(), LocalDateTime.now())
}
