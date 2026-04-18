package site.rahoon.message.monolithic.core.conversation.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.facade.ConversationMessageResult
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationDetailRow
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationListRow
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationReader
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import java.time.LocalDateTime

@Service
class AdminConversationQueryService(
    private val channelAccessPolicy: ChannelAccessPolicy,
    private val adminConversationReader: AdminConversationReader,
    private val channelMembershipRepository: ChannelMembershipRepository,
    private val conversationMessageRepository: ConversationMessageRepository,
) {
    /**
     * Lists conversations for an admin inbox.
     */
    @Transactional(readOnly = true)
    fun listConversations(query: AdminConversationListQuery): AdminConversationListResult {
        channelAccessPolicy.requireChannelRead(query.actor, query.channelId)
        val cursor = AdminInboxCursor.decodeOrNull(query.cursor)
        validateInboxFilters(query)
        val normalizedLimit = query.limit.coerceIn(1, MAX_LIMIT)
        val rows =
            adminConversationReader.listConversations(
                channelId = query.channelId,
                status = query.status?.name,
                assigneeMembershipId = query.assigneeMembershipId,
                unassignedOnly = query.unassigned,
                cursorActivityAt = cursor?.activityAt,
                cursorId = cursor?.id,
                limit = normalizedLimit + 1,
            )
        val hasMore = rows.size > normalizedLimit
        val pageItems = rows.take(normalizedLimit).map { AdminConversationListItemResult.from(it) }
        return AdminConversationListResult(
            items = pageItems,
            nextCursor =
                if (hasMore) {
                    pageItems.lastOrNull()?.let { AdminInboxCursor.from(it).encode() }
                } else {
                    null
                },
            hasMore = hasMore,
        )
    }

    /**
     * Gets a conversation detail for an admin inbox.
     */
    @Transactional(readOnly = true)
    fun getConversation(query: AdminConversationDetailQuery): AdminConversationDetailResult {
        channelAccessPolicy.requireChannelRead(query.actor, query.channelId)
        val row =
            adminConversationReader.findConversationDetail(query.channelId, query.conversationId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                    details = mapOf("conversationId" to query.conversationId),
                )
        return AdminConversationDetailResult.from(row)
    }

    /**
     * Lists visible conversation messages for an admin inbox.
     */
    @Transactional(readOnly = true)
    fun listMessages(query: AdminConversationMessagesQuery): AdminConversationMessagesResult {
        channelAccessPolicy.requireChannelRead(query.actor, query.channelId)
        if (!adminConversationReader.existsConversation(query.channelId, query.conversationId)) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                details = mapOf("conversationId" to query.conversationId),
            )
        }
        val normalizedLimit = query.limit.coerceIn(1, MAX_LIMIT)
        val messages =
            conversationMessageRepository.findVisibleAfterSequence(
                conversationId = query.conversationId,
                afterSequence = query.afterSequence.coerceAtLeast(0),
                limit = normalizedLimit + 1,
            )
        val hasMore = messages.size > normalizedLimit
        val pageMessages = messages.take(normalizedLimit).map { ConversationMessageResult.from(it) }
        return AdminConversationMessagesResult(
            messages = pageMessages,
            nextAfterSequence = pageMessages.lastOrNull()?.sequence ?: query.afterSequence.coerceAtLeast(0),
            hasMore = hasMore,
        )
    }

    companion object {
        private const val MAX_LIMIT = 100
    }

    /**
     * Verifies admin inbox filter combinations and ownership.
     */
    private fun validateInboxFilters(query: AdminConversationListQuery) {
        if (query.assigneeMembershipId != null && query.unassigned) {
            throw ConversationException(
                error = ConversationError.INVALID_ADMIN_INBOX_FILTER,
                details = mapOf("reason" to "assigneeMembershipId and unassigned cannot be used together"),
            )
        }
        query.assigneeMembershipId?.let { membershipId ->
            validateAssignableMembership(query, membershipId)
        }
    }

    private fun validateAssignableMembership(
        query: AdminConversationListQuery,
        membershipId: String,
    ) {
        val membership =
            channelMembershipRepository.findById(membershipId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_MEMBERSHIP_NOT_FOUND,
                    details = mapOf("membershipId" to membershipId),
                )
        if (membership.channelId != query.channelId) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_NOT_ASSIGNABLE,
                details = mapOf("membershipId" to membershipId, "channelId" to query.channelId),
            )
        }
    }
}

data class AdminConversationListQuery(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val status: ChannelConversationStatus?,
    val assigneeMembershipId: String?,
    val unassigned: Boolean = false,
    val cursor: String? = null,
    val limit: Int = 50,
)

data class AdminConversationDetailQuery(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val conversationId: String,
)

