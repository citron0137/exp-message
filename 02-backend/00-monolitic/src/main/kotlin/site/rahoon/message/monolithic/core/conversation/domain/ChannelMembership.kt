package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class ChannelMembership(
    val id: String,
    val channelId: String,
    val userId: String,
    val role: ChannelMembershipRole,
    val agentStatus: AgentStatus,
    val status: ChannelMembershipStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    /**
     * Returns true when this membership can receive operational assignments.
     */
    fun canBeAssigned(): Boolean = status == ChannelMembershipStatus.ACTIVE

    /**
     * Returns a membership copy with the requested role.
     */
    fun changeRole(
        role: ChannelMembershipRole,
        now: LocalDateTime = LocalDateTime.now(),
    ): ChannelMembership = copy(role = role, updatedAt = now)

    /**
     * Returns an active membership copy.
     */
    fun enable(now: LocalDateTime = LocalDateTime.now()): ChannelMembership = copy(status = ChannelMembershipStatus.ACTIVE, updatedAt = now)

    /**
     * Returns a disabled membership copy.
     */
    fun disable(now: LocalDateTime = LocalDateTime.now()): ChannelMembership =
        copy(status = ChannelMembershipStatus.DISABLED, updatedAt = now)

    companion object {
        /**
         * Creates a channel admin membership.
         */
        fun createChannelAdmin(
            channelId: String,
            userId: String,
        ): ChannelMembership {
            val now = LocalDateTime.now()
            return ChannelMembership(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                userId = userId,
                role = ChannelMembershipRole.CHANNEL_ADMIN,
                agentStatus = AgentStatus.OFFLINE,
                status = ChannelMembershipStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }

        /**
         * Creates a channel membership for the requested role.
         */
        fun create(
            channelId: String,
            userId: String,
            role: ChannelMembershipRole,
        ): ChannelMembership {
            val now = LocalDateTime.now()
            return ChannelMembership(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                userId = userId,
                role = role,
                agentStatus = AgentStatus.OFFLINE,
                status = ChannelMembershipStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
