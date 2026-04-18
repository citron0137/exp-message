package site.rahoon.message.monolithic.presentation.websocket.widget

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Controller
import site.rahoon.message.monolithic.core.conversation.application.facade.ConversationMessageResult
import site.rahoon.message.monolithic.core.conversation.application.facade.SendWidgetVisitorMessageCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetMessageFacade
import java.time.LocalDateTime

@Controller
class WidgetMessageWebSocketController(
    private val widgetMessageFacade: WidgetMessageFacade,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    /**
     * Stores and broadcasts a visitor message sent through WebSocket.
     */
    @MessageMapping("widget/conversations/{conversationId}/messages")
    fun sendMessage(
        accessor: StompHeaderAccessor,
        @DestinationVariable conversationId: String,
        @Valid @Payload request: WidgetMessageWebSocketRequest.SendMessage,
    ): WidgetMessageWebSocketResponse.Message {
        val session = WidgetWebSocketSessionAccessor.require(accessor)
        val result =
            widgetMessageFacade.sendVisitorMessage(
                SendWidgetVisitorMessageCommand(
                    publicKey = session.publicKey,
                    origin = session.origin,
                    visitorSessionToken = session.visitorSessionToken,
                    conversationId = conversationId,
                    clientMessageId = request.clientMessageId,
                    content = request.content,
                ),
            )
        val response = WidgetMessageWebSocketResponse.Message.from(result)
        messagingTemplate.convertAndSend(
            "/topic/widget/conversations/$conversationId/messages",
            response,
        )
        return response
    }
}

object WidgetMessageWebSocketRequest {
    data class SendMessage(
        @field:NotBlank
        val clientMessageId: String,
        @field:NotBlank
        val content: String,
    )
}

object WidgetMessageWebSocketResponse {
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
             * Maps an application message result to a WebSocket response.
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
