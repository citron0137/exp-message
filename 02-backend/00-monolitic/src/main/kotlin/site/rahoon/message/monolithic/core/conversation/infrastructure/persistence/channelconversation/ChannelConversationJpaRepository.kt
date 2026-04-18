package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.channelconversation

import org.springframework.data.jpa.repository.JpaRepository

interface ChannelConversationJpaRepository : JpaRepository<ChannelConversationEntity, String> {
    /**
     * Finds the first reusable conversation for a channel and visitor.
     */
    fun findFirstByChannelIdAndVisitorIdAndStatusInOrderByUpdatedAtDesc(
        channelId: String,
        visitorId: String,
        statuses: Collection<String>,
    ): ChannelConversationEntity?
}
