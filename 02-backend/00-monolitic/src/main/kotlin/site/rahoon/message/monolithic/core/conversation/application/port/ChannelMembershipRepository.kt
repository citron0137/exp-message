package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus

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
     * Finds channel memberships by optional role and status filters.
     */
    fun findByChannelIdAndFilters(
        channelId: String,
        role: ChannelMembershipRole?,
        status: ChannelMembershipStatus?,
    ): List<ChannelMembership>

    /**
     * Finds a channel membership by channel and user.
     */
    fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelMembership?

    /**
     * Returns true when a channel membership already exists for a user.
     */
    fun existsByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): Boolean

    /**
     * Counts memberships by channel, role, and status.
     */
    fun countByChannelIdAndRoleAndStatus(
        channelId: String,
        role: ChannelMembershipRole,
        status: ChannelMembershipStatus,
    ): Long
}
