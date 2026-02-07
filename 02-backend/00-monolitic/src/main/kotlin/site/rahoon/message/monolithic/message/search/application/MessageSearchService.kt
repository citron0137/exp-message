package site.rahoon.message.monolithic.message.search.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.message.domain.Message
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.search.infrastructure.MessageDocument
import site.rahoon.message.monolithic.message.search.infrastructure.MessageSearchRepository

private val logger = KotlinLogging.logger {}

/**
 * Application Service for message search operations.
 *
 * This service provides business logic for:
 * 1. Indexing messages into Elasticsearch
 * 2. Searching messages by keyword
 * 3. Managing the search index
 *
 * ## Architecture:
 * ```
 * Controller -> Service (this) -> Repository -> Elasticsearch
 * ```
 *
 * ## Indexing Flow:
 * When a message is created in MySQL:
 * 1. MessageApplicationService saves to MySQL
 * 2. Call indexMessage() to index in Elasticsearch
 * 3. Message is now searchable
 *
 * ## Search Flow:
 * 1. User calls search API with keyword
 * 2. Controller calls searchMessages()
 * 3. Repository queries Elasticsearch
 * 4. Results returned with relevance scores
 *
 * ## Important Notes:
 * - Elasticsearch is eventually consistent (~1 second delay)
 * - Index and MySQL can get out of sync (handled in Level 3)
 * - Search returns MessageDocument, not domain Message
 *
 * @property messageSearchRepository Repository for Elasticsearch operations
 */
@Service
class MessageSearchService(
    private val messageSearchRepository: MessageSearchRepository
) {

    /**
     * Index a message for search.
     *
     * Call this method when a new message is created to make it searchable.
     * The message will be converted to a MessageDocument and saved to Elasticsearch.
     *
     * ## When to call:
     * - After successfully saving a message to MySQL
     * - During batch re-indexing of existing messages
     *
     * ## Example:
     * ```kotlin
     * // In MessageApplicationService.createMessage()
     * val message = messageDomainService.create(command)
     * messageSearchService.indexMessage(message)
     * ```
     *
     * ## Failure Handling (Level 1):
     * Currently, if indexing fails, we log and continue.
     * In production (Level 3), consider:
     * - Retry mechanism
     * - Dead letter queue
     * - Eventual consistency via events
     *
     * @param message The domain Message to index
     * @return The indexed MessageDocument
     */
    fun indexMessage(message: Message): MessageDocument {
        logger.debug { "Indexing message: id=${message.id}, roomId=${message.chatRoomId}" }

        // Convert domain Message to Elasticsearch Document
        val document = MessageDocument.from(message)

        // Save to Elasticsearch index
        val indexed = messageSearchRepository.save(document)

        logger.info { "Message indexed successfully: id=${indexed.id}" }
        return indexed
    }

    /**
     * Index a message for search using MessageEvent.Created.
     *
     * This method exists because the current message creation flow publishes a MessageEvent.Created
     * through Spring's ApplicationEventPublisher.
     *
     * Using the event has two benefits:
     * 1. It avoids passing domain objects across layers/components.
     * 2. It aligns with the existing event-driven architecture (e.g., Redis relay).
     *
     * @param event The message created event containing all data needed for indexing
     * @return The indexed MessageDocument
     */
    fun indexMessage(event: MessageEvent.Created): MessageDocument {
        logger.debug { "Indexing message from event: id=${event.id}, roomId=${event.chatRoomId}" }

        val document =
            MessageDocument(
                id = event.id,
                chatRoomId = event.chatRoomId,
                userId = event.userId,
                content = event.content,
                createdAt = event.createdAt
            )

        val indexed = messageSearchRepository.save(document)

        logger.info { "Message indexed successfully from event: id=${indexed.id}" }
        return indexed
    }

    /**
     * Search messages by keyword across all chat rooms.
     *
     * Performs full-text search on message content field.
     * Results are ordered by relevance score (most relevant first).
     *
     * ## How it works:
     * 1. Keyword is analyzed (lowercased, tokenized)
     * 2. Elasticsearch finds documents with matching tokens
     * 3. Documents are scored by relevance (tf-idf algorithm)
     * 4. Results returned in descending score order
     *
     * ## Example:
     * ```
     * searchMessages("hello")
     * -> Returns messages containing "hello", "Hello", "HELLO", etc.
     * ```
     *
     * ## Limitations (Level 1):
     * - No pagination (returns all matches)
     * - No highlighting
     * - Korean text not properly tokenized (needs Nori - Level 2)
     *
     * @param keyword The search term to look for
     * @return List of matching MessageDocuments, ordered by relevance
     */
    fun searchMessages(keyword: String): List<MessageDocument> {
        logger.debug { "Searching messages: keyword=$keyword" }

        if (keyword.isBlank()) {
            logger.warn { "Empty search keyword provided" }
            return emptyList()
        }

        val results = messageSearchRepository.findByContentContaining(keyword)

        logger.info { "Search completed: keyword=$keyword, results=${results.size}" }
        return results
    }

    /**
     * Search messages within a specific chat room.
     *
     * Combines full-text search with room filtering.
     * Only returns messages from the specified chat room.
     *
     * ## Query executed:
     * ```json
     * {
     *   "query": {
     *     "bool": {
     *       "must": [
     *         { "match": { "content": "<keyword>" } }
     *       ],
     *       "filter": [
     *         { "term": { "chatRoomId": "<chatRoomId>" } }
     *       ]
     *     }
     *   }
     * }
     * ```
     *
     * ## Filter vs Must:
     * - filter: Exact match, no scoring impact, cached
     * - must: Affects relevance score
     *
     * @param keyword The search term to look for
     * @param chatRoomId The chat room to search within
     * @return List of matching messages in the room
     */
    fun searchMessagesInRoom(keyword: String, chatRoomId: String): List<MessageDocument> {
        logger.debug { "Searching in room: keyword=$keyword, roomId=$chatRoomId" }

        if (keyword.isBlank()) {
            logger.warn { "Empty search keyword provided" }
            return emptyList()
        }

        val results = messageSearchRepository.findByChatRoomIdAndContentContaining(
            chatRoomId = chatRoomId,
            keyword = keyword
        )

        logger.info { "Room search completed: roomId=$chatRoomId, keyword=$keyword, results=${results.size}" }
        return results
    }

    /**
     * Delete a message from the search index.
     *
     * Call this when a message is deleted from MySQL to keep indexes in sync.
     *
     * ## When to call:
     * - After successfully deleting a message from MySQL
     * - During cleanup operations
     *
     * @param messageId The ID of the message to remove from index
     */
    fun deleteMessage(messageId: String) {
        logger.debug { "Deleting message from index: id=$messageId" }

        messageSearchRepository.deleteById(messageId)

        logger.info { "Message removed from index: id=$messageId" }
    }

    /**
     * Bulk index multiple messages.
     *
     * More efficient than indexing one by one.
     * Use for initial data migration or re-indexing.
     *
     * ## Performance:
     * - Batches requests to Elasticsearch
     * - Reduces network round trips
     * - Use for initial migration from MySQL to ES
     *
     * @param messages List of domain Messages to index
     * @return List of indexed MessageDocuments
     */
    fun bulkIndexMessages(messages: List<Message>): List<MessageDocument> {
        logger.debug { "Bulk indexing ${messages.size} messages" }

        if (messages.isEmpty()) {
            return emptyList()
        }

        val documents = messages.map { MessageDocument.from(it) }
        val indexed = messageSearchRepository.saveAll(documents).toList()

        logger.info { "Bulk indexing completed: ${indexed.size} messages indexed" }
        return indexed
    }
}
