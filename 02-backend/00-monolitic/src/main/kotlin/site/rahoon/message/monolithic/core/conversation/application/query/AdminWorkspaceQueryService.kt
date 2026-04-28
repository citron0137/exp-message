package site.rahoon.message.monolithic.core.conversation.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelMembershipResult
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelResult
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

@Service
class AdminWorkspaceQueryService(
    private val channelRepository: ChannelRepository,
    private val channelMembershipRepository: ChannelMembershipRepository,
) {
    /**
     * Lists channels the current admin can enter as an operational workspace.
     */
    @Transactional(readOnly = true)
    fun listMyChannels(actor: AuthenticatedPrincipal): List<AdminWorkspaceChannelResult> {
        if (actor.isPlatformAdmin()) {
            return channelRepository
                .findAll()
                .map { AdminWorkspaceChannelResult(channel = ChannelResult.from(it), membership = null) }
        }
        return channelMembershipRepository
            .findByUserId(actor.userId)
            .map { membership ->
                val channel =
                    channelRepository.findById(membership.channelId)
                        ?: throw ConversationException(
                            error = ConversationError.CHANNEL_NOT_FOUND,
                            details = mapOf("channelId" to membership.channelId),
                        )
                AdminWorkspaceChannelResult.from(channel, membership)
            }
    }
}

data class AdminWorkspaceChannelResult(
    val channel: ChannelResult,
    val membership: ChannelMembershipResult?,
) {
    companion object {
        /**
         * Maps channel and membership domain objects to a workspace channel result.
         */
        fun from(
            channel: Channel,
            membership: ChannelMembership,
        ): AdminWorkspaceChannelResult =
            AdminWorkspaceChannelResult(
                channel = ChannelResult.from(channel),
                membership = ChannelMembershipResult.from(membership),
            )
    }
}
