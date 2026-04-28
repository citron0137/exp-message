package site.rahoon.message.monolithic.core.conversation.application.port

interface IntegrationKeyGenerator {
    /**
     * Generates a public widget key.
     */
    fun generateWidgetPublicKey(): String

    /**
     * Generates a raw widget secret that is returned only once.
     */
    fun generateWidgetSecret(): String
}
