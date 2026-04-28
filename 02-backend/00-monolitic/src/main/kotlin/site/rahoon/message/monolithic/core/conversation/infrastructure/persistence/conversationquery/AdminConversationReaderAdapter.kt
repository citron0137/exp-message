package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.conversationquery

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationDetailRow
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationListRow
import site.rahoon.message.monolithic.core.conversation.application.port.AdminConversationReader
import java.time.LocalDateTime

@Repository
class AdminConversationReaderAdapter(
    private val jpaRepository: AdminConversationJpaQueryRepository,
) : AdminConversationReader {
    /**
     * Lists conversations through the admin inbox JPA query.
     */
    override fun listConversations(
        channelId: String,
        status: String?,
        assigneeMembershipId: String?,
        unassignedOnly: Boolean,
        cursorActivityAt: LocalDateTime?,
        cursorId: String?,
        limit: Int,
    ): List<AdminConversationListRow> {
        val projections =
            jpaRepository.listConversations(
                channelId = channelId,
                status = status,
                assigneeMembershipId = assigneeMembershipId,
                unassignedOnly = if (unassignedOnly) 1 else 0,
                cursorActivityAt = cursorActivityAt,
                cursorId = cursorId,
                limit = limit,
            )
        return projections.map { it.toRow() }
    }

    /**
     * Finds one conversation detail through the admin inbox JPA query.
     */
    override fun findConversationDetail(
        channelId: String,
        conversationId: String,
    ): AdminConversationDetailRow? =
        jpaRepository
            .findConversationDetail(channelId = channelId, conversationId = conversationId)
            ?.toRow()

    /**
     * Checks conversation ownership through the admin inbox JPA query.
     */
    override fun existsConversation(
        channelId: String,
        conversationId: String,
    ): Boolean = jpaRepository.existsConversation(channelId = channelId, conversationId = conversationId)

    /**
     * Maps a JPA list projection to an application row.
     */
    private fun AdminConversationListProjection.toRow(): AdminConversationListRow =
        AdminConversationListRow(
            id = id,
            channelId = channelId,
            visitorId = visitorId,
            status = status,
            activityAt = activityAt,
            lastMessageSequence = lastMessageSequence,
            lastMessageAt = lastMessageAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            closedAt = closedAt,
            visitorExternalId = visitorExternalId,
            visitorDisplayName = visitorDisplayName,
            visitorEmail = visitorEmail,
            assigneeMembershipId = assigneeMembershipId,
            assigneeUserId = assigneeUserId,
            assigneeRole = assigneeRole,
            assigneeAgentStatus = assigneeAgentStatus,
            lastMessageId = lastMessageId,
            lastMessageSequenceValue = lastMessageSequenceValue,
            lastMessageSenderType = lastMessageSenderType,
            lastMessageContent = lastMessageContent,
            lastMessageCreatedAt = lastMessageCreatedAt,
        )

    /**
     * Maps a JPA detail projection to an application row.
     */
    private fun AdminConversationDetailProjection.toRow(): AdminConversationDetailRow =
        AdminConversationDetailRow(
            id = id,
            channelId = channelId,
            visitorId = visitorId,
            status = status,
            lastMessageSequence = lastMessageSequence,
            lastMessageAt = lastMessageAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            closedAt = closedAt,
            visitorExternalId = visitorExternalId,
            visitorDisplayName = visitorDisplayName,
            visitorEmail = visitorEmail,
            assigneeMembershipId = assigneeMembershipId,
            assigneeUserId = assigneeUserId,
            assigneeRole = assigneeRole,
            assigneeAgentStatus = assigneeAgentStatus,
        )
}
