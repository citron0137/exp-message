package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class ChannelIntegration(
    val id: String,
    val channelId: String,
    val type: ChannelIntegrationType,
    val publicKey: String,
    val secretHash: String,
    val status: ChannelIntegrationStatus,
    val allowedOrigins: AllowedOrigins,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    /**
     * Returns a copy with the integration enabled.
     */
    fun enable(): ChannelIntegration = copy(status = ChannelIntegrationStatus.ACTIVE, updatedAt = LocalDateTime.now())

    /**
     * Returns a copy with the integration disabled.
     */
    fun disable(): ChannelIntegration = copy(status = ChannelIntegrationStatus.DISABLED, updatedAt = LocalDateTime.now())

    /**
     * Returns a copy with replaced allowed origins.
     */
    fun updateAllowedOrigins(allowedOrigins: AllowedOrigins): ChannelIntegration =
        copy(allowedOrigins = allowedOrigins, updatedAt = LocalDateTime.now())

    /**
     * Returns true when this integration can currently be used.
     */
    fun isActive(): Boolean = status == ChannelIntegrationStatus.ACTIVE

    companion object {
        /**
         * Creates a new active widget integration.
         */
        fun createWidget(
            channelId: String,
            publicKey: String,
            secretHash: String,
            allowedOrigins: AllowedOrigins,
        ): ChannelIntegration {
            val now = LocalDateTime.now()
            return ChannelIntegration(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                type = ChannelIntegrationType.WIDGET,
                publicKey = publicKey,
                secretHash = secretHash,
                status = ChannelIntegrationStatus.ACTIVE,
                allowedOrigins = allowedOrigins,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
