package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.conversationquery

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AdminConversationJpaQueryRepository : Repository<AdminConversationQueryEntity, String> {
    /**
     * Lists conversations for the admin inbox.
     */
    @Query(
        value = """
            SELECT
                c.id AS id,
                c.channel_id AS channelId,
                c.visitor_id AS visitorId,
                c.status AS status,
                COALESCE(c.last_message_at, c.created_at) AS activityAt,
                c.assignee_membership_id AS assigneeMembershipId,
                c.last_message_sequence AS lastMessageSequence,
                c.last_message_at AS lastMessageAt,
                c.created_at AS createdAt,
                c.updated_at AS updatedAt,
                c.closed_at AS closedAt,
                v.external_id AS visitorExternalId,
                v.display_name AS visitorDisplayName,
                v.email AS visitorEmail,
                a.user_id AS assigneeUserId,
                a.role AS assigneeRole,
                a.agent_status AS assigneeAgentStatus,
                m.id AS lastMessageId,
                m.sequence AS lastMessageSequenceValue,
                m.sender_type AS lastMessageSenderType,
                m.content AS lastMessageContent,
                m.created_at AS lastMessageCreatedAt
            FROM cv_channel_conversations c
            JOIN cv_visitors v ON v.id = c.visitor_id
            LEFT JOIN cv_channel_memberships a ON a.id = c.assignee_membership_id
            LEFT JOIN cv_conversation_messages m
                ON m.conversation_id = c.id
                AND m.sequence = c.last_message_sequence
            WHERE c.channel_id = :channelId
                AND (:status IS NULL OR c.status = :status)
                AND (:assigneeMembershipId IS NULL OR c.assignee_membership_id = :assigneeMembershipId)
                AND (:unassignedOnly = 0 OR c.assignee_membership_id IS NULL)
                AND (
                    :cursorActivityAt IS NULL
                    OR COALESCE(c.last_message_at, c.created_at) < :cursorActivityAt
                    OR (
                        COALESCE(c.last_message_at, c.created_at) = :cursorActivityAt
                        AND c.id < :cursorId
                    )
                )
            ORDER BY
                COALESCE(c.last_message_at, c.created_at) DESC,
                c.id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun listConversations(
        @Param("channelId") channelId: String,
        @Param("status") status: String?,
        @Param("assigneeMembershipId") assigneeMembershipId: String?,
        @Param("unassignedOnly") unassignedOnly: Int,
        @Param("cursorActivityAt") cursorActivityAt: LocalDateTime?,
        @Param("cursorId") cursorId: String?,
        @Param("limit") limit: Int,
    ): List<AdminConversationListProjection>

    /**
     * Finds one conversation detail for the admin inbox.
     */
    @Query(
        value = """
            SELECT
                c.id AS id,
                c.channel_id AS channelId,
                c.visitor_id AS visitorId,
                c.status AS status,
                c.assignee_membership_id AS assigneeMembershipId,
                c.last_message_sequence AS lastMessageSequence,
                c.last_message_at AS lastMessageAt,
                c.created_at AS createdAt,
                c.updated_at AS updatedAt,
                c.closed_at AS closedAt,
                v.external_id AS visitorExternalId,
                v.display_name AS visitorDisplayName,
                v.email AS visitorEmail,
                a.user_id AS assigneeUserId,
                a.role AS assigneeRole,
                a.agent_status AS assigneeAgentStatus
            FROM cv_channel_conversations c
            JOIN cv_visitors v ON v.id = c.visitor_id
            LEFT JOIN cv_channel_memberships a ON a.id = c.assignee_membership_id
            WHERE c.channel_id = :channelId
                AND c.id = :conversationId
            LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findConversationDetail(
        @Param("channelId") channelId: String,
        @Param("conversationId") conversationId: String,
    ): AdminConversationDetailProjection?

    /**
     * Returns true when a conversation belongs to a channel.
     */
    @Query(
        value = """
            SELECT COUNT(1) > 0
            FROM cv_channel_conversations c
            WHERE c.channel_id = :channelId
                AND c.id = :conversationId
        """,
        nativeQuery = true,
    )
    fun existsConversation(
        @Param("channelId") channelId: String,
        @Param("conversationId") conversationId: String,
    ): Boolean
}

interface AdminConversationListProjection {
    val id: String
    val channelId: String
    val visitorId: String
    val status: String
    val activityAt: LocalDateTime
    val lastMessageSequence: Long
    val lastMessageAt: LocalDateTime?
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val closedAt: LocalDateTime?
    val visitorExternalId: String?
    val visitorDisplayName: String?
    val visitorEmail: String?
    val assigneeMembershipId: String?
    val assigneeUserId: String?
    val assigneeRole: String?
    val assigneeAgentStatus: String?
    val lastMessageId: String?
    val lastMessageSequenceValue: Long?
    val lastMessageSenderType: String?
    val lastMessageContent: String?
    val lastMessageCreatedAt: LocalDateTime?
}

interface AdminConversationDetailProjection {
    val id: String
    val channelId: String
    val visitorId: String
    val status: String
    val lastMessageSequence: Long
    val lastMessageAt: LocalDateTime?
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val closedAt: LocalDateTime?
    val visitorExternalId: String?
    val visitorDisplayName: String?
    val visitorEmail: String?
    val assigneeMembershipId: String?
    val assigneeUserId: String?
    val assigneeRole: String?
    val assigneeAgentStatus: String?
}
