package site.rahoon.message.monolithic.core.conversation.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.facade.ConversationMessageResult
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ConversationVisitorAccessPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy

@Service
class WidgetMessageQueryService(
    private val widgetAccessPolicy: WidgetAccessPolicy,
    private val visitorSessionPolicy: VisitorSessionPolicy,
    private val conversationVisitorAccessPolicy: ConversationVisitorAccessPolicy,
    private val conversationMessageRepository: ConversationMessageRepository,
) {
    /**
     * Lists visible widget messages after the requested sequence.
     */
    @Transactional(readOnly = true)
    fun listMessages(query: WidgetMessageListQuery): WidgetMessageListResult {
        val access = widgetAccessPolicy.requireAccessibleWidget(query.publicKey, query.origin)
        val session = visitorSessionPolicy.requireValidSession(query.visitorSessionToken, access.channel.id)
        conversationVisitorAccessPolicy.requireReadableConversation(query.conversationId, session)
        val normalizedLimit = query.limit.coerceIn(1, MAX_LIMIT)
        val messages =
            conversationMessageRepository.findVisibleAfterSequence(
                conversationId = query.conversationId,
                afterSequence = query.afterSequence.coerceAtLeast(0),
                limit = normalizedLimit + 1,
            )
        val hasMore = messages.size > normalizedLimit
        val pageMessages = messages.take(normalizedLimit).map { ConversationMessageResult.from(it) }
        return WidgetMessageListResult(
            messages = pageMessages,
            nextAfterSequence = pageMessages.lastOrNull()?.sequence ?: query.afterSequence.coerceAtLeast(0),
            hasMore = hasMore,
        )
    }

    companion object {
        private const val MAX_LIMIT = 100
    }
}

data class WidgetMessageListQuery(
    val publicKey: String,
    val origin: String,
    val visitorSessionToken: String,
    val conversationId: String,
    val afterSequence: Long = 0,
    val limit: Int = 50,
)

data class WidgetMessageListResult(
    val messages: List<ConversationMessageResult>,
    val nextAfterSequence: Long,
    val hasMore: Boolean,
)
