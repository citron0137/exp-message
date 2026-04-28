package site.rahoon.message.monolithic.core.conversation.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelIntegrationResult
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

@Service
class AdminChannelIntegrationQueryService(
    private val channelIntegrationRepository: ChannelIntegrationRepository,
    private val channelAccessPolicy: ChannelAccessPolicy,
) {
    /**
     * Lists channel integrations for a platform admin.
     */
    @Transactional(readOnly = true)
    fun listByChannel(
        actor: AuthenticatedPrincipal,
        channelId: String,
    ): List<ChannelIntegrationResult> {
        channelAccessPolicy.requirePlatformAdmin(actor)
        return channelIntegrationRepository.findByChannelId(channelId).map { ChannelIntegrationResult.from(it) }
    }
}
