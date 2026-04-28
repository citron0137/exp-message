package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType

interface ChannelIntegrationRepository {
    /**
     * Saves a channel integration.
     */
    fun save(integration: ChannelIntegration): ChannelIntegration

    /**
     * Finds a channel integration by identifier.
     */
    fun findById(id: String): ChannelIntegration?

    /**
     * Finds a channel integration by public key.
     */
    fun findByPublicKey(publicKey: String): ChannelIntegration?

    /**
     * Lists channel integrations for a channel.
     */
    fun findByChannelId(channelId: String): List<ChannelIntegration>

    /**
     * Returns true when a channel has an integration matching type and status.
     */
    fun existsByChannelIdAndTypeAndStatus(
        channelId: String,
        type: ChannelIntegrationType,
        status: ChannelIntegrationStatus,
    ): Boolean

    /**
     * Returns true when another channel integration matches type and status.
     */
    fun existsByChannelIdAndTypeAndStatusAndIdNot(
        channelId: String,
        type: ChannelIntegrationType,
        status: ChannelIntegrationStatus,
        excludedId: String,
    ): Boolean
}
