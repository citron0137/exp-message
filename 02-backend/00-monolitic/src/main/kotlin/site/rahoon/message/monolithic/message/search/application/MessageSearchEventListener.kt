package site.rahoon.message.monolithic.message.search.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.domain.Message

private val logger = KotlinLogging.logger {}

/**
 * Event Listener for indexing messages to Elasticsearch.
 *
 * This component listens to MessageEvent.Created events and indexes
 * the message to Elasticsearch for full-text search.
 *
 * ## Workflow:
 * ```
 * 1. User sends message
 * 2. MessageApplicationService.create() saves to MySQL
 * 3. ApplicationEventPublisher publishes MessageEvent.Created
 * 4. This listener receives the event (after transaction commits)
 * 5. MessageSearchService indexes to Elasticsearch
 * ```
 *
 * ## Why @TransactionalEventListener?
 * - phase = AFTER_COMMIT: Only index after MySQL transaction succeeds
 * - If MySQL save fails, we don't want to index to ES
 * - Ensures data consistency between MySQL and ES
 *
 * ## Why @Async?
 * - ES indexing shouldn't block the API response
 * - User gets immediate response after MySQL save
 * - ES indexing happens in background thread
 *
 * ## Error Handling (Level 1 - Simple):
 * - If ES indexing fails, we log the error and continue
 * - Message is still in MySQL, just not searchable
 * - In Level 3, we'd add retry logic or dead letter queue
 *
 * @property messageSearchService Service for ES operations
 */
@Component
class MessageSearchEventListener(
    private val messageSearchService: MessageSearchService
) {

    /**
     * Handle message created event by indexing to Elasticsearch.
     *
     * This method is called asynchronously after the MySQL transaction commits.
     *
     * @param event The message created event containing message data
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onMessageCreated(event: MessageEvent.Created) {
        logger.debug { "Received MessageEvent.Created: id=${event.id}" }

        try {
            // Convert event to domain Message for indexing
            // Note: We reconstruct Message from event data since event doesn't carry domain object
            val message = Message(
                id = event.id,
                chatRoomId = event.chatRoomId,
                userId = event.userId,
                content = event.content,
                createdAt = event.createdAt
            )

            // Index to Elasticsearch
            messageSearchService.indexMessage(message)

            logger.info { "Message indexed to Elasticsearch: id=${event.id}" }
        } catch (e: Exception) {
            // Level 1: Log and continue (message is still in MySQL)
            // Level 3: Add retry mechanism or dead letter queue
            logger.error(e) {
                "Failed to index message to Elasticsearch: id=${event.id}, error=${e.message}"
            }
        }
    }
}
