package site.rahoon.message.monolithic.core.iam.identity.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.iam.identity.application.port.IdentityUserRepository
import site.rahoon.message.monolithic.core.iam.identity.application.port.PasswordHasher
import site.rahoon.message.monolithic.core.iam.identity.application.service.TemporaryPasswordGenerator
import site.rahoon.message.monolithic.core.iam.identity.domain.GlobalRole
import site.rahoon.message.monolithic.core.iam.identity.domain.IdentityUser

@Service
class IdentityFacade(
    private val identityUserRepository: IdentityUserRepository,
    private val passwordHasher: PasswordHasher,
    private val temporaryPasswordGenerator: TemporaryPasswordGenerator,
) {
    /**
     * Creates a platform admin if no platform admin exists.
     */
    @Transactional
    fun createPlatformAdminIfAbsent(command: CreatePlatformAdminIfAbsentCommand): PlatformAdminCreationResult {
        if (identityUserRepository.existsByGlobalRole(GlobalRole.PLATFORM_ADMIN)) {
            return PlatformAdminCreationResult(created = false, temporaryPassword = null)
        }

        val password = command.password.takeIf { it.isNotBlank() } ?: temporaryPasswordGenerator.generate()
        val user =
            IdentityUser.create(
                email = command.email,
                passwordHash = passwordHasher.hash(password),
                nickname = command.nickname,
                globalRole = GlobalRole.PLATFORM_ADMIN,
            )
        identityUserRepository.save(user)
        return PlatformAdminCreationResult(created = true, temporaryPassword = password.takeIf { command.password.isBlank() })
    }

    /**
     * Creates a channel admin or loads an existing channel user by email.
     */
    @Transactional
    fun createOrLoadCustomerAdmin(command: CreateOrLoadCustomerAdminCommand): CustomerAdminIdentityResult {
        val result =
            createOrLoadChannelUser(
                CreateOrLoadChannelUserCommand(
                    email = command.email,
                    nickname = command.nickname,
                ),
            )
        return CustomerAdminIdentityResult(
            userId = result.userId,
            email = result.email,
            nickname = result.nickname,
            temporaryPassword = result.temporaryPassword,
            created = result.created,
        )
    }

    /**
     * Creates a channel user or loads an existing user by email.
     */
    @Transactional
    fun createOrLoadChannelUser(command: CreateOrLoadChannelUserCommand): ChannelUserIdentityResult {
        val existing = identityUserRepository.findByEmail(command.email)
        if (existing != null) {
            return ChannelUserIdentityResult(
                userId = existing.id,
                email = existing.email,
                nickname = existing.nickname,
                temporaryPassword = null,
                created = false,
            )
        }

        val temporaryPassword = temporaryPasswordGenerator.generate()
        val user =
            IdentityUser.create(
                email = command.email,
                passwordHash = passwordHasher.hash(temporaryPassword),
                nickname = command.nickname,
                globalRole = GlobalRole.CHANNEL_USER,
            )
        val saved = identityUserRepository.save(user)
        return ChannelUserIdentityResult(
            userId = saved.id,
            email = saved.email,
            nickname = saved.nickname,
            temporaryPassword = temporaryPassword,
            created = true,
        )
    }
}

data class CreatePlatformAdminIfAbsentCommand(
    val email: String,
    val password: String,
    val nickname: String,
)

data class PlatformAdminCreationResult(
    val created: Boolean,
    val temporaryPassword: String?,
)

data class CreateOrLoadCustomerAdminCommand(
    val email: String,
    val nickname: String,
)

data class CreateOrLoadChannelUserCommand(
    val email: String,
    val nickname: String,
)

data class CustomerAdminIdentityResult(
    val userId: String,
    val email: String,
    val nickname: String,
    val temporaryPassword: String?,
    val created: Boolean,
)

data class ChannelUserIdentityResult(
    val userId: String,
    val email: String,
    val nickname: String,
    val temporaryPassword: String?,
    val created: Boolean,
)
