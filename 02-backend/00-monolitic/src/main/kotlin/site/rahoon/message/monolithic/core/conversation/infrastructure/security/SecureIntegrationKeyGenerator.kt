package site.rahoon.message.monolithic.core.conversation.infrastructure.security

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.port.IntegrationKeyGenerator
import java.security.SecureRandom
import java.util.Base64

@Component
class SecureIntegrationKeyGenerator : IntegrationKeyGenerator {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    /**
     * Generates a public widget key.
     */
    override fun generateWidgetPublicKey(): String = "wpk_${randomToken()}"

    /**
     * Generates a raw widget secret.
     */
    override fun generateWidgetSecret(): String = "wsk_${randomToken()}"

    /**
     * Generates a URL-safe random token.
     */
    private fun randomToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    companion object {
        private const val TOKEN_BYTES = 32
    }
}
