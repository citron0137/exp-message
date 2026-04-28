package site.rahoon.message.monolithic.core.conversation.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessage
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageSenderType
import site.rahoon.message.monolithic.core.conversation.domain.MessageContent
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import java.time.LocalDateTime

@Service
class AdminConversationFacade(
    private val channelAccessPolicy: ChannelAccessPolicy,
    private val channelConversationRepository: ChannelConversationRepository,
    private val channelMembershipRepository: ChannelMembershipRepository,
    private val conversationMessageRepository: ConversationMessageRepository,
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
     * Stores an admin reply as an agent message.
     */
    @Transactional
    fun sendReply(command: SendAdminConversationReplyCommand): ConversationMessageResult {
        val membership = requireReplyMembership(command)
        val conversation = loadOwnedConversation(command.channelId, command.conversationId)
        requireReplyableConversation(conversation)
        val existingMessage =
            conversationMessageRepository.findByIdempotencyKey(
                conversationId = command.conversationId,
                senderType = ConversationMessageSenderType.AGENT,
                senderId = membership.id,
                clientMessageId = command.clientMessageId,
            )
        if (existingMessage != null) {
            return ConversationMessageResult.from(existingMessage)
        }
        val sequenceIssue = conversation.issueNextMessageSequence()
        val message =
            ConversationMessage.agentText(
                conversationId = conversation.id,
                channelId = conversation.channelId,
                membershipId = membership.id,
                sequence = sequenceIssue.sequence,
                clientMessageId = command.clientMessageId,
                content = MessageContent.text(command.content),
            )
        val savedMessage = conversationMessageRepository.save(message)
        val nextConversation =
            sequenceIssue.conversation
                .markOpen(savedMessage.createdAt)
                .recordMessage(
                    sequence = savedMessage.sequence,
                    messageCreatedAt = savedMessage.createdAt,
                )
        channelConversationRepository.save(nextConversation)
        return ConversationMessageResult.from(savedMessage)
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
        if (membership.channelId != channelId || membership.role !in ASSIGNABLE_ROLES || !membership.canBeAssigned()) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_NOT_ASSIGNABLE,
                details = mapOf("membershipId" to membershipId, "channelId" to channelId),
            )
        }
        return membership
    }

    /**
     * Returns the actor's channel membership when it is allowed to speak in the conversation.
     *
     * Admin replies are intentionally tied to `ChannelMembership`, not to the global IAM role. A
     * `PLATFORM_ADMIN` can supervise channels and perform administrative operations, but a customer
     * conversation should show a real channel operator as the sender. This keeps message history
     * understandable after a channel is separated into another service or after platform staff
     * rotate. If a platform user must reply to customers later, they should first receive an
     * explicit channel membership so the sender identity remains channel-owned.
     *
     * Disabled memberships are also blocked even when the IAM session is valid. Disabling a
     * membership is the operator offboarding switch for the channel, so allowing that user to reply
     * would make disable semantics unreliable for support operations.
     */
    private fun requireReplyMembership(command: SendAdminConversationReplyCommand): ChannelMembership {
        val membership = channelMembershipRepository.findByChannelIdAndUserId(command.channelId, command.actor.userId)
        if (membership == null || membership.role !in REPLY_ROLES || membership.status != ChannelMembershipStatus.ACTIVE) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_REPLY_NOT_ALLOWED,
                details =
                    mapOf(
                        "channelId" to command.channelId,
                        "userId" to command.actor.userId,
                    ),
            )
        }
        return membership
    }

    /**
     * Verifies that an admin reply can be appended to the conversation.
     *
     * `PENDING`, `OPEN`, and `DORMANT` conversations are replyable because they still represent a
     * customer thread that can continue. Sending a reply moves the conversation to `OPEN`, so the
     * inbox has a single active state for conversations with operator activity.
     *
     * `CLOSED` conversations are intentionally immutable from the reply path. Reopening a closed
     * conversation would make long-returning visitors see old threads again and would blur the
     * meaning of a close action. A new visitor entry should create a new conversation instead.
     */
    private fun requireReplyableConversation(conversation: ChannelConversation) {
        if (conversation.status == ChannelConversationStatus.CLOSED) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_REPLY_NOT_ALLOWED,
                details = mapOf("conversationId" to conversation.id, "status" to conversation.status.name),
            )
        }
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
        private val REPLY_ROLES = setOf(ChannelMembershipRole.CHANNEL_ADMIN, ChannelMembershipRole.AGENT)
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

data class SendAdminConversationReplyCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val conversationId: String,
    val clientMessageId: String,
    val content: String,
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
