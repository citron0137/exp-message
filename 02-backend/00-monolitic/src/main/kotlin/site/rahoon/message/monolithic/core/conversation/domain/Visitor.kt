package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class Visitor(
    val id: String,
    val channelId: String,
    val externalId: String?,
    val displayName: String?,
    val email: String?,
    val metadata: Map<String, String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * Creates a visitor inside a channel.
         */
        fun create(
            channelId: String,
            externalId: String?,
            displayName: String?,
            email: String?,
            metadata: Map<String, String>,
        ): Visitor {
            val now = LocalDateTime.now()
            return Visitor(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                externalId = externalId?.trim()?.takeIf { it.isNotBlank() },
                displayName = displayName?.trim()?.takeIf { it.isNotBlank() },
                email = email?.trim()?.takeIf { it.isNotBlank() },
                metadata = metadata.filterKeys { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
