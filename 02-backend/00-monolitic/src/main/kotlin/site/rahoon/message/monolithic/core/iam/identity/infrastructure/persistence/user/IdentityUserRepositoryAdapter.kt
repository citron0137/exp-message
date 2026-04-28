package site.rahoon.message.monolithic.core.iam.identity.infrastructure.persistence.user

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.iam.exception.IdentityError
import site.rahoon.message.monolithic.core.iam.exception.IdentityException
import site.rahoon.message.monolithic.core.iam.identity.application.port.IdentityUserRepository
import site.rahoon.message.monolithic.core.iam.identity.domain.GlobalRole
import site.rahoon.message.monolithic.core.iam.identity.domain.IdentityUser

@Repository
class IdentityUserRepositoryAdapter(
    private val jpaRepository: IdentityUserJpaRepository,
) : IdentityUserRepository {
    /**
     * Saves an identity user through JPA.
     */
    override fun save(user: IdentityUser): IdentityUser = jpaRepository.save(user.toEntity()).toDomain()

    /**
     * Finds an identity user by identifier through JPA.
     */
    override fun findById(id: String): IdentityUser? = jpaRepository.findById(id).orElse(null)?.toDomain()

    /**
     * Finds an identity user by email through JPA.
     */
    override fun findByEmail(email: String): IdentityUser? = jpaRepository.findByEmail(email)?.toDomain()

    /**
     * Checks whether an identity user exists with the given global role.
     */
    override fun existsByGlobalRole(globalRole: GlobalRole): Boolean = jpaRepository.existsByGlobalRole(globalRole.name)

    /**
     * Lists all identity users through JPA.
     */
    override fun findAll(): List<IdentityUser> = jpaRepository.findAll().map { it.toDomain() }

    /**
     * Maps an identity user domain object to a JPA entity.
     */
    private fun IdentityUser.toEntity(): IdentityUserEntity =
        IdentityUserEntity(
            id = id,
            email = email,
            passwordHash = passwordHash,
            nickname = nickname,
            globalRole = globalRole.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    /**
     * Maps an identity user JPA entity to a domain object.
     */
    private fun IdentityUserEntity.toDomain(): IdentityUser =
        IdentityUser(
            id = id,
            email = email,
            passwordHash = passwordHash,
            nickname = nickname,
            globalRole =
                runCatching { GlobalRole.valueOf(globalRole) }
                    .getOrElse {
                        throw IdentityException(
                            error = IdentityError.INVALID_ROLE,
                            details = mapOf("globalRole" to globalRole),
                            cause = it,
                        )
                    },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