data class AdminConversationMessagesQuery(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val conversationId: String,
    val afterSequence: Long = 0,
    val limit: Int = 50,
)

data class AdminConversationListResult(
    val items: List<AdminConversationListItemResult>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class AdminConversationMessagesResult(
    val messages: List<ConversationMessageResult>,
    val nextAfterSequence: Long,
    val hasMore: Boolean,
)

data class AdminConversationListItemResult(
    val id: String,
    val channelId: String,
    val visitor: AdminConversationVisitorResult,
    val assignee: AdminConversationAssigneeResult?,
    val status: ChannelConversationStatus,
    val activityAt: LocalDateTime,
    val lastMessageSequence: Long,
    val lastMessageAt: LocalDateTime?,
    val lastMessage: AdminConversationLastMessageResult?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val closedAt: LocalDateTime?,
) {
    companion object {
        /**
         * Maps a query projection to an inbox list item.
         */
        fun from(row: AdminConversationListRow): AdminConversationListItemResult =
            AdminConversationListItemResult(
                id = row.id,
                channelId = row.channelId,
                visitor = AdminConversationVisitorResult.from(row),
                assignee = AdminConversationAssigneeResult.from(row),
                status = ChannelConversationStatus.valueOf(row.status),
                activityAt = row.activityAt,
                lastMessageSequence = row.lastMessageSequence,
                lastMessageAt = row.lastMessageAt,
                lastMessage =
                    row.lastMessageId?.let {
                        AdminConversationLastMessageResult(
                            id = it,
                            sequence = row.lastMessageSequenceValue,
                            senderType = row.lastMessageSenderType,
                            content = row.lastMessageContent,
                            createdAt = row.lastMessageCreatedAt,
                        )
                    },
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                closedAt = row.closedAt,
            )
    }
}

data class AdminConversationDetailResult(
    val id: String,
    val channelId: String,
    val visitor: AdminConversationVisitorResult,
    val assignee: AdminConversationAssigneeResult?,
    val status: ChannelConversationStatus,
    val lastMessageSequence: Long,
    val lastMessageAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val closedAt: LocalDateTime?,
) {
    companion object {
        /**
         * Maps a query projection to an inbox detail.
         */
        fun from(row: AdminConversationDetailRow): AdminConversationDetailResult =
            AdminConversationDetailResult(
                id = row.id,
                channelId = row.channelId,
                visitor = AdminConversationVisitorResult.from(row),
                assignee = AdminConversationAssigneeResult.from(row),
                status = ChannelConversationStatus.valueOf(row.status),
                lastMessageSequence = row.lastMessageSequence,
                lastMessageAt = row.lastMessageAt,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                closedAt = row.closedAt,
            )
    }
}

data class AdminConversationVisitorResult(
    val id: String,
    val externalId: String?,
    val displayName: String?,
    val email: String?,
) {
    companion object {
        /**
         * Maps a list projection visitor shape to a result.
         */
        fun from(row: AdminConversationListRow): AdminConversationVisitorResult =
            AdminConversationVisitorResult(
                id = row.visitorId,
                externalId = row.visitorExternalId,
                displayName = row.visitorDisplayName,
                email = row.visitorEmail,
            )

        /**
         * Maps a detail projection visitor shape to a result.
         */
        fun from(row: AdminConversationDetailRow): AdminConversationVisitorResult =
            AdminConversationVisitorResult(
                id = row.visitorId,
                externalId = row.visitorExternalId,
                displayName = row.visitorDisplayName,
                email = row.visitorEmail,
            )
    }
}

data class AdminConversationAssigneeResult(
    val membershipId: String,
    val userId: String,
    val role: String,
    val agentStatus: String,
) {
    companion object {
        /**
         * Maps a list projection assignee shape to a result.
         */
        fun from(row: AdminConversationListRow): AdminConversationAssigneeResult? =
            row.assigneeMembershipId?.let {
                AdminConversationAssigneeResult(
                    membershipId = it,
                    userId = row.assigneeUserId.orEmpty(),
                    role = row.assigneeRole.orEmpty(),
                    agentStatus = row.assigneeAgentStatus.orEmpty(),
                )
            }

        /**
         * Maps a detail projection assignee shape to a result.
         */
        fun from(row: AdminConversationDetailRow): AdminConversationAssigneeResult? =
            row.assigneeMembershipId?.let {
                AdminConversationAssigneeResult(
                    membershipId = it,
                    userId = row.assigneeUserId.orEmpty(),
                    role = row.assigneeRole.orEmpty(),
                    agentStatus = row.assigneeAgentStatus.orEmpty(),
                )
            }
    }
}

data class AdminConversationLastMessageResult(
    val id: String,
    val sequence: Long?,
    val senderType: String?,
    val content: String?,
    val createdAt: LocalDateTime?,
)
