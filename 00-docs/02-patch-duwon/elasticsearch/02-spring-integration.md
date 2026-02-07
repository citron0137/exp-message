# Spring Boot Elasticsearch Integration Guide

## Overview

This document explains how to integrate Elasticsearch with Spring Boot using Spring Data Elasticsearch.

## Dependencies

Add to `build.gradle.kts`:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
```

## Configuration

### Application Properties

```properties
# Elasticsearch connection settings
spring.elasticsearch.uris=http://localhost:9200

# Optional: Authentication (if enabled)
# spring.elasticsearch.username=elastic
# spring.elasticsearch.password=changeme

# Optional: Connection timeout
spring.elasticsearch.connection-timeout=5s
spring.elasticsearch.socket-timeout=30s
```

### Configuration Class

```kotlin
@Configuration
@EnableElasticsearchRepositories(basePackages = ["your.package.search"])
class ElasticsearchConfig {
    // Spring Boot auto-configures ElasticsearchClient
    // Custom configuration can be added here if needed
}
```

## Document Entity

A Document class represents an Elasticsearch document. It maps to an index.

```kotlin
@Document(indexName = "messages")
data class MessageDocument(
    // Unique identifier for the document
    @Id
    val id: String,

    // Keyword field for exact matching (filtering by room)
    @Field(type = FieldType.Keyword)
    val chatRoomId: String,

    // Keyword field for exact matching (filtering by user)
    @Field(type = FieldType.Keyword)
    val userId: String,

    // Text field for full-text search
    @Field(type = FieldType.Text)
    val content: String,

    // Date field for time-based queries
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis])
    val createdAt: LocalDateTime
)
```

### Field Type Reference

| Annotation          | Elasticsearch Type | Use Case                     |
| ------------------- | ------------------ | ---------------------------- |
| `FieldType.Text`    | text               | Full-text searchable content |
| `FieldType.Keyword` | keyword            | IDs, exact values, sorting   |
| `FieldType.Date`    | date               | Timestamps                   |
| `FieldType.Long`    | long               | Numeric IDs, counts          |
| `FieldType.Boolean` | boolean            | Flags                        |

## Repository

Spring Data Elasticsearch provides repository abstraction similar to Spring Data JPA.

### Basic Repository

```kotlin
interface MessageSearchRepository : ElasticsearchRepository<MessageDocument, String> {
    // Derived query methods work like JPA
    fun findByChatRoomId(chatRoomId: String): List<MessageDocument>

    fun findByContentContaining(keyword: String): List<MessageDocument>
}
```

### Custom Query Methods

```kotlin
interface MessageSearchRepository : ElasticsearchRepository<MessageDocument, String> {

    // Search by content with pagination
    @Query("""
        {
            "match": {
                "content": "?0"
            }
        }
    """)
    fun searchByContent(keyword: String, pageable: Pageable): Page<MessageDocument>

    // Search within a specific chat room
    @Query("""
        {
            "bool": {
                "must": [
                    { "match": { "content": "?0" } }
                ],
                "filter": [
                    { "term": { "chatRoomId": "?1" } }
                ]
            }
        }
    """)
    fun searchInRoom(keyword: String, chatRoomId: String): List<MessageDocument>
}
```

## Service Layer

```kotlin
@Service
class MessageSearchService(
    private val messageSearchRepository: MessageSearchRepository
) {
    /**
     * Index a message document for search.
     * Call this when a new message is created.
     */
    fun indexMessage(message: Message): MessageDocument {
        val document = MessageDocument(
            id = message.id,
            chatRoomId = message.chatRoomId,
            userId = message.userId,
            content = message.content,
            createdAt = message.createdAt
        )
        return messageSearchRepository.save(document)
    }

    /**
     * Search messages by keyword.
     */
    fun searchMessages(keyword: String): List<MessageDocument> {
        return messageSearchRepository.findByContentContaining(keyword)
    }

    /**
     * Search messages within a specific chat room.
     */
    fun searchInRoom(keyword: String, chatRoomId: String): List<MessageDocument> {
        return messageSearchRepository.searchInRoom(keyword, chatRoomId)
    }

    /**
     * Delete a message from the search index.
     * Call this when a message is deleted.
     */
    fun deleteMessage(messageId: String) {
        messageSearchRepository.deleteById(messageId)
    }
}
```

## Controller

```kotlin
@RestController
@RequestMapping("/api/messages/search")
class MessageSearchController(
    private val messageSearchService: MessageSearchService
) {
    /**
     * Search messages by keyword.
     * GET /api/messages/search?q=hello
     */
    @GetMapping
    fun search(@RequestParam("q") keyword: String): List<MessageSearchResponse> {
        return messageSearchService.searchMessages(keyword)
            .map { MessageSearchResponse.from(it) }
    }

    /**
     * Search messages within a specific chat room.
     * GET /api/messages/search/rooms/{roomId}?q=hello
     */
    @GetMapping("/rooms/{roomId}")
    fun searchInRoom(
        @PathVariable roomId: String,
        @RequestParam("q") keyword: String
    ): List<MessageSearchResponse> {
        return messageSearchService.searchInRoom(keyword, roomId)
            .map { MessageSearchResponse.from(it) }
    }
}
```

## Error Handling

Common issues and solutions:

### 1. Connection Refused

```
org.elasticsearch.client.transport.NoNodeAvailableException
```

**Solution**: Ensure Elasticsearch is running and accessible at the configured URI.

### 2. Index Not Found

```
org.elasticsearch.index.IndexNotFoundException
```

**Solution**: The index is created automatically when the first document is indexed. Or create it manually via Kibana.

### 3. Mapping Conflict

```
mapper_parsing_exception: failed to parse field
```

**Solution**: Field types are immutable. Delete the index and reindex if you need to change mappings.

## Testing

### Unit Test with Testcontainers

```kotlin
@Testcontainers
@SpringBootTest
class MessageSearchServiceTest {

    companion object {
        @Container
        val elasticsearch = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.3.0")
            .withEnv("xpack.security.enabled", "false")
    }

    @DynamicPropertySource
    @JvmStatic
    fun properties(registry: DynamicPropertyRegistry) {
        registry.add("spring.elasticsearch.uris") { elasticsearch.httpHostAddress }
    }

    @Autowired
    lateinit var messageSearchService: MessageSearchService

    @Test
    fun `should index and search message`() {
        // given
        val message = Message.create("room-1", "user-1", "Hello world")

        // when
        messageSearchService.indexMessage(message)
        Thread.sleep(1100) // Wait for ES refresh

        // then
        val results = messageSearchService.searchMessages("hello")
        assertThat(results).hasSize(1)
        assertThat(results[0].content).isEqualTo("Hello world")
    }
}
```

## Sync Strategy (Brief Overview)

For Level 1, we manually call `indexMessage()` when saving to MySQL. In production, consider:

1. **Event-driven**: Publish events on message CRUD, consume to update ES
2. **Change Data Capture (CDC)**: Use Debezium to stream MySQL changes to ES
3. **Scheduled batch**: Periodically sync MySQL to ES

This will be covered in detail in Level 3.

## Next Steps

1. Start Elasticsearch container
2. Run the application
3. Create a message via existing API
4. Search for it via the new search API
5. Explore Kibana to see indexed documents
