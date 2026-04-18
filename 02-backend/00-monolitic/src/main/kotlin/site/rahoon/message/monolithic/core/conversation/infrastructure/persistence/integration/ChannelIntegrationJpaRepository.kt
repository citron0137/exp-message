package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.integration

import org.springframework.data.jpa.repository.JpaRepository

interface ChannelIntegrationJpaRepository : JpaRepository<ChannelIntegrationEntity, String> {
    /**
     * Finds an integration by public key.
     */
    fun findByPublicKey(publicKey: String): ChannelIntegrationEntity?

    /**
     * Lists integrations by channel identifier.
     */
    fun findByChannelId(channelId: String): List<ChannelIntegrationEntity>

    /**
     * Returns true when a channel has an integration matching type and status.
     */
    fun existsByChannelIdAndTypeAndStatus(
        channelId: String,
        type: String,
        status: String,
    ): Boolean

    /**
     * Returns true when another integration matches type and status.
     */
    fun existsByChannelIdAndTypeAndStatusAndIdNot(
        channelId: String,
        type: String,
        status: String,
        id: String,
    ): Boolean
}
