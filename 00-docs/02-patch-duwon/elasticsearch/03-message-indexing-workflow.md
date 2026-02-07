# Message Indexing Workflow

## Overview

This document describes how messages flow from user input to Elasticsearch indexing.

## Complete Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         User Sends Message                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  MessageController.create()                                                  │
│  POST /messages                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  MessageApplicationService.create()                                          │
│  1. Validate chat room exists                                                │
│  2. Validate user is room member                                             │
│  3. Call messageDomainService.create() → MySQL INSERT                        │
│  4. Publish MessageEvent.Created                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
        ┌───────────────────────┐     ┌───────────────────────────────┐
        │  Response to User     │     │  Spring Event System          │
        │  (Immediate)          │     │  (After Transaction Commit)   │
        └───────────────────────┘     └───────────────────────────────┘
                                                    │
                        ┌───────────────────────────┼───────────────────────────┐
                        │                           │                           │
                        ▼                           ▼                           ▼
        ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
        │ MessageCommandEvent     │  │ MessageSearchEvent      │  │ (Future listeners...)   │
        │ Dispatcher              │  │ Listener                │  │                         │
        │                         │  │                         │  │                         │
        │ → Send to WebSocket     │  │ → Index to ES           │  │                         │
        │ → Redis Pub/Sub         │  │ → Full-text searchable  │  │                         │
        └─────────────────────────┘  └─────────────────────────┘  └─────────────────────────┘
                    │                           │
                    ▼                           ▼
        ┌─────────────────────────┐  ┌─────────────────────────┐
        │  Real-time delivery     │  │  Elasticsearch          │
        │  to chat room members   │  │  "messages" index       │
        └─────────────────────────┘  └─────────────────────────┘
```

## Key Components

### 1. MessageApplicationService

Location: `message/application/MessageApplicationService.kt`

Responsible for:
- Validating business rules (room exists, user is member)
- Saving message to MySQL via domain service
- Publishing `MessageEvent.Created` event

### 2. MessageSearchEventListener

Location: `message/search/application/MessageSearchEventListener.kt`

Responsible for:
- Listening to `MessageEvent.Created` events
- Indexing messages to Elasticsearch asynchronously

### 3. MessageCommandEventDispatcher

Location: `message/application/MessageCommandEventDispatcher.kt`

Responsible for:
- Listening to `MessageEvent.Created` events
- Dispatching real-time notifications via WebSocket/Redis

## Event Flow Details

| Step | Component | Action | Storage |
|------|-----------|--------|---------|
| 1 | Controller | Receive HTTP request | - |
| 2 | ApplicationService | Validate & save | MySQL |
| 3 | ApplicationService | Publish event | - |
| 4a | CommandEventDispatcher | Send to WebSocket | Redis Pub/Sub |
| 4b | SearchEventListener | Index for search | Elasticsearch |

## Why This Design?

### Separation of Concerns

- **MySQL**: Source of truth (ACID transactions)
- **Elasticsearch**: Optimized for search (eventually consistent)
- **Redis**: Real-time message delivery

### Event-Driven Architecture

- Loose coupling between components
- Each listener handles its own concern
- Easy to add new listeners (e.g., push notifications)

### Asynchronous Processing

- `@Async`: ES indexing doesn't block API response
- `@TransactionalEventListener(phase = AFTER_COMMIT)`: Only process after MySQL commit succeeds

## Error Handling

### Level 1 (Current)

```kotlin
try {
    messageSearchService.indexMessage(message)
} catch (e: Exception) {
    logger.error(e) { "Failed to index message" }
    // Continue - message is still in MySQL
}
```

### Level 3 (Production)

- Retry with exponential backoff
- Dead letter queue for failed messages
- Periodic reconciliation job

## Testing the Workflow

```bash
# 1. Create a message
curl -X POST http://localhost/api/messages \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"chatRoomId": "room-1", "content": "Hello world"}'

# 2. Wait ~1 second for ES refresh

# 3. Search for the message
curl "http://localhost/api/messages/search?q=hello"

# 4. Verify in Kibana Dev Tools
GET /messages/_search
{
  "query": { "match_all": {} }
}
```

## Related Files

| File | Purpose |
|------|---------|
| `MessageApplicationService.kt` | Publishes MessageEvent.Created |
| `MessageSearchEventListener.kt` | Receives event, indexes to ES |
| `MessageSearchService.kt` | ES indexing logic |
| `MessageDocument.kt` | ES document structure |
| `MessageSearchRepository.kt` | ES data access |
