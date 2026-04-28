package site.rahoon.message.monolithic.channelconversation.infrastructure

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.common.infrastructure.JpaSoftDeleteRepository

/**
 * Spring Data JPA Repository
 */
@Repository("legacyChannelConversationJpaRepository")
interface ChannelConversationJpaRepository : JpaSoftDeleteRepository<ChannelConversationEntity, String> {
    fun findByChannelId(channelId: String): List<ChannelConversationEntity>

    fun findByCustomerId(customerId: String): List<ChannelConversationEntity>
}
