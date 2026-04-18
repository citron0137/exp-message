package site.rahoon.message.monolithic.core.conversation.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.domain.Origin
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

@Service
class WidgetBootstrapQueryService(
    private val channelIntegrationRepository: ChannelIntegrationRepository,
    private val channelRepository: ChannelRepository,
) {
    /**
     * Resolves public widget bootstrap data from a public key and request origin.
     */
    @Transactional(readOnly = true)
    fun bootstrap(query: WidgetBootstrapQuery): WidgetBootstrapResult {
        val origin =
            Origin.parse(query.origin)
                ?: throw ConversationException(
                    error = ConversationError.INVALID_WIDGET_ORIGIN,
                    details = mapOf("origin" to query.origin),
                )
        val integration =
            channelIntegrationRepository.findByPublicKey(query.publicKey)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_INTEGRATION_NOT_FOUND,
                    details = mapOf("publicKey" to query.publicKey),
                )
        requireWidgetIntegration(integration)
        requireActiveIntegration(integration)
        requireAllowedOrigin(integration, origin)
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
        return WidgetBootstrapResult(
            channel =
                WidgetBootstrapChannelResult(
                    id = channel.id,
                    name = channel.name,
                ),
            integration =
                WidgetBootstrapIntegrationResult(
                    id = integration.id,
                    type = integration.type,
                    publicKey = integration.publicKey,
                ),
        )
    }

    /**
     * Requires the integration to be a widget integration.
     */
    private fun requireWidgetIntegration(integration: ChannelIntegration) {
        if (integration.type != ChannelIntegrationType.WIDGET) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_NOT_FOUND,
                details = mapOf("publicKey" to integration.publicKey),
            )
        }
    }

    /**
     * Requires the integration to be active.
     */
    private fun requireActiveIntegration(integration: ChannelIntegration) {
        if (!integration.isActive()) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_DISABLED,
                details = mapOf("integrationId" to integration.id),
            )
        }
    }

    /**
     * Requires the origin to be allowed by the integration.
     */
    private fun requireAllowedOrigin(
        integration: ChannelIntegration,
        origin: Origin,
    ) {
        if (!integration.allowedOrigins.allows(origin)) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_ORIGIN_DENIED,
                details = mapOf("integrationId" to integration.id, "origin" to origin.value),
            )
        }
    }
}

data class WidgetBootstrapQuery(
    val publicKey: String,
    val origin: String,
)

data class WidgetBootstrapResult(
    val channel: WidgetBootstrapChannelResult,
    val integration: WidgetBootstrapIntegrationResult,
)

data class WidgetBootstrapChannelResult(
    val id: String,
    val name: String,
)

data class WidgetBootstrapIntegrationResult(
    val id: String,
    val type: ChannelIntegrationType,
    val publicKey: String,
)
