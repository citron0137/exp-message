package site.rahoon.message.monolithic.core.conversation.domain

import java.time.LocalDateTime
import java.util.UUID

data class Channel(
    val id: String,
    val name: String,
    val status: ChannelStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * Creates a new active channel.
         */
        fun create(name: String): Channel {
            val now = LocalDateTime.now()
            return Channel(
                id = UUID.randomUUID().toString(),
                name = name,
                status = ChannelStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
