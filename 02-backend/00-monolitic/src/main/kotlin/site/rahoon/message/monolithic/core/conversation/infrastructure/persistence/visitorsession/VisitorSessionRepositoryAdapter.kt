package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.visitorsession

import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionRepository
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession

@Repository
class VisitorSessionRepositoryAdapter(
    private val jpaRepository: VisitorSessionJpaRepository,
) : VisitorSessionRepository {
    /**
     * Saves a visitor session through JPA.
     */
    override fun save(session: VisitorSession): VisitorSession = jpaRepository.save(session.toEntity()).toDomain()

    /**
     * Finds a visitor session by token hash through JPA.
     */
    override fun findByTokenHash(tokenHash: String): VisitorSession? = jpaRepository.findByTokenHash(tokenHash)?.toDomain()

    /**
     * Maps a visitor session domain object to a JPA entity.
     */
    private fun VisitorSession.toEntity(): VisitorSessionEntity =
        VisitorSessionEntity(
            id = id,
            visitorId = visitorId,
            channelId = channelId,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
            createdAt = createdAt,
            lastSeenAt = lastSeenAt,
        )

    /**
     * Maps a visitor session JPA entity to a domain object.
     */
    private fun VisitorSessionEntity.toDomain(): VisitorSession =
        VisitorSession(
            id = id,
            visitorId = visitorId,
            channelId = channelId,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
            createdAt = createdAt,
            lastSeenAt = lastSeenAt,
        )
}
