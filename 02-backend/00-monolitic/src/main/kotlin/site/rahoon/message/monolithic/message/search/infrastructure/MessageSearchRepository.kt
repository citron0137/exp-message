package site.rahoon.message.monolithic.message.search.infrastructure

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
 * Elasticsearch Repository for MessageDocument.
 *
 * This interface provides data access operations for the Elasticsearch "messages" index.
 * Spring Data Elasticsearch automatically implements this interface at runtime.
 *
 * ## How Spring Data Elasticsearch Works:
 * 1. Extend ElasticsearchRepository<DocumentType, IdType>
 * 2. Spring scans for interfaces (via @EnableElasticsearchRepositories)
 * 3. Implementation is generated at runtime using dynamic proxies
 *
 * ## Available Methods (inherited from ElasticsearchRepository):
 * - save(document): Index a single document
 * - saveAll(documents): Bulk index multiple documents
 * - findById(id): Find document by ID
 * - findAll(): Get all documents (use with caution!)
 * - deleteById(id): Remove document by ID
 * - delete(document): Remove document
 * - count(): Count total documents
 *
 * ## Derived Query Methods:
 * Spring Data can generate queries from method names:
 * - findByFieldName(value) -> {"term": {"fieldName": "value"}}
 * - findByContentContaining(text) -> {"match": {"content": "text"}}
 *
 * ## Custom Queries:
 * For complex queries, use @Query annotation with Elasticsearch Query DSL:
 * ```
 * @Query("""{"bool": {"must": [{"match": {"content": "?0"}}]}}""")
 * fun searchByContent(keyword: String): List<MessageDocument>
 * ```
 *
 * @see ElasticsearchRepository Base repository with CRUD operations
 * @see MessageDocument The document type this repository manages
 */
interface MessageSearchRepository : ElasticsearchRepository<MessageDocument, String> {

    /**
     * Find messages by chat room ID.
     *
     * This is a derived query method. Spring Data parses the method name
     * and generates the Elasticsearch query:
     * ```json
     * {
     *   "query": {
     *     "term": {
     *       "chatRoomId": "<chatRoomId>"
     *     }
     *   }
     * }
     * ```
     *
     * Term query is used because chatRoomId is a Keyword field (exact match).
     *
     * @param chatRoomId The exact chat room ID to filter by
     * @return List of messages in the specified chat room
     */
    fun findByChatRoomId(chatRoomId: String): List<MessageDocument>

    /**
     * Search messages containing the specified text in content.
     *
     * This generates a match query for full-text search:
     * ```json
     * {
     *   "query": {
     *     "match": {
     *       "content": "<keyword>"
     *     }
     *   }
     * }
     * ```
     *
     * Match query analyzes the input and finds matching tokens.
     * Example: "hello world" -> finds documents with "hello" OR "world"
     *
     * Note: Results are scored by relevance (tf-idf).
     *
     * @param keyword The search term to look for in message content
     * @return List of matching messages, ordered by relevance score
     */
    fun findByContentContaining(keyword: String): List<MessageDocument>

    /**
     * Search messages in a specific chat room containing the keyword.
     *
     * Combines two conditions:
     * 1. chatRoomId must match exactly (filter)
     * 2. content must contain the keyword (match)
     *
     * @param chatRoomId The exact chat room ID to search within
     * @param keyword The search term to look for
     * @return List of matching messages in the specified room
     */
    fun findByChatRoomIdAndContentContaining(
        chatRoomId: String,
        keyword: String
    ): List<MessageDocument>
}
