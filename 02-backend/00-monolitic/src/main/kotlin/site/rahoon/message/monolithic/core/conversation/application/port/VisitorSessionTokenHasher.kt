package site.rahoon.message.monolithic.core.conversation.application.port

interface VisitorSessionTokenHasher {
    /**
     * Hashes a raw visitor session token deterministically.
     */
    fun hash(rawToken: String): String
}
