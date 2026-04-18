package site.rahoon.message.monolithic.core.conversation.application.service

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

@Service
class ChannelAccessPolicy(
    private val channelMembershipRepository: ChannelMembershipRepository,
) {
    /**
     * Requires the principal to be a platform admin.
     */
    fun requirePlatformAdmin(principal: AuthenticatedPrincipal) {
        if (!principal.isPlatformAdmin()) {
            throw ConversationException(ConversationError.PLATFORM_ADMIN_REQUIRED)
        }
    }

    /**
     * Requires the principal to be a platform admin or a member of the channel.
     */
    fun requireChannelRead(
        principal: AuthenticatedPrincipal,
        channelId: String,
    ) {
        if (principal.isPlatformAdmin()) {
            return
        }
        val membership = channelMembershipRepository.findByChannelIdAndUserId(channelId, principal.userId)
        if (membership == null) {
            throw ConversationException(
                error = ConversationError.CHANNEL_ACCESS_DENIED,
                details = mapOf("channelId" to channelId, "userId" to principal.userId),
            )
        }
    }

    /**
     * Requires the principal to be a platform admin or a channel admin member.
     */
    fun requireChannelAdminWrite(
        principal: AuthenticatedPrincipal,
        channelId: String,
    ) {
        if (principal.isPlatformAdmin()) {
            return
        }
        requireChannelAdminMembership(principal, channelId)
    }

    /**
     * Requires the principal to be a channel admin member and returns that membership.
     */
    fun requireChannelAdminMembership(
        principal: AuthenticatedPrincipal,
        channelId: String,
    ): ChannelMembership {
        val membership = channelMembershipRepository.findByChannelIdAndUserId(channelId, principal.userId)
        if (membership?.role != ChannelMembershipRole.CHANNEL_ADMIN) {
            throw ConversationException(
                error = ConversationError.CHANNEL_ADMIN_REQUIRED,
                details = mapOf("channelId" to channelId, "userId" to principal.userId),
            )
        }
        return membership
    }
}
