package site.rahoon.message.monolithic.core.iam.access.infrastructure.persistence.loginfailure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "iam_login_failures",
    indexes = [
        Index(name = "idx_iam_login_failures_email", columnList = "email"),
        Index(name = "idx_iam_login_failures_ip_address", columnList = "ip_address"),
    ],
)
class CoreLoginFailureEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "email", nullable = false, length = 255)
    var email: String,
    @Column(name = "ip_address", nullable = false, length = 64)
    var ipAddress: String,
    @Column(name = "failed_at", nullable = false)
    var failedAt: LocalDateTime,
) {
    constructor() : this("", "", "", LocalDateTime.now())
}
