# Elasticsearch Learning Roadmap for Chat Application

## Overview

This document provides a step-by-step learning roadmap for integrating Elasticsearch into the exp-message chat application. The goal is to enable full-text search capabilities for chat messages.

## Why Elasticsearch for Chat Applications?

| Feature | MySQL LIKE Search | Elasticsearch |
|---------|------------------|---------------|
| Korean morphological analysis | ❌ | ✅ (Nori plugin) |
| Typo-tolerant search | ❌ | ✅ (Fuzzy query) |
| Search result highlighting | ❌ | ✅ |
| Large-scale search performance | Slow | Fast |
| Real-time indexing | Limited | ✅ |

## Learning Phases

### Phase 1: Basic Understanding (Level 1 - MVP) ⬅️ Current

**Goal**: Get Elasticsearch running and understand basic concepts

1. **Setup Elasticsearch locally**
   - Run Elasticsearch + Kibana via Docker Compose
   - Verify connection via `http://localhost:9200`

2. **Learn core concepts**
   - **Index**: Similar to a database table
   - **Document**: A single record (JSON format)
   - **Mapping**: Schema definition for documents
   - **Query DSL**: Elasticsearch's query language

3. **Spring Boot Integration**
   - Add `spring-boot-starter-data-elasticsearch` dependency
   - Configure connection properties
   - Create Document entity and Repository

4. **Implement basic search API**
   - `POST /api/messages/search` - Index a message
   - `GET /api/messages/search?q={keyword}` - Search messages

### Phase 2: Korean Search Optimization (Level 2)

**Goal**: Implement production-quality Korean text search

1. **Nori Analyzer Setup**
   - Install Nori plugin for Korean morphological analysis
   - Configure custom analyzer in index settings

2. **Advanced Querying**
   - Multi-field search (content, username)
   - Boolean queries (must, should, must_not)
   - Phrase matching

3. **Search UX Improvements**
   - Highlighting search terms in results
   - Cursor-based pagination
   - Search suggestions (autocomplete)

### Phase 3: Production Considerations (Level 3)

**Goal**: Make the system production-ready

1. **Data Synchronization**
   - Event-driven sync (on message create/update/delete)
   - Batch synchronization for initial data
   - Handle sync failures and retries

2. **Index Management**
   - Index versioning and migration
   - Index lifecycle management
   - Backup and restore

3. **Monitoring & Operations**
   - Health checks and metrics
   - Query performance monitoring
   - Alerting on failures

## Quick Reference

### Essential Kibana Dev Tools Commands

```json
// Check cluster health
GET /_cluster/health

// List all indices
GET /_cat/indices?v

// View index mapping
GET /messages/_mapping

// Simple search
GET /messages/_search
{
  "query": {
    "match": {
      "content": "hello"
    }
  }
}

// Delete an index (careful!)
DELETE /messages
```

### Useful Links

- [Elasticsearch Official Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Spring Data Elasticsearch Reference](https://docs.spring.io/spring-data/elasticsearch/reference/)
- [Nori Korean Analyzer](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-nori.html)

## Project Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Elasticsearch + Kibana containers |
| `ElasticsearchConfig.kt` | Spring Boot ES configuration |
| `MessageDocument.kt` | ES document entity for messages |
| `MessageSearchRepository.kt` | Spring Data ES repository |
| `MessageSearchService.kt` | Business logic for search |
| `MessageSearchController.kt` | REST API endpoints |

## Next Steps

After completing Level 1:
1. Test search functionality via Swagger UI
2. Explore Kibana Dev Tools to run manual queries
3. Observe how Korean text is tokenized (will show limitations)
4. Proceed to Level 2 for Nori analyzer integration
