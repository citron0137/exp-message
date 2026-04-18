package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class ChannelMembership(
    val id: String,
    val channelId: String,
    val userId: String,
    val role: ChannelMembershipRole,
    val agentStatus: AgentStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
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
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
