package site.rahoon.message.monolithic.core.conversation.infrastructure.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.port.IntegrationSecretHasher

@Component
class BCryptIntegrationSecretHasher : IntegrationSecretHasher {
    private val encoder = BCryptPasswordEncoder()

    /**
     * Hashes a raw integration secret with BCrypt.
     */
    override fun hash(rawSecret: String): String = encoder.encode(rawSecret)
}
