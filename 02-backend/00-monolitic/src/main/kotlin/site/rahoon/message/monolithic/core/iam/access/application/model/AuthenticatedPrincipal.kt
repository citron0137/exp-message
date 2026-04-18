package site.rahoon.message.monolithic.core.iam.access.application.model

import java.time.LocalDateTime

data class AuthenticatedPrincipal(
    val userId: String,
    val sessionId: String,
    val globalRole: PrincipalGlobalRole,
    val expiresAt: LocalDateTime,
) {
    /**
     * Returns true when this principal represents a platform admin.
     */
    fun isPlatformAdmin(): Boolean = globalRole == PrincipalGlobalRole.PLATFORM_ADMIN
}

enum class PrincipalGlobalRole {
    PLATFORM_ADMIN,
    CHANNEL_USER,
}
