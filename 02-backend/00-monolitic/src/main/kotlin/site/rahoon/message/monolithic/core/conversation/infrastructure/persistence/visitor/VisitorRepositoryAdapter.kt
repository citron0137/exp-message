package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.visitor

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorRepository
import site.rahoon.message.monolithic.core.conversation.domain.Visitor

@Repository
class VisitorRepositoryAdapter(
    private val jpaRepository: VisitorJpaRepository,
    private val objectMapper: ObjectMapper,
) : VisitorRepository {
    private val metadataType = object : TypeReference<Map<String, String>>() {}

    /**
     * Saves a visitor through JPA.
     */
    override fun save(visitor: Visitor): Visitor = jpaRepository.save(visitor.toEntity()).toDomain()

    /**
     * Finds a visitor by identifier through JPA.
     */
    override fun findById(id: String): Visitor? = jpaRepository.findById(id).orElse(null)?.toDomain()

    /**
     * Maps a visitor domain object to a JPA entity.
     */
    private fun Visitor.toEntity(): VisitorEntity =
        VisitorEntity(
            id = id,
            channelId = channelId,
            externalId = externalId,
            displayName = displayName,
            email = email,
            metadata = objectMapper.writeValueAsString(metadata),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    /**
     * Maps a visitor JPA entity to a domain object.
     */
    private fun VisitorEntity.toDomain(): Visitor =
        Visitor(
            id = id,
            channelId = channelId,
            externalId = externalId,
            displayName = displayName,
            email = email,
            metadata = objectMapper.readValue(metadata, metadataType),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
