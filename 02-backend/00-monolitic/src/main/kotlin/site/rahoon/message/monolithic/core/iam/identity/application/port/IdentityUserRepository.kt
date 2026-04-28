package site.rahoon.message.monolithic.core.iam.identity.application.port

import site.rahoon.message.monolithic.core.iam.identity.domain.GlobalRole
import site.rahoon.message.monolithic.core.iam.identity.domain.IdentityUser

interface IdentityUserRepository {
    /**
     * Saves an identity user.
     */
    fun save(user: IdentityUser): IdentityUser

    /**
     * Finds an identity user by identifier.
     */
    fun findById(id: String): IdentityUser?

    /**
     * Finds an identity user by email.
     */
    fun findByEmail(email: String): IdentityUser?

    /**
     * Checks whether any identity user has the given global role.
     */
    fun existsByGlobalRole(globalRole: GlobalRole): Boolean

    /**
     * Lists all identity users.
     */
    fun findAll(): List<IdentityUser>
}
