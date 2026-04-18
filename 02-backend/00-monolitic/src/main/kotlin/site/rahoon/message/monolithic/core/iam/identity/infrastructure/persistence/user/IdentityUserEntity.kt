package site.rahoon.message.monolithic.core.iam.identity.infrastructure.persistence.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "iam_users",
    indexes = [
        Index(name = "uk_iam_users_email", columnList = "email", unique = true),
        Index(name = "idx_iam_users_global_role", columnList = "global_role"),
    ],
)
class IdentityUserEntity(
    @Id
    @Column(name = "id", length = 36)
    var id: String,
    @Column(name = "email", nullable = false, length = 255)
    var email: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,
    @Column(name = "nickname", nullable = false, length = 100)
    var nickname: String,
    @Column(name = "global_role", nullable = false, length = 40)
    var globalRole: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) {
    constructor() : this("", "", "", "", "", LocalDateTime.now(), LocalDateTime.now())
}
