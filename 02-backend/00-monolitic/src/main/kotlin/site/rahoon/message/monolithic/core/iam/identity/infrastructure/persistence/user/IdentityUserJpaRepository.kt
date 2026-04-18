package site.rahoon.message.monolithic.core.iam.identity.infrastructure.persistence.user

import org.springframework.data.jpa.repository.JpaRepository

interface IdentityUserJpaRepository : JpaRepository<IdentityUserEntity, String> {
    /**
     * Finds an identity user entity by email.
     */
    fun findByEmail(email: String): IdentityUserEntity?

    /**
     * Checks whether an identity user exists by global role.
     */
    fun existsByGlobalRole(globalRole: String): Boolean
}
