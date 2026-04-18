package site.rahoon.message.monolithic.core.conversation.application.port

interface IntegrationSecretHasher {
    /**
     * Hashes a raw integration secret.
     */
    fun hash(rawSecret: String): String
}
