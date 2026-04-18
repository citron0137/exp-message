package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership

interface ChannelMembershipRepository {
    /**
     * Saves a channel membership.
     */
    fun save(membership: ChannelMembership): ChannelMembership

    /**
     * Finds a channel membership by identifier.
     */
    fun findById(id: String): ChannelMembership?

    /**
     * Finds channel memberships by channel identifier.
     */
    fun findByChannelId(channelId: String): List<ChannelMembership>

    /**
     * Finds a channel membership by channel and user.
     */
    fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelMembership?
}
