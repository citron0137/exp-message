package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.visitor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "cv_visitors",
    indexes = [
        Index(name = "idx_cv_visitors_channel_id", columnList = "channel_id"),
        Index(name = "idx_cv_visitors_external_id", columnList = "external_id"),
    ],
)
class VisitorEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "external_id", length = 100)
    var externalId: String?,
    @Column(name = "display_name", length = 100)
    var displayName: String?,
    @Column(name = "email", length = 255)
    var email: String?,
    @Column(name = "metadata", nullable = false, columnDefinition = "JSON")
    var metadata: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) {
    constructor() : this("", "", null, null, null, "{}", LocalDateTime.now(), LocalDateTime.now())
}
