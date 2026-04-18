package site.rahoon.message.monolithic.presentation.http.admin

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminConversationFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminConversationOperationResult
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeConversationAssigneeCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeConversationStatusCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.ConversationMessageResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationAssigneeResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationDetailQuery
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationDetailResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationLastMessageResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationListItemResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationListQuery
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationListResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationMessagesQuery
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationMessagesResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationQueryService
import site.rahoon.message.monolithic.core.conversation.application.query.AdminConversationVisitorResult
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/admin/channels/{channelId}/conversations")
class AdminConversationController(
    private val adminConversationQueryService: AdminConversationQueryService,
    private val adminConversationFacade: AdminConversationFacade,
) {
    /**
     * Lists conversations for the admin inbox.
     */
    @GetMapping
    fun listConversations(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @RequestParam(required = false) status: ChannelConversationStatus?,
        @RequestParam(required = false) assigneeMembershipId: String?,
        @RequestParam(defaultValue = "false") unassigned: Boolean,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ApiResponse<AdminConversationResponse.ConversationList> =
        ApiResponse.success(
            AdminConversationResponse.ConversationList.from(
                adminConversationQueryService.listConversations(
                    AdminConversationListQuery(
                        actor = principal,
                        channelId = channelId,
                        status = status,
                        assigneeMembershipId = assigneeMembershipId,
                        unassigned = unassigned,
                        cursor = cursor,
                        limit = limit,
                    ),
                ),
            ),
        )

    /**
     * Gets one conversation detail for the admin inbox.
     */
    @GetMapping("/{conversationId}")
    fun getConversation(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable conversationId: String,
    ): ApiResponse<AdminConversationResponse.ConversationDetail> =
        ApiResponse.success(
            AdminConversationResponse.ConversationDetail.from(
                adminConversationQueryService.getConversation(
                    AdminConversationDetailQuery(
                        actor = principal,
                        channelId = channelId,
                        conversationId = conversationId,
                    ),
                ),
            ),
        )

    /**
     * Lists messages for one admin inbox conversation.
     */
    @GetMapping("/{conversationId}/messages")
    fun listMessages(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable conversationId: String,
        @RequestParam(defaultValue = "0") afterSequence: Long,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ApiResponse<AdminConversationResponse.MessageList> =
        ApiResponse.success(
            AdminConversationResponse.MessageList.from(
                adminConversationQueryService.listMessages(
                    AdminConversationMessagesQuery(
                        actor = principal,
                        channelId = channelId,
                        conversationId = conversationId,
                        afterSequence = afterSequence,
                        limit = limit,
                    ),
                ),
            ),
        )

    /**
     * Changes a conversation status for admin operations.
     */
    @PatchMapping("/{conversationId}/status")
    fun changeStatus(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable conversationId: String,
        @Valid @RequestBody request: AdminConversationRequest.ChangeStatus,
    ): ApiResponse<AdminConversationResponse.OperationResult> =
        ApiResponse.success(
            AdminConversationResponse.OperationResult.from(
                adminConversationFacade.changeStatus(
                    ChangeConversationStatusCommand(
                        actor = principal,
                        channelId = channelId,
                        conversationId = conversationId,
                        status = request.status,
                    ),
                ),
            ),
        )

    /**
     * Changes a conversation assignee for admin operations.
     */
    @PatchMapping("/{conversationId}/assignee")
    fun changeAssignee(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable conversationId: String,
        @Valid @RequestBody request: AdminConversationRequest.ChangeAssignee,
    ): ApiResponse<AdminConversationResponse.OperationResult> =
        ApiResponse.success(
            AdminConversationResponse.OperationResult.from(
                adminConversationFacade.changeAssignee(
                    ChangeConversationAssigneeCommand(
                        actor = principal,
                        channelId = channelId,
                        conversationId = conversationId,
                        assigneeMembershipId = request.assigneeMembershipId,
                    ),
                ),
            ),
        )
}

object AdminConversationRequest {
    data class ChangeStatus(
        val status: ChannelConversationStatus,
    )

    data class ChangeAssignee(
        val assigneeMembershipId: String?,
    )
}

object AdminConversationResponse {
    data class ConversationList(
        val items: List<ConversationListItem>,
        val nextCursor: String?,
        val hasMore: Boolean,
    ) {
        companion object {
            /**
             * Maps an application list result to a response.
             */
            fun from(result: AdminConversationListResult): ConversationList =
                ConversationList(
                    items = result.items.map { ConversationListItem.from(it) },
                    nextCursor = result.nextCursor,
                    hasMore = result.hasMore,
                )
        }
    }

    data class ConversationListItem(
        val id: String,
        val channelId: String,
        val visitor: Visitor,
        val assignee: Assignee?,
        val status: String,
        val lastMessageSequence: Long,
        val lastMessageAt: LocalDateTime?,
        val lastMessage: LastMessage?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
        val closedAt: LocalDateTime?,
    ) {
        companion object {
            /**
             * Maps an application list item to a response.
             */
            fun from(result: AdminConversationListItemResult): ConversationListItem =
                ConversationListItem(
                    id = result.id,
                    channelId = result.channelId,
                    visitor = Visitor.from(result.visitor),
                    assignee = result.assignee?.let { Assignee.from(it) },
                    status = result.status.name,
                    lastMessageSequence = result.lastMessageSequence,
                    lastMessageAt = result.lastMessageAt,
                    lastMessage = result.lastMessage?.let { LastMessage.from(it) },
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt,
                    closedAt = result.closedAt,
                )
        }
    }

    data class ConversationDetail(
        val id: String,
        val channelId: String,
        val visitor: Visitor,
        val assignee: Assignee?,
        val status: String,
        val lastMessageSequence: Long,
        val lastMessageAt: LocalDateTime?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
        val closedAt: LocalDateTime?,
    ) {
        companion object {
            /**
             * Maps an application detail result to a response.
             */
            fun from(result: AdminConversationDetailResult): ConversationDetail =
                ConversationDetail(
                    id = result.id,
                    channelId = result.channelId,
                    visitor = Visitor.from(result.visitor),
                    assignee = result.assignee?.let { Assignee.from(it) },
                    status = result.status.name,
                    lastMessageSequence = result.lastMessageSequence,
                    lastMessageAt = result.lastMessageAt,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt,
                    closedAt = result.closedAt,
                )
        }
    }

    data class Visitor(
        val id: String,
        val externalId: String?,
        val displayName: String?,
        val email: String?,
    ) {
        companion object {
            /**
             * Maps a visitor result to a response.
             */
            fun from(result: AdminConversationVisitorResult): Visitor =
                Visitor(
                    id = result.id,
                    externalId = result.externalId,
                    displayName = result.displayName,
                    email = result.email,
                )
        }
    }

    data class Assignee(
        val membershipId: String,
        val userId: String,
        val role: String,
        val agentStatus: String,
    ) {
        companion object {
            /**
             * Maps an assignee result to a response.
             */
            fun from(result: AdminConversationAssigneeResult): Assignee =
                Assignee(
                    membershipId = result.membershipId,
                    userId = result.userId,
                    role = result.role,
                    agentStatus = result.agentStatus,
                )
        }
    }

    data class OperationResult(
        val id: String,
        val channelId: String,
        val visitorId: String,
        val status: String,
        val assigneeMembershipId: String?,
        val lastMessageSequence: Long,
        val lastMessageAt: LocalDateTime?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
        val closedAt: LocalDateTime?,
    ) {
        companion object {
            /**
             * Maps an admin conversation operation result to a response.
             */
            fun from(result: AdminConversationOperationResult): OperationResult =
                OperationResult(
                    id = result.id,
                    channelId = result.channelId,
                    visitorId = result.visitorId,
                    status = result.status.name,
                    assigneeMembershipId = result.assigneeMembershipId,
                    lastMessageSequence = result.lastMessageSequence,
                    lastMessageAt = result.lastMessageAt,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt,
                    closedAt = result.closedAt,
                )
        }
    }

    data class LastMessage(
        val id: String,
        val sequence: Long?,
        val senderType: String?,
        val content: String?,
        val createdAt: LocalDateTime?,
    ) {
        companion object {
            /**
             * Maps a last message result to a response.
             */
            fun from(result: AdminConversationLastMessageResult): LastMessage =
                LastMessage(
                    id = result.id,
                    sequence = result.sequence,
                    senderType = result.senderType,
                    content = result.content,
                    createdAt = result.createdAt,
                )
        }
    }

    data class MessageList(
        val messages: List<Message>,
        val nextAfterSequence: Long,
        val hasMore: Boolean,
    ) {
        companion object {
            /**
             * Maps an application message list result to a response.
             */
            fun from(result: AdminConversationMessagesResult): MessageList =
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
