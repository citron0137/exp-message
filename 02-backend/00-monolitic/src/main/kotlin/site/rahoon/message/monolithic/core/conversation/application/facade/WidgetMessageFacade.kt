package site.rahoon.message.monolithic.core.conversation.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ConversationMessageRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionRepository
import site.rahoon.message.monolithic.core.conversation.application.service.ConversationVisitorAccessPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessage
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageSenderType
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageStatus
import site.rahoon.message.monolithic.core.conversation.domain.ConversationMessageType
import site.rahoon.message.monolithic.core.conversation.domain.MessageContent
import java.time.LocalDateTime

@Service
class WidgetMessageFacade(
    private val widgetAccessPolicy: WidgetAccessPolicy,
    private val visitorSessionPolicy: VisitorSessionPolicy,
    private val conversationVisitorAccessPolicy: ConversationVisitorAccessPolicy,
    private val channelConversationRepository: ChannelConversationRepository,
    private val conversationMessageRepository: ConversationMessageRepository,
    private val visitorSessionRepository: VisitorSessionRepository,
) {
    /**
     * Stores a visitor message through the transport-neutral write path.
     */
    @Transactional
    fun sendVisitorMessage(command: SendWidgetVisitorMessageCommand): ConversationMessageResult {
        val access = widgetAccessPolicy.requireAccessibleWidget(command.publicKey, command.origin)
        val session = visitorSessionPolicy.requireValidSession(command.visitorSessionToken, access.channel.id)
        val conversation = conversationVisitorAccessPolicy.requireAppendableConversation(command.conversationId, session)
        val existingMessage =
            conversationMessageRepository.findByIdempotencyKey(
                conversationId = command.conversationId,
                senderType = ConversationMessageSenderType.VISITOR,
                senderId = session.visitorId,
                clientMessageId = command.clientMessageId,
            )
        if (existingMessage != null) {
            visitorSessionRepository.save(session.touch())
            return ConversationMessageResult.from(existingMessage)
        }
        val sequenceIssue = conversation.issueNextMessageSequence()
        val message =
            ConversationMessage.visitorText(
                conversationId = conversation.id,
                channelId = conversation.channelId,
                visitorId = session.visitorId,
                sequence = sequenceIssue.sequence,
                clientMessageId = command.clientMessageId,
                content = MessageContent.text(command.content),
            )
        channelConversationRepository.save(sequenceIssue.conversation)
        val savedMessage = conversationMessageRepository.save(message)
        visitorSessionRepository.save(session.touch())
        return ConversationMessageResult.from(savedMessage)
    }
}

data class SendWidgetVisitorMessageCommand(
    val publicKey: String,
    val origin: String,
    val visitorSessionToken: String,
    val conversationId: String,
    val clientMessageId: String,
    val content: String,
)

data class ConversationMessageResult(
    val id: String,
    val conversationId: String,
    val channelId: String,
    val sequence: Long,
    val senderType: ConversationMessageSenderType,
    val senderId: String,
    val clientMessageId: String,
    val type: ConversationMessageType,
    val content: String,
    val status: ConversationMessageStatus,
    val createdAt: LocalDateTime,
) {
    companion object {
        /**
         * Maps a conversation message domain object to an application result.
         */
        fun from(message: ConversationMessage): ConversationMessageResult =
            ConversationMessageResult(
                id = message.id,
                conversationId = message.conversationId,
                channelId = message.channelId,
                sequence = message.sequence,
                senderType = message.senderType,
                senderId = message.senderId,
                clientMessageId = message.clientMessageId,
                type = message.type,
                content = message.content.value,
                status = message.status,
                createdAt = message.createdAt,
            )
    }
}
