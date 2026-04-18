package site.rahoon.message.monolithic.core.conversation.application.service

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException

@Service
class ConversationVisitorAccessPolicy(
    private val channelConversationRepository: ChannelConversationRepository,
) {
    /**
     * Requires a conversation that can accept a visitor message.
     */
    fun requireAppendableConversation(
        conversationId: String,
        session: VisitorSession,
    ): ChannelConversation {
        val conversation = requireOwnedConversation(conversationId, session)
        if (!conversation.canAcceptVisitorMessage()) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_NOT_APPENDABLE,
                details = mapOf("conversationId" to conversation.id, "status" to conversation.status.name),
            )
        }
        return conversation
    }

    /**
     * Requires a conversation that can be viewed by a visitor.
     */
    fun requireReadableConversation(
        conversationId: String,
        session: VisitorSession,
    ): ChannelConversation {
        val conversation = requireOwnedConversation(conversationId, session)
        if (!conversation.canBeViewedByVisitor()) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_NOT_VIEWABLE,
                details = mapOf("conversationId" to conversation.id, "status" to conversation.status.name),
            )
        }
        return conversation
    }

    /**
     * Requires that the conversation belongs to the visitor session.
     */
    private fun requireOwnedConversation(
        conversationId: String,
        session: VisitorSession,
    ): ChannelConversation {
        val conversation =
            channelConversationRepository.findById(conversationId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                    details = mapOf("conversationId" to conversationId),
                )
        if (conversation.channelId != session.channelId || conversation.visitorId != session.visitorId) {
            throw ConversationException(
                error = ConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                details = mapOf("conversationId" to conversationId),
            )
        }
        return conversation
    }
}
