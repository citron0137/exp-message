package site.rahoon.message.monolithic.core.conversation.application.service

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.domain.Origin
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

@Service
class WidgetAccessPolicy(
    private val channelIntegrationRepository: ChannelIntegrationRepository,
    private val channelRepository: ChannelRepository,
) {
    /**
     * Resolves and verifies widget access from public key and origin.
     */
    fun requireAccessibleWidget(
        publicKey: String,
        rawOrigin: String,
    ): WidgetAccess {
        val origin =
            Origin.parse(rawOrigin)
                ?: throw ConversationException(
                    error = ConversationError.INVALID_WIDGET_ORIGIN,
                    details = mapOf("origin" to rawOrigin),
                )
        val integration =
            channelIntegrationRepository.findByPublicKey(publicKey)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_INTEGRATION_NOT_FOUND,
                    details = mapOf("publicKey" to publicKey),
                )
        if (integration.type != ChannelIntegrationType.WIDGET) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_NOT_FOUND,
                details = mapOf("publicKey" to publicKey),
            )
        }
        if (!integration.isActive()) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_DISABLED,
                details = mapOf("integrationId" to integration.id),
            )
        }
        if (!integration.allowedOrigins.allows(origin)) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_ORIGIN_DENIED,
                details = mapOf("integrationId" to integration.id, "origin" to origin.value),
            )
        }
        val channel =
            channelRepository.findById(integration.channelId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_NOT_FOUND,
                    details = mapOf("channelId" to integration.channelId),
                )
        if (channel.status != ChannelStatus.ACTIVE) {
            throw ConversationException(
                error = ConversationError.CHANNEL_NOT_ACTIVE,
                details = mapOf("channelId" to channel.id, "status" to channel.status.name),
            )
        }
        return WidgetAccess(channel = channel, integration = integration, origin = origin)
    }
}

data class WidgetAccess(
    val channel: Channel,
    val integration: ChannelIntegration,
    val origin: Origin,
)
