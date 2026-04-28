package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.channel

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus

@Repository
class ChannelRepositoryAdapter(
    private val jpaRepository: ChannelJpaRepository,
) : ChannelRepository {
    /**
     * Saves a channel through JPA.
     */
    override fun save(channel: Channel): Channel = jpaRepository.save(channel.toEntity()).toDomain()

    /**
     * Finds a channel by identifier through JPA.
     */
    override fun findById(id: String): Channel? = jpaRepository.findById(id).orElse(null)?.toDomain()

    /**
     * Lists all channels through JPA.
     */
    override fun findAll(): List<Channel> = jpaRepository.findAll().map { it.toDomain() }

    /**
     * Maps a channel domain object to a JPA entity.
     */
    private fun Channel.toEntity(): ChannelEntity =
        ChannelEntity(
            id = id,
            name = name,
            status = status.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    /**
     * Maps a channel JPA entity to a domain object.
     */
    private fun ChannelEntity.toDomain(): Channel =
        Channel(
            id = id,
            name = name,
            status = ChannelStatus.valueOf(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
