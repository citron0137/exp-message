package site.rahoon.message.monolithic.core.conversation.application.facade

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenGenerator
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenHasher
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.domain.Visitor
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

@ConfigurationProperties(prefix = "core.conversation.visitor-session")
data class VisitorSessionProperties(
    val ttlSeconds: Long = 604800,
)

@Service
class WidgetEntryFacade(
    private val widgetAccessPolicy: WidgetAccessPolicy,
    private val visitorRepository: VisitorRepository,
    private val visitorSessionRepository: VisitorSessionRepository,
    private val channelConversationRepository: ChannelConversationRepository,
    private val visitorSessionTokenGenerator: VisitorSessionTokenGenerator,
    private val visitorSessionTokenHasher: VisitorSessionTokenHasher,
    private val visitorSessionPolicy: VisitorSessionPolicy,
    private val visitorSessionProperties: VisitorSessionProperties,
) {
    /**
     * Creates a visitor and visitor session for an accessible widget.
     */
    @Transactional
    fun createVisitorSession(command: CreateWidgetVisitorSessionCommand): WidgetVisitorSessionResult {
        val access = widgetAccessPolicy.requireAccessibleWidget(command.publicKey, command.origin)
        val visitor =
            visitorRepository.save(
                Visitor.create(
                    channelId = access.channel.id,
                    externalId = command.externalId,
                    displayName = command.displayName,
                    email = command.email,
                    metadata = command.metadata,
                ),
            )
        val rawToken = visitorSessionTokenGenerator.generate()
        val expiresAt = LocalDateTime.now().plusSeconds(visitorSessionProperties.ttlSeconds)
        val session =
            visitorSessionRepository.save(
                VisitorSession.create(
                    visitorId = visitor.id,
                    channelId = access.channel.id,
                    tokenHash = visitorSessionTokenHasher.hash(rawToken),
                    expiresAt = expiresAt,
                ),
            )
        return WidgetVisitorSessionResult(
            visitor = VisitorResult.from(visitor),
            session =
                VisitorSessionResult(
                    token = rawToken,
                    expiresAt = session.expiresAt,
                ),
        )
    }

    /**
     * Enters a channel conversation with a valid visitor session.
     */
    @Transactional
    fun enterConversation(command: EnterWidgetConversationCommand): WidgetConversationEntryResult {
        val access = widgetAccessPolicy.requireAccessibleWidget(command.publicKey, command.origin)
        val session = visitorSessionPolicy.requireValidSession(command.visitorSessionToken, access.channel.id)
        val visitor =
            visitorRepository.findById(session.visitorId)
                ?: throw ConversationException(
                    error = ConversationError.VISITOR_NOT_FOUND,
                    details = mapOf("visitorId" to session.visitorId),
                )
        val reusableConversation = channelConversationRepository.findReusableByChannelIdAndVisitorId(access.channel.id, visitor.id)
        val conversation =
            if (reusableConversation != null) {
                reusableConversation.reactivateAsPending().let { reactivated ->
                    if (reactivated != reusableConversation) {
                        channelConversationRepository.save(reactivated)
                    } else {
                        reactivated
                    }
                }
            } else {
                channelConversationRepository.save(
                    ChannelConversation.start(
                        channelId = access.channel.id,
                        visitorId = visitor.id,
                    ),
                )
            }
        visitorSessionRepository.save(session.touch())
        return WidgetConversationEntryResult(
            visitor = VisitorResult.from(visitor),
            conversation = ChannelConversationResult.from(conversation),
        )
    }
}

data class CreateWidgetVisitorSessionCommand(
    val publicKey: String,
    val origin: String,
    val externalId: String?,
    val displayName: String?,
    val email: String?,
    val metadata: Map<String, String>,
)

data class EnterWidgetConversationCommand(
    val publicKey: String,
    val origin: String,
    val visitorSessionToken: String,
)

data class WidgetVisitorSessionResult(
    val visitor: VisitorResult,
    val session: VisitorSessionResult,
)

data class WidgetConversationEntryResult(
    val visitor: VisitorResult,
    val conversation: ChannelConversationResult,
)

data class VisitorResult(
    val id: String,
    val channelId: String,
    val externalId: String?,
    val displayName: String?,
    val email: String?,
    val metadata: Map<String, String>,
) {
    companion object {
        /**
         * Maps a visitor domain object to an application result.
         */
        fun from(visitor: Visitor): VisitorResult =
            VisitorResult(
                id = visitor.id,
                channelId = visitor.channelId,
                externalId = visitor.externalId,
                displayName = visitor.displayName,
                email = visitor.email,
                metadata = visitor.metadata,
            )
    }
}

data class VisitorSessionResult(
    val token: String,
    val expiresAt: LocalDateTime,
)

data class ChannelConversationResult(
    val id: String,
    val channelId: String,
    val visitorId: String,
    val status: ChannelConversationStatus,
) {
    companion object {
        /**
         * Maps a channel conversation domain object to an application result.
         */
        fun from(conversation: ChannelConversation): ChannelConversationResult =
            ChannelConversationResult(
                id = conversation.id,
                channelId = conversation.channelId,
                visitorId = conversation.visitorId,
                status = conversation.status,
            )
    }
}
