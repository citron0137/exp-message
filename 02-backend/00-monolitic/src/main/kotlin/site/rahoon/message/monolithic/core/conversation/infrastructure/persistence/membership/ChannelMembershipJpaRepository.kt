package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.membership

import org.springframework.data.jpa.repository.JpaRepository

interface ChannelMembershipJpaRepository : JpaRepository<ChannelMembershipEntity, String> {
    /**
     * Finds membership entities by channel identifier.
     */
    fun findByChannelId(channelId: String): List<ChannelMembershipEntity>

    /**
     * Finds a membership entity by channel and user.
     */
    fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelMembershipEntity?
}
