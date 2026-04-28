package site.rahoon.message.monolithic.core.iam.access.application.port

interface AccessPasswordVerifier {
    /**
     * Verifies a raw password against a stored password hash.
     */
    fun verify(
        rawPassword: String,
        passwordHash: String,
    ): Boolean
}
