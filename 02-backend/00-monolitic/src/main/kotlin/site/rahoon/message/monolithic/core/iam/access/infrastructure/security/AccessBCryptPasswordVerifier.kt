package site.rahoon.message.monolithic.core.iam.access.infrastructure.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.iam.access.application.port.AccessPasswordVerifier

@Component
class AccessBCryptPasswordVerifier : AccessPasswordVerifier {
    private val encoder = BCryptPasswordEncoder()

    /**
     * Verifies a raw password against a BCrypt hash.
     */
    override fun verify(
        rawPassword: String,
        passwordHash: String,
    ): Boolean = encoder.matches(rawPassword, passwordHash)
}
