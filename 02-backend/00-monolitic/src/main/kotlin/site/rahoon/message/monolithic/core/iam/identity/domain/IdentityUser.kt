package site.rahoon.message.monolithic.core.iam.identity.domain

import java.time.LocalDateTime
import java.util.UUID

data class IdentityUser(
    val id: String,
    val email: String,
    val passwordHash: String,
    val nickname: String,
    val globalRole: GlobalRole,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * Creates a new identity user with a generated identifier.
         */
        fun create(
            email: String,
            passwordHash: String,
            nickname: String,
            globalRole: GlobalRole,
        ): IdentityUser {
            val now = LocalDateTime.now()
            return IdentityUser(
                id = UUID.randomUUID().toString(),
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                globalRole = globalRole,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
