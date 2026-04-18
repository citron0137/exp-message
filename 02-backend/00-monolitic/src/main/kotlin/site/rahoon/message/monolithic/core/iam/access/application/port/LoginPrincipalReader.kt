package site.rahoon.message.monolithic.core.iam.access.application.port

import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole

interface LoginPrincipalReader {
    /**
     * Finds a login principal by email.
     */
    fun findByEmail(email: String): LoginPrincipalSnapshot?

    /**
     * Finds a login principal by user identifier.
     */
    fun findById(userId: String): LoginPrincipalSnapshot?
}

data class LoginPrincipalSnapshot(
    val userId: String,
    val email: String,
    val passwordHash: String,
    val globalRole: PrincipalGlobalRole,
)
