package site.rahoon.message.monolithic.core.conversation.application.port

import java.time.LocalDateTime

interface AdminConversationReader {
    /**
     * Lists conversations for the admin inbox.
     */
    fun listConversations(
        channelId: String,
        status: String?,
        assigneeMembershipId: String?,
        unassignedOnly: Boolean,
        cursorActivityAt: LocalDateTime?,
        cursorId: String?,
        limit: Int,
    ): List<AdminConversationListRow>

    /**
     * Finds one conversation detail for the admin inbox.
     */
    fun findConversationDetail(
        channelId: String,
        conversationId: String,
    ): AdminConversationDetailRow?

    /**
     * Returns true when a conversation belongs to a channel.
     */
    fun existsConversation(
        channelId: String,
        conversationId: String,
    ): Boolean
}

data class AdminConversationListRow(
    val id: String,
    val channelId: String,
    val visitorId: String,
    val status: String,
    val activityAt: LocalDateTime,
    val lastMessageSequence: Long,
    val lastMessageAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val closedAt: LocalDateTime?,
    val visitorExternalId: String?,
    val visitorDisplayName: String?,
    val visitorEmail: String?,
    val assigneeMembershipId: String?,
    val assigneeUserId: String?,
    val assigneeRole: String?,
    val assigneeAgentStatus: String?,
    val lastMessageId: String?,
    val lastMessageSequenceValue: Long?,
    val lastMessageSenderType: String?,
    val lastMessageContent: String?,
    val lastMessageCreatedAt: LocalDateTime?,
)

data class AdminConversationDetailRow(
    val id: String,
    val channelId: String,
    val visitorId: String,
    val status: String,
    val lastMessageSequence: Long,
    val lastMessageAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val closedAt: LocalDateTime?,
    val visitorExternalId: String?,
    val visitorDisplayName: String?,
    val visitorEmail: String?,
    val assigneeMembershipId: String?,
    val assigneeUserId: String?,
    val assigneeRole: String?,
    val assigneeAgentStatus: String?,
)
