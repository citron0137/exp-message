package site.rahoon.message.monolithic.core.conversation.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelMembershipResult
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

@Service
class AdminChannelMembershipQueryService(
    private val channelAccessPolicy: ChannelAccessPolicy,
    private val channelMembershipRepository: ChannelMembershipRepository,
) {
    /**
     * Lists memberships for channel membership management.
     */
    @Transactional(readOnly = true)
    fun listByChannel(
        actor: AuthenticatedPrincipal,
        channelId: String,
        role: ChannelMembershipRole? = null,
        status: ChannelMembershipStatus? = null,
    ): List<ChannelMembershipResult> {
        channelAccessPolicy.requireChannelAdminWrite(actor, channelId)
        return channelMembershipRepository
            .findByChannelIdAndFilters(channelId = channelId, role = role, status = status)
            .map { ChannelMembershipResult.from(it) }
    }
}
