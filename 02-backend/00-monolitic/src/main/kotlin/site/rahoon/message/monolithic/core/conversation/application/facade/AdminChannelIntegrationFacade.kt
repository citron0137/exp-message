package site.rahoon.message.monolithic.core.conversation.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.port.IntegrationKeyGenerator
import site.rahoon.message.monolithic.core.conversation.application.port.IntegrationSecretHasher
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

@Service
class AdminChannelIntegrationFacade(
    private val channelRepository: ChannelRepository,
    private val channelIntegrationRepository: ChannelIntegrationRepository,
    private val integrationKeyGenerator: IntegrationKeyGenerator,
    private val integrationSecretHasher: IntegrationSecretHasher,
    private val channelAccessPolicy: ChannelAccessPolicy,
) {
    /**
     * Creates an active widget integration for a channel.
     */
    @Transactional
    fun createWidgetIntegration(command: CreateWidgetIntegrationCommand): WidgetIntegrationCreationResult {
        channelAccessPolicy.requirePlatformAdmin(command.actor)
        requireActiveChannel(command.channelId)
        requireNoActiveIntegration(command.channelId, ChannelIntegrationType.WIDGET)

        val rawSecret = integrationKeyGenerator.generateWidgetSecret()
        val integration =
            ChannelIntegration.createWidget(
                channelId = command.channelId,
                publicKey = integrationKeyGenerator.generateWidgetPublicKey(),
                secretHash = integrationSecretHasher.hash(rawSecret),
                allowedOrigins = AllowedOrigins.of(command.allowedOrigins),
            )
        val saved = channelIntegrationRepository.save(integration)
        return WidgetIntegrationCreationResult(
            integration = ChannelIntegrationResult.from(saved),
            secret = rawSecret,
        )
    }

    /**
     * Enables a channel integration.
     */
    @Transactional
    fun enableIntegration(command: ChangeChannelIntegrationStatusCommand): ChannelIntegrationResult {
        channelAccessPolicy.requirePlatformAdmin(command.actor)
        requireActiveChannel(command.channelId)
        val integration = findOwnedIntegration(command.channelId, command.integrationId)
        requireNoOtherActiveIntegration(integration)
        return ChannelIntegrationResult.from(channelIntegrationRepository.save(integration.enable()))
    }

    /**
     * Disables a channel integration.
     */
    @Transactional
    fun disableIntegration(command: ChangeChannelIntegrationStatusCommand): ChannelIntegrationResult {
        channelAccessPolicy.requirePlatformAdmin(command.actor)
        val integration = findOwnedIntegration(command.channelId, command.integrationId)
        return ChannelIntegrationResult.from(channelIntegrationRepository.save(integration.disable()))
    }

    /**
     * Replaces allowed origins for a channel integration.
     */
    @Transactional
    fun updateAllowedOrigins(command: UpdateChannelIntegrationAllowedOriginsCommand): ChannelIntegrationResult {
        channelAccessPolicy.requirePlatformAdmin(command.actor)
        val integration = findOwnedIntegration(command.channelId, command.integrationId)
        val updated = integration.updateAllowedOrigins(AllowedOrigins.of(command.allowedOrigins))
        return ChannelIntegrationResult.from(channelIntegrationRepository.save(updated))
    }

    /**
     * Requires the channel to exist and be active.
     */
    private fun requireActiveChannel(channelId: String) {
        val channel =
            channelRepository.findById(channelId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_NOT_FOUND,
                    details = mapOf("channelId" to channelId),
                )
        if (channel.status != ChannelStatus.ACTIVE) {
            throw ConversationException(
                error = ConversationError.CHANNEL_NOT_ACTIVE,
                details = mapOf("channelId" to channelId, "status" to channel.status.name),
            )
        }
    }

    /**
     * Requires no active integration for a channel and type.
     */
    private fun requireNoActiveIntegration(
        channelId: String,
        type: ChannelIntegrationType,
    ) {
        if (channelIntegrationRepository.existsByChannelIdAndTypeAndStatus(channelId, type, ChannelIntegrationStatus.ACTIVE)) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_ALREADY_EXISTS,
                details = mapOf("channelId" to channelId, "type" to type.name),
            )
        }
    }

    /**
     * Requires no other active integration with the same channel and type.
     */
    private fun requireNoOtherActiveIntegration(integration: ChannelIntegration) {
        if (
            channelIntegrationRepository.existsByChannelIdAndTypeAndStatusAndIdNot(
                channelId = integration.channelId,
                type = integration.type,
                status = ChannelIntegrationStatus.ACTIVE,
                excludedId = integration.id,
            )
        ) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_ALREADY_EXISTS,
                details = mapOf("channelId" to integration.channelId, "type" to integration.type.name),
            )
        }
    }

    /**
     * Finds an integration that belongs to the requested channel.
     */
    private fun findOwnedIntegration(
        channelId: String,
        integrationId: String,
    ): ChannelIntegration {
        val integration =
            channelIntegrationRepository.findById(integrationId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_INTEGRATION_NOT_FOUND,
                    details = mapOf("channelId" to channelId, "integrationId" to integrationId),
                )
        if (integration.channelId != channelId) {
            throw ConversationException(
                error = ConversationError.CHANNEL_INTEGRATION_NOT_FOUND,
                details = mapOf("channelId" to channelId, "integrationId" to integrationId),
            )
        }
        return integration
    }
}

data class CreateWidgetIntegrationCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val allowedOrigins: List<String>,
)

data class ChangeChannelIntegrationStatusCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val integrationId: String,
)

data class UpdateChannelIntegrationAllowedOriginsCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val integrationId: String,
    val allowedOrigins: List<String>,
)

data class WidgetIntegrationCreationResult(
    val integration: ChannelIntegrationResult,
    val secret: String,
)
