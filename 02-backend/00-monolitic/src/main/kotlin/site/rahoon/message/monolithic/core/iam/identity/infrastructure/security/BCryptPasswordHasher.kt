package site.rahoon.message.monolithic.core.iam.identity.infrastructure.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.iam.identity.application.port.PasswordHasher

@Component
class BCryptPasswordHasher : PasswordHasher {
    private val encoder = BCryptPasswordEncoder()

    /**
     * Hashes a raw password with BCrypt.
     */
    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    /**
     * Verifies a raw password against a BCrypt hash.
     */
    override fun verify(
        rawPassword: String,
        passwordHash: String,
    ): Boolean = encoder.matches(rawPassword, passwordHash)
}
