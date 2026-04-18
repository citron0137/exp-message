package site.rahoon.message.monolithic.core.iam.identity.application.port

interface PasswordHasher {
    /**
     * Hashes a raw password for storage.
     */
    fun hash(rawPassword: String): String

    /**
     * Verifies a raw password against a stored hash.
     */
    fun verify(
        rawPassword: String,
        passwordHash: String,
    ): Boolean
}
