package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.Channel

interface ChannelRepository {
    /**
     * Saves a channel.
     */
    fun save(channel: Channel): Channel

    /**
     * Finds a channel by identifier.
     */
    fun findById(id: String): Channel?

    /**
     * Lists all channels.
     */
    fun findAll(): List<Channel>
}
