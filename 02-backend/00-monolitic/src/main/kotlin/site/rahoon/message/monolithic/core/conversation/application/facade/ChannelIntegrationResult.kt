package site.rahoon.message.monolithic.core.conversation.application.facade

import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType
import java.time.LocalDateTime

data class ChannelIntegrationResult(
    val id: String,
    val channelId: String,
    val type: ChannelIntegrationType,
    val publicKey: String,
    val status: ChannelIntegrationStatus,
    val allowedOrigins: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * Maps a channel integration domain object to an application result.
         */
        fun from(integration: ChannelIntegration): ChannelIntegrationResult =
            ChannelIntegrationResult(
                id = integration.id,
                channelId = integration.channelId,
                type = integration.type,
                publicKey = integration.publicKey,
                status = integration.status,
                allowedOrigins = integration.allowedOrigins.values,
                createdAt = integration.createdAt,
                updatedAt = integration.updatedAt,
            )
    }
}
