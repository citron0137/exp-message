package site.rahoon.message.monolithic.core.conversation.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.port.CreateOrLoadCustomerAdminIdentityCommand
import site.rahoon.message.monolithic.core.conversation.application.port.CustomerAdminIdentityPort
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import java.time.LocalDateTime

@Service
class AdminChannelFacade(
    private val channelRepository: ChannelRepository,
    private val channelMembershipRepository: ChannelMembershipRepository,
    private val customerAdminIdentityPort: CustomerAdminIdentityPort,
    private val channelAccessPolicy: ChannelAccessPolicy,
) {
    /**
     * Creates a channel and its initial customer admin membership.
     */
    @Transactional
    fun createChannel(command: CreateAdminChannelCommand): AdminChannelCreationResult {
        channelAccessPolicy.requirePlatformAdmin(command.actor)
        val channel = channelRepository.save(Channel.create(command.name))
        val customerAdmin =
            customerAdminIdentityPort.createOrLoadCustomerAdmin(
                CreateOrLoadCustomerAdminIdentityCommand(
                    email = command.adminEmail,
                    nickname = command.adminNickname,
                ),
            )
        channelMembershipRepository.save(
            ChannelMembership.createChannelAdmin(
                channelId = channel.id,
                userId = customerAdmin.userId,
            ),
        )
        return AdminChannelCreationResult(
            channel = ChannelResult.from(channel),
            initialAdmin =
                InitialChannelAdminResult(
                    userId = customerAdmin.userId,
                    email = customerAdmin.email,
                    nickname = customerAdmin.nickname,
                    temporaryPassword = customerAdmin.temporaryPassword,
                    created = customerAdmin.created,
                ),
        )
    }

    /**
     * Lists channels for a platform admin.
     */
    @Transactional(readOnly = true)
    fun listChannels(actor: AuthenticatedPrincipal): List<ChannelResult> {
        channelAccessPolicy.requirePlatformAdmin(actor)
        return channelRepository.findAll().map { ChannelResult.from(it) }
    }

    /**
     * Gets a channel by identifier for a platform admin.
     */
    @Transactional(readOnly = true)
    fun getChannel(
        actor: AuthenticatedPrincipal,
        channelId: String,
    ): ChannelResult {
        channelAccessPolicy.requirePlatformAdmin(actor)
        val channel =
            channelRepository.findById(channelId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_NOT_FOUND,
                    details = mapOf("channelId" to channelId),
                )
        return ChannelResult.from(channel)
    }
}

data class CreateAdminChannelCommand(
    val actor: AuthenticatedPrincipal,
    val name: String,
    val adminEmail: String,
    val adminNickname: String,
)

data class AdminChannelCreationResult(
    val channel: ChannelResult,
    val initialAdmin: InitialChannelAdminResult,
)

data class ChannelResult(
    val id: String,
    val name: String,
    val status: ChannelStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * Maps a channel domain object to an application result.
         */
        fun from(channel: Channel): ChannelResult =
            ChannelResult(
                id = channel.id,
                name = channel.name,
                status = channel.status,
                createdAt = channel.createdAt,
                updatedAt = channel.updatedAt,
            )
    }
}

data class InitialChannelAdminResult(
    val userId: String,
    val email: String,
    val nickname: String,
    val temporaryPassword: String?,
    val created: Boolean,
)
