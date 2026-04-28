package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation

interface ChannelConversationRepository {
    /**
     * Saves a channel conversation.
     */
    fun save(conversation: ChannelConversation): ChannelConversation

    /**
     * Finds a channel conversation by id.
     */
    fun findById(id: String): ChannelConversation?

    /**
     * Finds a reusable conversation for a channel and visitor.
     */
    fun findReusableByChannelIdAndVisitorId(
        channelId: String,
        visitorId: String,
    ): ChannelConversation?
}
