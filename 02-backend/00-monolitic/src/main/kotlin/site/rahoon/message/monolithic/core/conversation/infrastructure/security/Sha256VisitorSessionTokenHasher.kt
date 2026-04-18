package site.rahoon.message.monolithic.core.conversation.infrastructure.security

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenHasher
import java.security.MessageDigest

@Component
class Sha256VisitorSessionTokenHasher : VisitorSessionTokenHasher {
    /**
     * Hashes a raw visitor session token with SHA-256 for deterministic lookup.
     */
    override fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
