# Elasticsearch Basic Concepts

## Core Terminology

### Index

An **index** is like a database table in relational databases. It's a collection of documents that share similar characteristics.

```
Relational DB  →  Elasticsearch
─────────────────────────────────
Database       →  Cluster
Table          →  Index
Row            →  Document
Column         →  Field
```

**Example**: In our chat app, we have a `messages` index that stores all chat messages.

### Document

A **document** is the basic unit of data in Elasticsearch. It's a JSON object that contains your actual data.

```json
{
  "id": "abc123",
  "chatRoomId": "room-001",
  "userId": "user-456",
  "content": "Hello, how are you?",
  "createdAt": "2025-01-15T10:30:00"
}
```

### Mapping

A **mapping** defines how a document and its fields are stored and indexed. It's like a schema definition.

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "chatRoomId": { "type": "keyword" },
      "userId": { "type": "keyword" },
      "content": { "type": "text" },
      "createdAt": { "type": "date" }
    }
  }
}
```

### Field Types

| Type | Use Case | Example |
|------|----------|---------|
| `text` | Full-text search (analyzed) | Message content |
| `keyword` | Exact matching, sorting, aggregations | User ID, Room ID |
| `date` | Date/time values | Created timestamp |
| `long` | Numeric values | Message count |
| `boolean` | True/false values | Is deleted flag |

**Key Difference**: 
- `text` fields are **analyzed** (broken into tokens for search)
- `keyword` fields are stored **as-is** (for exact matching)

## How Search Works

### 1. Indexing (Write)

When you save a document:
1. Elasticsearch receives the JSON document
2. The **analyzer** processes text fields
3. Tokens are stored in an **inverted index**

```
Original text: "Hello world"
After analysis: ["hello", "world"]
```

### 2. Searching (Read)

When you search:
1. Your query goes through the same analyzer
2. Elasticsearch looks up tokens in the inverted index
3. Matching documents are scored and returned

```
Search query: "hello"
→ Analyzed to: ["hello"]
→ Finds documents containing "hello"
```

### 3. Inverted Index

An inverted index maps tokens to document IDs:

```
Token    → Document IDs
─────────────────────────
"hello"  → [doc1, doc3, doc7]
"world"  → [doc1, doc2]
"chat"   → [doc2, doc3, doc5]
```

## Query DSL Basics

### Match Query (Full-text search)

```json
GET /messages/_search
{
  "query": {
    "match": {
      "content": "hello world"
    }
  }
}
```

This searches for documents where `content` contains "hello" OR "world".

### Term Query (Exact match)

```json
GET /messages/_search
{
  "query": {
    "term": {
      "chatRoomId": "room-001"
    }
  }
}
```

This finds documents where `chatRoomId` is exactly "room-001".

### Bool Query (Combining conditions)

```json
GET /messages/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "content": "hello" } }
      ],
      "filter": [
        { "term": { "chatRoomId": "room-001" } }
      ]
    }
  }
}
```

- `must`: Conditions that MUST match (affects score)
- `filter`: Conditions that MUST match (no scoring, faster)
- `should`: Conditions that SHOULD match (boosts score)
- `must_not`: Conditions that MUST NOT match

## Analyzers

An **analyzer** processes text into tokens. It consists of:

1. **Character filters**: Preprocess (e.g., strip HTML)
2. **Tokenizer**: Split text into tokens
3. **Token filters**: Transform tokens (e.g., lowercase)

### Standard Analyzer (Default)

```
Input:  "Hello, World! How are you?"
Output: ["hello", "world", "how", "are", "you"]
```

### Why Korean Needs Special Treatment

Standard analyzer doesn't understand Korean grammar:

```
Input:  "안녕하세요"
Output: ["안녕하세요"]  ← Treated as one token!
```

With Nori analyzer:

```
Input:  "안녕하세요"
Output: ["안녕", "하", "세요"]  ← Properly tokenized
```

This is why we'll add Nori in Level 2.

## Hands-on Practice with Kibana

After starting Elasticsearch and Kibana, open Kibana Dev Tools at `http://localhost:5601/app/dev_tools`.

### 1. Create an index with mapping

```json
PUT /test-messages
{
  "mappings": {
    "properties": {
      "content": { "type": "text" },
      "roomId": { "type": "keyword" }
    }
  }
}
```

### 2. Index a document

```json
POST /test-messages/_doc/1
{
  "content": "Hello, this is a test message",
  "roomId": "room-001"
}
```

### 3. Search for it

```json
GET /test-messages/_search
{
  "query": {
    "match": {
      "content": "test"
    }
  }
}
```

### 4. Clean up

```json
DELETE /test-messages
```

## Common Gotchas

1. **Text vs Keyword confusion**: Use `text` for searchable content, `keyword` for IDs and exact values

2. **Mapping is immutable**: Once a field is mapped, you can't change its type. You need to reindex.

3. **Near real-time**: Indexed documents are searchable after ~1 second (refresh interval)

4. **Case sensitivity**: `text` fields are lowercased by default, `keyword` fields are case-sensitive

## Next Steps

- Start the Elasticsearch container (`docker-compose up -d`)
- Open Kibana and try the practice queries
- Proceed to implement the Spring Boot integration
