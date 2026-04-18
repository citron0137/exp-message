package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class ChannelConversation(
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
    /**
     * Returns true when the conversation can be reused for visitor entry.
     */
    fun canReuseForVisitorEntry(): Boolean =
        status == ChannelConversationStatus.PENDING ||
            status == ChannelConversationStatus.OPEN ||
            status == ChannelConversationStatus.DORMANT

    /**
     * Returns true when the conversation can accept a visitor message without reactivation.
     */
    fun canAcceptVisitorMessage(): Boolean =
        status == ChannelConversationStatus.PENDING ||
            status == ChannelConversationStatus.OPEN

    /**
     * Returns true when the visitor can view this conversation.
     */
    fun canBeViewedByVisitor(): Boolean = status != ChannelConversationStatus.CLOSED

    /**
     * Returns the next message sequence and an updated conversation copy.
     */
    fun issueNextMessageSequence(now: LocalDateTime = LocalDateTime.now()): MessageSequenceIssue {
        val nextSequence = lastMessageSequence + 1
        return MessageSequenceIssue(
            conversation = copy(lastMessageSequence = nextSequence, updatedAt = now),
            sequence = nextSequence,
        )
    }

    /**
     * Returns a conversation copy with latest message summary values.
     */
    fun recordMessage(
        sequence: Long,
        messageCreatedAt: LocalDateTime,
    ): ChannelConversation =
        copy(
            lastMessageSequence = sequence,
            lastMessageAt = messageCreatedAt,
            updatedAt = messageCreatedAt,
        )

    /**
     * Returns a conversation copy with the requested status.
     */
    fun changeStatus(
        nextStatus: ChannelConversationStatus,
        now: LocalDateTime = LocalDateTime.now(),
    ): ChannelConversation =
        when (nextStatus) {
            ChannelConversationStatus.PENDING -> copy(status = ChannelConversationStatus.PENDING, updatedAt = now, closedAt = null)
            ChannelConversationStatus.OPEN -> copy(status = ChannelConversationStatus.OPEN, updatedAt = now, closedAt = null)
            ChannelConversationStatus.DORMANT -> copy(status = ChannelConversationStatus.DORMANT, updatedAt = now, closedAt = null)
            ChannelConversationStatus.CLOSED -> copy(status = ChannelConversationStatus.CLOSED, updatedAt = now, closedAt = now)
        }

    /**
     * Returns a conversation copy assigned to a channel membership.
     */
    fun assignTo(
        membershipId: String?,
        now: LocalDateTime = LocalDateTime.now(),
    ): ChannelConversation = copy(assigneeMembershipId = membershipId, updatedAt = now)

    /**
     * Returns an open conversation copy.
     */
    fun markOpen(now: LocalDateTime = LocalDateTime.now()): ChannelConversation =
        copy(status = ChannelConversationStatus.OPEN, updatedAt = now, closedAt = null)

    /**
     * Returns a dormant conversation copy.
     */
    fun markDormant(now: LocalDateTime = LocalDateTime.now()): ChannelConversation =
        copy(status = ChannelConversationStatus.DORMANT, updatedAt = now, closedAt = null)

    /**
     * Returns a closed conversation copy.
     */
    fun markClosed(now: LocalDateTime = LocalDateTime.now()): ChannelConversation =
        copy(status = ChannelConversationStatus.CLOSED, updatedAt = now, closedAt = now)

    /**
     * Reactivates a dormant conversation as pending.
     */
    fun reactivateAsPending(now: LocalDateTime = LocalDateTime.now()): ChannelConversation =
        if (status == ChannelConversationStatus.DORMANT) {
            copy(status = ChannelConversationStatus.PENDING, updatedAt = now, closedAt = null)
        } else {
            this
        }

    companion object {
        /**
         * Starts a pending channel conversation for a visitor.
         */
        fun start(
            channelId: String,
            visitorId: String,
        ): ChannelConversation {
            val now = LocalDateTime.now()
            return ChannelConversation(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                visitorId = visitorId,
                status = ChannelConversationStatus.PENDING,
                assigneeMembershipId = null,
                lastMessageSequence = 0,
                lastMessageAt = null,
                createdAt = now,
                updatedAt = now,
                closedAt = null,
            )
        }
    }
}

data class MessageSequenceIssue(
    val conversation: ChannelConversation,
    val sequence: Long,
)
