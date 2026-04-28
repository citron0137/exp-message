package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.membership

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus

@Repository
class ChannelMembershipRepositoryAdapter(
    private val jpaRepository: ChannelMembershipJpaRepository,
) : ChannelMembershipRepository {
    /**
     * Saves a channel membership through JPA.
     */
    override fun save(membership: ChannelMembership): ChannelMembership = jpaRepository.save(membership.toEntity()).toDomain()

    /**
     * Finds a channel membership by identifier through JPA.
     */
    override fun findById(id: String): ChannelMembership? = jpaRepository.findById(id).orElse(null)?.toDomain()

    /**
     * Finds channel memberships by channel identifier through JPA.
     */
    override fun findByChannelId(channelId: String): List<ChannelMembership> =
        jpaRepository.findByChannelId(channelId).map { it.toDomain() }

    /**
     * Finds channel memberships by user identifier through JPA.
     */
    override fun findByUserId(userId: String): List<ChannelMembership> =
        jpaRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).map { it.toDomain() }

    /**
     * Finds memberships by optional role and status filters through JPA.
     */
    override fun findByChannelIdAndFilters(
        channelId: String,
        role: ChannelMembershipRole?,
        status: ChannelMembershipStatus?,
    ): List<ChannelMembership> {
        val entities =
            jpaRepository.findByChannelIdAndFilters(
                channelId = channelId,
                role = role?.name,
                status = status?.name,
            )
        return entities.map { it.toDomain() }
    }

    /**
     * Finds a channel membership by channel and user through JPA.
     */
    override fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelMembership? = jpaRepository.findByChannelIdAndUserId(channelId, userId)?.toDomain()

    /**
     * Returns true when a membership already exists for the channel and user through JPA.
     */
    override fun existsByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): Boolean = jpaRepository.existsByChannelIdAndUserId(channelId, userId)

    /**
     * Counts memberships by channel, role, and status through JPA.
     */
    override fun countByChannelIdAndRoleAndStatus(
        channelId: String,
        role: ChannelMembershipRole,
        status: ChannelMembershipStatus,
    ): Long =
        jpaRepository.countByChannelIdAndRoleAndStatus(
            channelId = channelId,
            role = role.name,
            status = status.name,
        )

    /**
     * Maps a channel membership domain object to a JPA entity.
     */
    private fun ChannelMembership.toEntity(): ChannelMembershipEntity =
        ChannelMembershipEntity(
            id = id,
            channelId = channelId,
            userId = userId,
            role = role.name,
            agentStatus = agentStatus.name,
            status = status.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    /**
     * Maps a channel membership JPA entity to a domain object.
     */
    private fun ChannelMembershipEntity.toDomain(): ChannelMembership =
        ChannelMembership(
            id = id,
            channelId = channelId,
            userId = userId,
            role = ChannelMembershipRole.valueOf(role),
            agentStatus = AgentStatus.valueOf(agentStatus),
            status = ChannelMembershipStatus.valueOf(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
