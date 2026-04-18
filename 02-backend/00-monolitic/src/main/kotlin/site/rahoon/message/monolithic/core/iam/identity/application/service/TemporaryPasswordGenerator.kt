package site.rahoon.message.monolithic.core.iam.identity.application.service

import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class TemporaryPasswordGenerator {
    private val random = SecureRandom()

    /**
     * Generates a temporary password for one-time delivery.
     */
    fun generate(): String =
        (1..PASSWORD_LENGTH)
            .map { PASSWORD_CHARS[random.nextInt(PASSWORD_CHARS.length)] }
            .joinToString("")

    companion object {
        private const val PASSWORD_LENGTH = 16
        private const val PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*"
    }
}
