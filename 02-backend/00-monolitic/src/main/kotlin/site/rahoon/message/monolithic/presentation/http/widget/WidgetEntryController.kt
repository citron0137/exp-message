package site.rahoon.message.monolithic.presentation.http.widget

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelConversationResult
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateWidgetVisitorSessionCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.EnterWidgetConversationCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.VisitorResult
import site.rahoon.message.monolithic.core.conversation.application.facade.VisitorSessionResult
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetConversationEntryResult
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetEntryFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetVisitorSessionResult
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/widget")
class WidgetEntryController(
    private val widgetEntryFacade: WidgetEntryFacade,
) {
    /**
     * Creates a visitor session for a public widget.
     */
    @PostMapping("/visitor-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVisitorSession(
        servletRequest: HttpServletRequest,
        @Valid @RequestBody request: WidgetEntryRequest.CreateVisitorSession,
    ): ApiResponse<WidgetEntryResponse.CreateVisitorSession> {
        val result =
            widgetEntryFacade.createVisitorSession(
                CreateWidgetVisitorSessionCommand(
                    publicKey = request.publicKey,
                    origin = resolveOrigin(servletRequest, request.origin),
                    externalId = request.visitor?.externalId,
                    displayName = request.visitor?.displayName,
                    email = request.visitor?.email,
                    metadata = request.visitor?.metadata.orEmpty(),
                ),
            )
        return ApiResponse.success(WidgetEntryResponse.CreateVisitorSession.from(result))
    }

    /**
     * Enters or creates a widget conversation for a visitor session.
     */
    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    fun enterConversation(
        servletRequest: HttpServletRequest,
        @Valid @RequestBody request: WidgetEntryRequest.EnterConversation,
    ): ApiResponse<WidgetEntryResponse.EnterConversation> {
        val result =
            widgetEntryFacade.enterConversation(
                EnterWidgetConversationCommand(
                    publicKey = request.publicKey,
                    origin = resolveOrigin(servletRequest, request.origin),
                    visitorSessionToken = request.visitorSessionToken,
                ),
            )
        return ApiResponse.success(WidgetEntryResponse.EnterConversation.from(result))
    }

    /**
     * Resolves request origin with header precedence.
     */
    private fun resolveOrigin(
        servletRequest: HttpServletRequest,
        fallbackOrigin: String?,
    ): String = servletRequest.getHeader("Origin") ?: fallbackOrigin.orEmpty()
}

object WidgetEntryRequest {
    data class CreateVisitorSession(
        @field:NotBlank
        val publicKey: String,
        val origin: String? = null,
        val visitor: Visitor? = null,
    )

    data class Visitor(
        val externalId: String? = null,
        val displayName: String? = null,
        val email: String? = null,
        val metadata: Map<String, String> = emptyMap(),
    )

    data class EnterConversation(
        @field:NotBlank
        val publicKey: String,
        val origin: String? = null,
        @field:NotBlank
        val visitorSessionToken: String,
    )
}

object WidgetEntryResponse {
    data class CreateVisitorSession(
        val visitor: Visitor,
        val session: VisitorSession,
    ) {
        companion object {
            /**
             * Maps a visitor session result to a response.
             */
            fun from(result: WidgetVisitorSessionResult): CreateVisitorSession =
                CreateVisitorSession(
                    visitor = Visitor.from(result.visitor),
                    session = VisitorSession.from(result.session),
                )
        }
    }

    data class EnterConversation(
        val visitor: Visitor,
        val conversation: Conversation,
    ) {
        companion object {
            /**
             * Maps a widget conversation entry result to a response.
             */
            fun from(result: WidgetConversationEntryResult): EnterConversation =
                EnterConversation(
                    visitor = Visitor.from(result.visitor),
                    conversation = Conversation.from(result.conversation),
                )
        }
    }

    data class Visitor(
        val id: String,
        val channelId: String,
        val externalId: String?,
        val displayName: String?,
        val email: String?,
        val metadata: Map<String, String>,
    ) {
        companion object {
            /**
             * Maps a visitor result to a response.
             */
            fun from(result: VisitorResult): Visitor =
                Visitor(
                    id = result.id,
                    channelId = result.channelId,
                    externalId = result.externalId,
                    displayName = result.displayName,
                    email = result.email,
                    metadata = result.metadata,
                )
        }
    }

    data class VisitorSession(
        val token: String,
        val expiresAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Maps a visitor session result to a response.
             */
            fun from(result: VisitorSessionResult): VisitorSession =
                VisitorSession(
                    token = result.token,
                    expiresAt = result.expiresAt,
                )
        }
    }

    data class Conversation(
        val id: String,
        val channelId: String,
        val visitorId: String,
        val status: String,
    ) {
        companion object {
            /**
             * Maps a channel conversation result to a response.
             */
            fun from(result: ChannelConversationResult): Conversation =
                Conversation(
                    id = result.id,
                    channelId = result.channelId,
                    visitorId = result.visitorId,
                    status = result.status.name,
                )
        }
    }
}
