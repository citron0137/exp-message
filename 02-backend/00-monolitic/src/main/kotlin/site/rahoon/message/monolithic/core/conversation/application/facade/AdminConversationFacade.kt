package site.rahoon.message.monolithic.core.conversation.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import java.time.LocalDateTime

@Service
class AdminConversationFacade(
    private val channelAccessPolicy: ChannelAccessPolicy,
    private val channelConversationRepository: ChannelConversationRepository,
    private val channelMembershipRepository: ChannelMembershipRepository,
) {
    /**
     * Changes a conversation status through the admin operation path.
     */
    @Transactional
    fun changeStatus(command: ChangeConversationStatusCommand): AdminConversationOperationResult {
        channelAccessPolicy.requireChannelAdminWrite(command.actor, command.channelId)
        val conversation = loadOwnedConversation(command.channelId, command.conversationId)
        requireAllowedStatusChange(conversation, command.status)
        return AdminConversationOperationResult.from(
            channelConversationRepository.save(conversation.changeStatus(command.status)),
        )
    }

    /**
     * Assigns or unassigns a conversation through the admin operation path.
     */
    @Transactional
    fun changeAssignee(command: ChangeConversationAssigneeCommand): AdminConversationOperationResult {
        channelAccessPolicy.requireChannelAdminWrite(command.actor, command.channelId)
        val conversation = loadOwnedConversation(command.channelId, command.conversationId)
        if (conversation.status == ChannelConversationStatus.CLOSED) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_STATUS_CHANGE_NOT_ALLOWED,
                details = mapOf("conversationId" to conversation.id, "status" to conversation.status.name),
            )
        }
        val assignee = command.assigneeMembershipId?.let { loadAssignableMembership(command.channelId, it) }
        return AdminConversationOperationResult.from(
            channelConversationRepository.save(conversation.assignTo(assignee?.id)),
        )
    }

    /**
     * Loads a conversation and verifies channel ownership.
     */
    private fun loadOwnedConversation(
        channelId: String,
        conversationId: String,
    ): ChannelConversation {
        val conversation =
            channelConversationRepository.findById(conversationId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                    details = mapOf("conversationId" to conversationId),
                )
        if (conversation.channelId != channelId) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                details = mapOf("conversationId" to conversationId, "channelId" to channelId),
            )
        }
        return conversation
    }

    /**
     * Loads an assignable membership and verifies channel ownership.
     */
    private fun loadAssignableMembership(
        channelId: String,
        membershipId: String,
    ): ChannelMembership {
        val membership =
            channelMembershipRepository.findById(membershipId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_MEMBERSHIP_NOT_FOUND,
                    details = mapOf("membershipId" to membershipId),
                )
        if (membership.channelId != channelId || membership.role !in ASSIGNABLE_ROLES) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_NOT_ASSIGNABLE,
                details = mapOf("membershipId" to membershipId, "channelId" to channelId),
            )
        }
        return membership
    }

    /**
     * Verifies that the requested status transition is allowed.
     */
    private fun requireAllowedStatusChange(
        conversation: ChannelConversation,
        nextStatus: ChannelConversationStatus,
    ) {
        val allowed =
            when (conversation.status) {
                ChannelConversationStatus.PENDING ->
                    nextStatus == ChannelConversationStatus.OPEN ||
                        nextStatus == ChannelConversationStatus.CLOSED ||
                        nextStatus == ChannelConversationStatus.PENDING
                ChannelConversationStatus.OPEN ->
                    nextStatus == ChannelConversationStatus.DORMANT ||
                        nextStatus == ChannelConversationStatus.CLOSED ||
                        nextStatus == ChannelConversationStatus.OPEN
                ChannelConversationStatus.DORMANT ->
                    nextStatus == ChannelConversationStatus.OPEN ||
                        nextStatus == ChannelConversationStatus.CLOSED ||
                        nextStatus == ChannelConversationStatus.DORMANT
                ChannelConversationStatus.CLOSED -> false
            }
        if (!allowed) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_STATUS_CHANGE_NOT_ALLOWED,
                details =
                    mapOf(
                        "conversationId" to conversation.id,
                        "currentStatus" to conversation.status.name,
                        "nextStatus" to nextStatus.name,
                    ),
            )
        }
    }

    companion object {
        private val ASSIGNABLE_ROLES = setOf(ChannelMembershipRole.CHANNEL_ADMIN, ChannelMembershipRole.AGENT)
    }
}

data class ChangeConversationStatusCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val conversationId: String,
    val status: ChannelConversationStatus,
)

data class ChangeConversationAssigneeCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val conversationId: String,
    val assigneeMembershipId: String?,
)

data class AdminConversationOperationResult(
    val id: String,
    val channelId: String,
    val visitorId: String,
    val status: ChannelConversationStatus,
    val assigneeMembershipId: String?,
    val lastMessageSequence: Long,
    val lastMessageAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val closedAt: LocalDateTime?,
) {
    companion object {
        /**
         * Maps a conversation aggregate to an admin operation result.
         */
        fun from(conversation: ChannelConversation): AdminConversationOperationResult =
            AdminConversationOperationResult(
                id = conversation.id,
                channelId = conversation.channelId,
                visitorId = conversation.visitorId,
                status = conversation.status,
                assigneeMembershipId = conversation.assigneeMembershipId,
                lastMessageSequence = conversation.lastMessageSequence,
                lastMessageAt = conversation.lastMessageAt,
                createdAt = conversation.createdAt,
                updatedAt = conversation.updatedAt,
                closedAt = conversation.closedAt,
            )
    }
}
