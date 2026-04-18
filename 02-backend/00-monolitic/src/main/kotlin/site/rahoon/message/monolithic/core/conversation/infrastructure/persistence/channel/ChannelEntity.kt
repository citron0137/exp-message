package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.channel

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "cv_channels",
    indexes = [
        Index(name = "idx_cv_channels_status", columnList = "status"),
    ],
)
class ChannelEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "status", nullable = false, length = 40)
    var status: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) {
    constructor() : this("", "", "", LocalDateTime.now(), LocalDateTime.now())
}
