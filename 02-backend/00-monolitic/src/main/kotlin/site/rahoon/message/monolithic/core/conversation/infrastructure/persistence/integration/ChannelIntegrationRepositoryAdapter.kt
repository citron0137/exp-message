package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType

@Repository
class ChannelIntegrationRepositoryAdapter(
    private val jpaRepository: ChannelIntegrationJpaRepository,
    private val objectMapper: ObjectMapper,
) : ChannelIntegrationRepository {
    private val stringListType = object : TypeReference<List<String>>() {}

    /**
     * Saves a channel integration through JPA.
     */
    override fun save(integration: ChannelIntegration): ChannelIntegration = jpaRepository.save(integration.toEntity()).toDomain()

    /**
     * Finds a channel integration by identifier through JPA.
     */
    override fun findById(id: String): ChannelIntegration? = jpaRepository.findById(id).orElse(null)?.toDomain()

    /**
     * Finds a channel integration by public key through JPA.
     */
    override fun findByPublicKey(publicKey: String): ChannelIntegration? = jpaRepository.findByPublicKey(publicKey)?.toDomain()

    /**
     * Lists channel integrations through JPA.
     */
    override fun findByChannelId(channelId: String): List<ChannelIntegration> =
        jpaRepository.findByChannelId(channelId).map { it.toDomain() }

    /**
     * Returns true when a channel has an integration matching type and status.
     */
    override fun existsByChannelIdAndTypeAndStatus(
        channelId: String,
        type: ChannelIntegrationType,
        status: ChannelIntegrationStatus,
    ): Boolean =
        jpaRepository.existsByChannelIdAndTypeAndStatus(
            channelId = channelId,
            type = type.name,
            status = status.name,
        )

    /**
     * Returns true when another integration matches type and status.
     */
    override fun existsByChannelIdAndTypeAndStatusAndIdNot(
        channelId: String,
        type: ChannelIntegrationType,
        status: ChannelIntegrationStatus,
        excludedId: String,
    ): Boolean =
        jpaRepository.existsByChannelIdAndTypeAndStatusAndIdNot(
            channelId = channelId,
            type = type.name,
            status = status.name,
            id = excludedId,
        )

    /**
     * Maps a channel integration domain object to a JPA entity.
     */
    private fun ChannelIntegration.toEntity(): ChannelIntegrationEntity =
        ChannelIntegrationEntity(
            id = id,
            channelId = channelId,
            type = type.name,
            publicKey = publicKey,
            secretHash = secretHash,
            status = status.name,
            allowedOrigins = objectMapper.writeValueAsString(allowedOrigins.values),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    /**
     * Maps a channel integration JPA entity to a domain object.
     */
    private fun ChannelIntegrationEntity.toDomain(): ChannelIntegration =
        ChannelIntegration(
            id = id,
            channelId = channelId,
            type = ChannelIntegrationType.valueOf(type),
            publicKey = publicKey,
            secretHash = secretHash,
            status = ChannelIntegrationStatus.valueOf(status),
            allowedOrigins = AllowedOrigins.of(objectMapper.readValue(allowedOrigins, stringListType)),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
