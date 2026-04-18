package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.channelconversation

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus

@Repository
class ChannelConversationRepositoryAdapter(
    private val jpaRepository: ChannelConversationJpaRepository,
) : ChannelConversationRepository {
    /**
     * Saves a channel conversation through JPA.
     */
    override fun save(conversation: ChannelConversation): ChannelConversation = jpaRepository.save(conversation.toEntity()).toDomain()

    /**
     * Finds a channel conversation by id through JPA.
     */
    override fun findById(id: String): ChannelConversation? = jpaRepository.findById(id).orElse(null)?.toDomain()

    /**
     * Finds a reusable conversation for a channel and visitor through JPA.
     */
    override fun findReusableByChannelIdAndVisitorId(
        channelId: String,
        visitorId: String,
    ): ChannelConversation? {
        val entity =
            jpaRepository.findFirstByChannelIdAndVisitorIdAndStatusInOrderByUpdatedAtDesc(
                channelId = channelId,
                visitorId = visitorId,
                statuses =
                    listOf(
                        ChannelConversationStatus.PENDING.name,
                        ChannelConversationStatus.OPEN.name,
                        ChannelConversationStatus.DORMANT.name,
                    ),
            )
        return entity?.toDomain()
    }

    /**
     * Maps a channel conversation domain object to a JPA entity.
     */
    private fun ChannelConversation.toEntity(): ChannelConversationEntity =
        ChannelConversationEntity(
            id = id,
            channelId = channelId,
            visitorId = visitorId,
            status = status.name,
            lastMessageSequence = lastMessageSequence,
            createdAt = createdAt,
            updatedAt = updatedAt,
            closedAt = closedAt,
        )

    /**
     * Maps a channel conversation JPA entity to a domain object.
     */
    private fun ChannelConversationEntity.toDomain(): ChannelConversation =
        ChannelConversation(
            id = id,
            channelId = channelId,
            visitorId = visitorId,
            status = ChannelConversationStatus.valueOf(status),
            lastMessageSequence = lastMessageSequence,
            createdAt = createdAt,
            updatedAt = updatedAt,
            closedAt = closedAt,
        )
}
