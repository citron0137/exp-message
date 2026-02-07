package site.rahoon.message.monolithic.message.search.controller

import io.swagger.v3.oas.annotations.media.Schema
import site.rahoon.message.monolithic.message.search.infrastructure.MessageDocument
import java.time.LocalDateTime

/**
 * Response DTO for message search results.
 *
 * This class represents a single search result returned to the client.
 * It contains the essential message information needed for display.
 *
 * ## Why a separate Response class?
 * 1. API stability: Internal model changes don't break API contract
 * 2. Security: Control exactly what data is exposed
 * 3. Documentation: OpenAPI annotations for Swagger UI
 *
 * ## Field selection:
 * - All fields from MessageDocument are included (Level 1)
 * - In Level 2, we might add: highlight, score, etc.
 *
 * @property id Message unique identifier
 * @property chatRoomId Chat room this message belongs to
 * @property userId User who sent the message
 * @property content Message text content
 * @property createdAt When the message was created
 */
@Schema(description = "Message search result")
data class MessageSearchResponse(
    @Schema(
        description = "Unique message identifier",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    val id: String,

    @Schema(
        description = "Chat room identifier",
        example = "room-123"
    )
    val chatRoomId: String,

    @Schema(
        description = "Message sender's user ID",
        example = "user-456"
    )
    val userId: String,

    @Schema(
        description = "Message content (the search matched this field)",
        example = "Hello, how are you?"
    )
    val content: String,

    @Schema(
        description = "Message creation timestamp",
        example = "2025-01-15T10:30:00"
    )
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * Convert MessageDocument to response DTO.
         *
         * @param document The Elasticsearch document to convert
         * @return Response DTO for API response
         */
        fun from(document: MessageDocument): MessageSearchResponse =
            MessageSearchResponse(
                id = document.id,
                chatRoomId = document.chatRoomId,
                userId = document.userId,
                content = document.content,
                createdAt = document.createdAt
            )
    }
}

/**
 * Wrapper response for search results with metadata.
 *
 * In Level 2, this could include:
 * - Total count
 * - Pagination cursors
 * - Search metadata (took time, etc.)
 *
 * @property results List of search results
 * @property count Number of results returned
 */
@Schema(description = "Search results wrapper")
data class MessageSearchListResponse(
    @Schema(description = "List of matching messages")
    val results: List<MessageSearchResponse>,

    @Schema(
        description = "Number of results returned",
        example = "10"
    )
    val count: Int
) {
    companion object {
        /**
         * Create response from list of documents.
         *
         * @param documents List of Elasticsearch documents
         * @return Wrapped response with results and count
         */
        fun from(documents: List<MessageDocument>): MessageSearchListResponse =
            MessageSearchListResponse(
                results = documents.map { MessageSearchResponse.from(it) },
                count = documents.size
            )
    }
}
