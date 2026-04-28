package site.rahoon.message.monolithic.core.iam.access.infrastructure.identity

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import site.rahoon.message.monolithic.core.iam.access.application.port.LoginPrincipalReader
import site.rahoon.message.monolithic.core.iam.access.application.port.LoginPrincipalSnapshot
import site.rahoon.message.monolithic.core.iam.identity.infrastructure.persistence.user.IdentityUserEntity
import site.rahoon.message.monolithic.core.iam.identity.infrastructure.persistence.user.IdentityUserJpaRepository

@Component
class IdentityLoginPrincipalAdapter(
    private val jpaRepository: IdentityUserJpaRepository,
) : LoginPrincipalReader {
    /**
     * Finds a login principal by email from the identity table.
     */
    override fun findByEmail(email: String): LoginPrincipalSnapshot? = jpaRepository.findByEmail(email)?.toSnapshot()

    /**
     * Finds a login principal by user identifier from the identity table.
     */
    override fun findById(userId: String): LoginPrincipalSnapshot? = jpaRepository.findById(userId).orElse(null)?.toSnapshot()

    /**
     * Maps an identity user entity to an access login snapshot.
     */
    private fun IdentityUserEntity.toSnapshot(): LoginPrincipalSnapshot =
        LoginPrincipalSnapshot(
            userId = id,
            email = email,
            passwordHash = passwordHash,
            globalRole = PrincipalGlobalRole.valueOf(globalRole),
        )
}
