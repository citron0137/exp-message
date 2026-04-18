package site.rahoon.message.monolithic.channel.infrastructure

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.common.infrastructure.JpaSoftDeleteRepository

/**
 * Spring Data JPA Repository
 */
@Repository("legacyChannelJpaRepository")
interface ChannelJpaRepository : JpaSoftDeleteRepository<ChannelEntity, String> {
    fun findByApiKey(apiKey: String): ChannelEntity?
}
