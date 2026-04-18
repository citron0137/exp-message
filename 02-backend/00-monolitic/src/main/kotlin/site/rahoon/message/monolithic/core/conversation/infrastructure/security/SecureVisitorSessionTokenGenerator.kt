package site.rahoon.message.monolithic.core.conversation.infrastructure.security

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenGenerator
import java.security.SecureRandom
import java.util.Base64

@Component
class SecureVisitorSessionTokenGenerator : VisitorSessionTokenGenerator {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    /**
     * Generates a raw visitor session token.
     */
    override fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return "wvs_${encoder.encodeToString(bytes)}"
    }

    companion object {
        private const val TOKEN_BYTES = 32
    }
}
