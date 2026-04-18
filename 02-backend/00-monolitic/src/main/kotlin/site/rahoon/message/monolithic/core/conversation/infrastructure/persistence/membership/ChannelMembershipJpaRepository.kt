package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.membership

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChannelMembershipJpaRepository : JpaRepository<ChannelMembershipEntity, String> {
    /**
     * Finds membership entities by channel identifier.
     */
    fun findByChannelId(channelId: String): List<ChannelMembershipEntity>

    /**
     * Finds membership entities by optional role and status filters.
     */
    @Query(
        """
        SELECT m
        FROM ChannelMembershipEntity m
        WHERE m.channelId = :channelId
            AND (:role IS NULL OR m.role = :role)
            AND (:status IS NULL OR m.status = :status)
        ORDER BY m.createdAt DESC, m.id DESC
        """,
    )
    fun findByChannelIdAndFilters(
        @Param("channelId") channelId: String,
        @Param("role") role: String?,
        @Param("status") status: String?,
    ): List<ChannelMembershipEntity>

    /**
     * Finds a membership entity by channel and user.
     */
    fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelMembershipEntity?

    /**
     * Returns true when a membership entity exists by channel and user.
     */
    fun existsByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): Boolean

    /**
     * Counts membership entities by channel, role, and status.
     */
    fun countByChannelIdAndRoleAndStatus(
        channelId: String,
        role: String,
        status: String,
    ): Long
}
