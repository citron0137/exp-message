package site.rahoon.message.monolithic.message.search.infrastructure

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import site.rahoon.message.monolithic.message.domain.Message
import java.time.LocalDateTime

/**
 * Elasticsearch Document representing a chat message for full-text search.
 *
 * This class maps to an Elasticsearch index named "messages". Each instance
 * represents a single document in the index.
 *
 * ## Index vs Document:
 * - Index: Like a database table, stores all message documents
 * - Document: Like a row, represents one message
 *
 * ## Field Types:
 * - FieldType.Keyword: For exact matching (IDs, filtering, sorting)
 *   - NOT analyzed, stored as-is
 *   - Use for: chatRoomId, userId (exact match filters)
 *
 * - FieldType.Text: For full-text search
 *   - Analyzed into tokens (words)
 *   - Use for: content (searchable text)
 *
 * - FieldType.Date: For date/time values
 *   - Enables date range queries
 *   - Use for: createdAt (time-based filtering)
 *
 * ## Why separate from JPA Entity?
 * 1. Different concerns: JPA for persistence, ES for search
 * 2. Different field types: JPA Column vs ES Field annotations
 * 3. Flexibility: Can index only fields needed for search
 *
 * @property id Unique identifier (same as Message.id for correlation)
 * @property chatRoomId Chat room identifier for filtering searches
 * @property userId Message sender identifier
 * @property content Message text content (full-text searchable)
 * @property createdAt Message creation timestamp
 *
 * @see Message Domain model this document is derived from
 */
@Document(indexName = "messages")
data class MessageDocument(
    /**
     * Unique document identifier.
     * Uses the same ID as the Message domain entity for easy correlation.
     * Elasticsearch uses this as the document's _id field.
     */
    @Id
    val id: String,

    /**
     * Chat room identifier for filtering search results.
     * Keyword type enables exact matching: "room-123" matches only "room-123"
     */
    @Field(type = FieldType.Keyword)
    val chatRoomId: String,

    /**
     * User identifier of the message sender.
     * Keyword type for exact matching and aggregations.
     */
    @Field(type = FieldType.Keyword)
    val userId: String,

    /**
     * Message content for full-text search.
     * Text type means this field is analyzed:
     * - "Hello world" becomes ["hello", "world"]
     * - Search for "hello" will find this document
     *
     * Note: Standard analyzer is used by default.
     * Korean text won't be properly tokenized until Nori is added (Level 2).
     */
    @Field(type = FieldType.Text)
    val content: String,

    /**
     * Message creation timestamp.
     * Date type enables range queries: find messages from last 7 days.
     * Format specifies how the date is serialized.
     */
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis])
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * Factory method to create MessageDocument from domain Message.
         *
         * Use this when indexing a newly created message:
         * ```
         * val message = Message.create(roomId, userId, content)
         * val document = MessageDocument.from(message)
         * repository.save(document)
         * ```
         *
         * @param message The domain Message to convert
         * @return A new MessageDocument ready for indexing
         */
        fun from(message: Message): MessageDocument =
            MessageDocument(
                id = message.id,
                chatRoomId = message.chatRoomId,
                userId = message.userId,
                content = message.content,
                createdAt = message.createdAt
            )
    }
}
