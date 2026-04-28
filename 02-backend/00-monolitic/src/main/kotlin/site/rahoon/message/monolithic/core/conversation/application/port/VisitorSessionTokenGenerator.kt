package site.rahoon.message.monolithic.core.conversation.application.port

interface VisitorSessionTokenGenerator {
    /**
     * Generates a raw visitor session token.
     */
    fun generate(): String
}
