package site.rahoon.message.monolithic.presentation.http.widget

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.facade.ConversationMessageResult
import site.rahoon.message.monolithic.core.conversation.application.facade.SendWidgetVisitorMessageCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetMessageFacade
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetMessageListQuery
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetMessageListResult
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetMessageQueryService
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import site.rahoon.message.monolithic.presentation.websocket.admin.AdminConversationWebSocketResponse
import site.rahoon.message.monolithic.presentation.websocket.admin.AdminConversationWebSocketTopics
import site.rahoon.message.monolithic.presentation.websocket.widget.WidgetMessageWebSocketResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/widget/conversations/{conversationId}/messages")
class WidgetMessageController(
    private val widgetMessageFacade: WidgetMessageFacade,
    private val widgetMessageQueryService: WidgetMessageQueryService,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    /**
     * Sends a visitor message through the widget HTTP API.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun sendMessage(
        servletRequest: HttpServletRequest,
        @PathVariable conversationId: String,
        @Valid @RequestBody request: WidgetMessageRequest.SendMessage,
    ): ApiResponse<WidgetMessageResponse.Message> {
        val result =
            widgetMessageFacade.sendVisitorMessage(
                SendWidgetVisitorMessageCommand(
                    publicKey = request.publicKey,
                    origin = resolveOrigin(servletRequest, request.origin),
                    visitorSessionToken = request.visitorSessionToken,
                    conversationId = conversationId,
                    clientMessageId = request.clientMessageId,
                    content = request.content,
                ),
            )
        /*
         * HTTP is kept as a recovery path for widgets that cannot use STOMP. Stored messages
         * still need to fan out to open widget tabs and admin consoles so every client observes
         * the same canonical message log regardless of the transport used to create it.
         */
        messagingTemplate.convertAndSend(
            "/topic/widget/conversations/$conversationId/messages",
            WidgetMessageWebSocketResponse.Message.from(result),
        )
        messagingTemplate.convertAndSend(
            AdminConversationWebSocketTopics.channelConversations(result.channelId),
            AdminConversationWebSocketResponse.ConversationChanged.from(result, "VISITOR_MESSAGE_SENT"),
        )
        messagingTemplate.convertAndSend(
            AdminConversationWebSocketTopics.conversationMessages(result.channelId, conversationId),
            AdminConversationWebSocketResponse.Message.from(result),
        )
        return ApiResponse.success(WidgetMessageResponse.Message.from(result))
    }

    /**
     * Lists visitor-readable widget messages.
     */
    @GetMapping
    fun listMessages(
        servletRequest: HttpServletRequest,
        @PathVariable conversationId: String,
        @RequestParam publicKey: String,
        @RequestParam(required = false) origin: String?,
        @RequestParam(defaultValue = "0") afterSequence: Long,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ApiResponse<WidgetMessageResponse.MessageList> {
        val result =
            widgetMessageQueryService.listMessages(
                WidgetMessageListQuery(
                    publicKey = publicKey,
                    origin = resolveOrigin(servletRequest, origin),
                    visitorSessionToken = servletRequest.getHeader(VISITOR_SESSION_HEADER).orEmpty(),
                    conversationId = conversationId,
                    afterSequence = afterSequence,
                    limit = limit,
                ),
            )
        return ApiResponse.success(WidgetMessageResponse.MessageList.from(result))
    }

    /**
     * Resolves request origin with header precedence.
     */
    private fun resolveOrigin(
        servletRequest: HttpServletRequest,
        fallbackOrigin: String?,
    ): String = servletRequest.getHeader("Origin") ?: fallbackOrigin.orEmpty()

    companion object {
        private const val VISITOR_SESSION_HEADER = "X-Visitor-Session"
    }
}

object WidgetMessageRequest {
    data class SendMessage(
        @field:NotBlank
        val publicKey: String,
        val origin: String? = null,
        @field:NotBlank
        val visitorSessionToken: String,
        @field:NotBlank
        val clientMessageId: String,
        @field:NotBlank
        val content: String,
    )
}

object WidgetMessageResponse {
    data class MessageList(
        val messages: List<Message>,
        val nextAfterSequence: Long,
        val hasMore: Boolean,
    ) {
        companion object {
            /**
             * Maps an application message list result to a response.
             */
            fun from(result: WidgetMessageListResult): MessageList =
                MessageList(
                    messages = result.messages.map { Message.from(it) },
                    nextAfterSequence = result.nextAfterSequence,
                    hasMore = result.hasMore,
                )
        }
    }

    data class Message(
        val id: String,
        val conversationId: String,
        val channelId: String,
        val sequence: Long,
        val senderType: String,
        val senderId: String,
        val clientMessageId: String,
        val type: String,
        val content: String,
        val status: String,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Maps an application message result to a response.
             */
            fun from(result: ConversationMessageResult): Message =
                Message(
                    id = result.id,
                    conversationId = result.conversationId,
                    channelId = result.channelId,
                    sequence = result.sequence,
                    senderType = result.senderType.name,
                    senderId = result.senderId,
                    clientMessageId = result.clientMessageId,
                    type = result.type.name,
                    content = result.content,
                    status = result.status.name,
                    createdAt = result.createdAt,
                )
        }
    }
}
