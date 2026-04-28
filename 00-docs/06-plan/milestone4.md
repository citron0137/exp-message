# Milestone 4. Elasticsearch Message Search Projection
# 마일스톤 4. Elasticsearch 메시지 검색 Projection

This document is a working completion log for Milestone 4.
이 문서는 Milestone 4를 진행하면서 채워 넣는 완료 기록이다.

Use this document as a checklist before implementation and as delivery evidence after implementation.
구현 전에는 체크리스트로 사용하고, 구현 후에는 완료 증거로 사용한다.

## Status
## 상태

Status: TODO
상태: TODO

Completed date: TODO
완료일: TODO

Owners: TODO
담당자: TODO

Related roadmap: `00-docs/06-plan/todo.md`
관련 로드맵: `00-docs/06-plan/todo.md`

Related pull requests: TODO
관련 PR: TODO

## Goal
## 목표

Build a search projection for conversation messages using Kafka consumers and Elasticsearch.
Kafka consumer와 Elasticsearch를 사용해 conversation message 검색 projection을 만든다.

This milestone proves that the relational database can remain the source of truth while Elasticsearch serves fast search queries.
이 마일스톤은 relational database가 source of truth로 남고 Elasticsearch가 빠른 search query를 담당할 수 있음을 증명한다.

## Scenario Summary
## 시나리오 요약

An admin searches conversation messages by keyword, channel, conversation, sender type, and date range.
관리자는 keyword, channel, conversation, sender type, date range로 conversation message를 검색한다.

Message creation events are consumed from Kafka and indexed into Elasticsearch asynchronously.
메시지 생성 event는 Kafka에서 소비되어 Elasticsearch에 비동기로 색인된다.

If the search index is lost or stale, it can be rebuilt from database records.
search index가 유실되거나 오래된 상태가 되면 database record로부터 재구축할 수 있다.

## Scope
## 범위

### Included
### 포함

- [ ] Kafka indexing consumer
- [ ] Kafka indexing consumer
- [ ] Elasticsearch index mapping
- [ ] Elasticsearch index mapping
- [ ] Korean analyzer evaluation
- [ ] Korean analyzer 검토
- [ ] Message search API
- [ ] message search API
- [ ] Keyword search
- [ ] keyword search
- [ ] Channel ID filter
- [ ] channel ID filter
- [ ] Conversation ID filter
- [ ] conversation ID filter
- [ ] Sender type filter
- [ ] sender type filter
- [ ] Date range filter
- [ ] date range filter
- [ ] Highlight support
- [ ] highlight 지원
- [ ] Idempotent indexing
- [ ] 멱등 indexing
- [ ] Reindex job
- [ ] reindex job

### Excluded
### 제외

- [ ] Advanced ranking
- [ ] advanced ranking
- [ ] Semantic search
- [ ] semantic search
- [ ] Cross-channel analytics
- [ ] cross-channel analytics
- [ ] Admin search UI unless explicitly planned
- [ ] 명시적으로 계획되지 않은 admin search UI
- [ ] Multi-language analyzer optimization beyond Korean evaluation
- [ ] Korean 검토를 넘어선 multi-language analyzer optimization

## Design Note
## 설계 메모

Write this section before implementation.
이 섹션은 구현 전에 작성한다.

### Proposed Approach
### 제안 방식

TODO

### Search Index Design
### Search Index 설계

TODO

### Mapping and Analyzer Plan
### Mapping 및 Analyzer 계획

TODO

### Consumer Idempotency
### Consumer 멱등성

TODO

### Reindex Strategy
### Reindex 전략

TODO

### Search API Contract
### Search API 계약

TODO

### Failure Handling
### 실패 처리

TODO

### Test Plan
### 테스트 계획

TODO

## Architecture Notes
## 아키텍처 메모

The relational database remains the source of truth.
관계형 데이터베이스는 source of truth로 남는다.

Elasticsearch is a projection and may be rebuilt from database state.
Elasticsearch는 projection이며 database state로부터 재구축될 수 있어야 한다.

Consumers must be idempotent by `eventId` or deterministic document ID.
consumer는 `eventId` 또는 deterministic document ID를 기준으로 멱등해야 한다.

Search query services may use Elasticsearch directly because they are read-model adapters.
search query service는 read-model adapter이므로 Elasticsearch를 직접 사용할 수 있다.

Core domain objects must not depend on Elasticsearch client types.
core domain object는 Elasticsearch client type에 의존하면 안 된다.

## Main Flows
## 주요 흐름

### Message Indexing Consumer
### Message Indexing Consumer

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Consumer receives `conversation.message.created`.
1. consumer는 `conversation.message.created`를 수신한다.
2. Consumer validates payload version.
2. consumer는 payload version을 검증한다.
3. Consumer maps event payload to a search document.
3. consumer는 event payload를 search document로 매핑한다.
4. Consumer upserts the document into Elasticsearch.
4. consumer는 document를 Elasticsearch에 upsert한다.
5. Consumer records or relies on deterministic idempotency.
5. consumer는 idempotency를 기록하거나 deterministic idempotency에 의존한다.

Implementation notes:
구현 메모:

TODO

### Admin Message Search
### 관리자 메시지 검색

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Admin identity is extracted by presentation.
1. admin identity는 presentation에서 추출한다.
2. Application validates channel access.
2. application은 channel access를 검증한다.
3. Query service builds an Elasticsearch search request.
3. query service는 Elasticsearch search request를 구성한다.
4. Search applies keyword and structured filters.
4. search는 keyword와 structured filter를 적용한다.
5. Response returns matching messages and optional highlights.
5. response는 matching message와 optional highlight를 반환한다.

Implementation notes:
구현 메모:

TODO

### Duplicate Event Consumption
### 중복 Event 소비

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Same event is consumed more than once.
1. 같은 event가 두 번 이상 소비된다.
2. Consumer uses `eventId` or deterministic document ID.
2. consumer는 `eventId` 또는 deterministic document ID를 사용한다.
3. Elasticsearch document is updated or skipped without duplication.
3. Elasticsearch document는 중복 없이 update되거나 skip된다.
4. Duplicate handling is observable through logs or metrics.
4. duplicate handling은 log 또는 metric으로 관측 가능하다.

Implementation notes:
구현 메모:

TODO

### Reindex
### Reindex

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Reindex job reads message records from the database.
1. reindex job은 database에서 message record를 읽는다.
2. Job maps records into search documents.
2. job은 record를 search document로 매핑한다.
3. Job writes documents into a target index.
3. job은 document를 target index에 기록한다.
4. Alias switch is performed if alias-based rollout is implemented.
4. alias 기반 rollout을 구현했다면 alias switch를 수행한다.
5. Reindex result is recorded.
5. reindex result를 기록한다.

Implementation notes:
구현 메모:

TODO

## Acceptance Criteria
## 완료 기준

- [ ] New messages are searchable after Kafka consumption and Elasticsearch indexing.
- [ ] 새 메시지는 Kafka 소비와 Elasticsearch 색인 이후 검색 가능해야 한다.
- [ ] Search supports keyword, channel ID, conversation ID, sender type, and date range filters.
- [ ] 검색은 keyword, channel ID, conversation ID, sender type, date range filter를 지원한다.
- [ ] Search responses can include highlights when keyword search is used.
- [ ] keyword search를 사용할 때 search response는 highlight를 포함할 수 있다.
- [ ] Duplicate event consumption does not create duplicate search documents.
- [ ] 중복 event 소비는 중복 search document를 생성하지 않는다.
- [ ] Reindex can rebuild the message index from database records.
- [ ] reindex는 database record로부터 message index를 재구축할 수 있다.
- [ ] Search access is restricted to admins with valid channel access.
- [ ] search access는 유효한 channel access를 가진 admin으로 제한된다.

## Verification
## 검증

### Automated Tests
### 자동화 테스트

Command:
명령어:

```bash
TODO
```

Result:
결과:

```text
TODO
```

Notes:
메모:

TODO

### Manual Demo
### 수동 데모

Environment:
환경:

```text
TODO
```

Steps:
절차:

1. Start database, Kafka, Elasticsearch, and application.
1. database, Kafka, Elasticsearch, application을 시작한다.
2. Send a visitor message containing a searchable keyword.
2. 검색 가능한 keyword를 포함한 visitor message를 전송한다.
3. Confirm the message event is consumed.
3. message event가 소비되었는지 확인한다.
4. Confirm the message document exists in Elasticsearch.
4. message document가 Elasticsearch에 존재하는지 확인한다.
5. Run admin message search with keyword and filters.
5. keyword와 filter로 admin message search를 실행한다.
6. Confirm the search response contains the expected message.
6. search response에 기대한 message가 포함되는지 확인한다.
7. Trigger duplicate event handling.
7. duplicate event handling을 발생시킨다.
8. Confirm no duplicate search documents are created.
8. 중복 search document가 생성되지 않는지 확인한다.
9. Run reindex and confirm search still works.
9. reindex를 실행하고 search가 여전히 동작하는지 확인한다.

Evidence:
증거:

- API response: TODO
- API response: TODO
- Elasticsearch document: TODO
- Elasticsearch document: TODO
- Kafka consumer logs: TODO
- Kafka consumer logs: TODO
- Reindex output: TODO
- Reindex output: TODO

## Metrics
## 지표

| Metric | Result | Notes |
| --- | ---: | --- |
| Indexing success count | TODO | TODO |
| Indexing failure count | TODO | TODO |
| Indexing lag | TODO | TODO |
| Search latency | TODO | TODO |
| Reindex duration | TODO | TODO |

| 지표 | 결과 | 메모 |
| --- | ---: | --- |
| indexing 성공 수 | TODO | TODO |
| indexing 실패 수 | TODO | TODO |
| indexing lag | TODO | TODO |
| search latency | TODO | TODO |
| reindex duration | TODO | TODO |

## Key Files
## 주요 파일

### Application
### Application

- TODO
- TODO

### Infrastructure
### Infrastructure

- TODO
- TODO

### Elasticsearch
### Elasticsearch

- TODO
- TODO

### Kafka Consumer
### Kafka Consumer

- TODO
- TODO

### Tests
### Tests

- TODO
- TODO

### Docs
### Docs

- TODO
- TODO

## Decisions
## 결정 사항

### Decision 1
### 결정 1

Decision:
결정:

TODO

Reason:
이유:

TODO

Trade-off:
트레이드오프:

TODO

### Decision 2
### 결정 2

Decision:
결정:

TODO

Reason:
이유:

TODO

Trade-off:
트레이드오프:

TODO

## Known Gaps
## 알려진 한계

- Advanced ranking is not implemented in this milestone.
- advanced ranking은 이 마일스톤에서 구현하지 않는다.
- Semantic search is not implemented in this milestone.
- semantic search는 이 마일스톤에서 구현하지 않는다.
- Admin search UI is not included unless explicitly added.
- 명시적으로 추가하지 않는 한 admin search UI는 포함하지 않는다.
- Elasticsearch remains eventually consistent with the relational database.
- Elasticsearch는 relational database와 eventually consistent 상태로 남는다.

Additional gaps found during implementation:
구현 중 발견한 추가 한계:

- TODO
- TODO

## Follow-up Milestones
## 후속 마일스톤

- M5. WebSocket Scale-out
- M5. WebSocket Scale-out
- M6. Redis Presence and Rate Limiting
- M6. Redis Presence 및 Rate Limiting
- M7. K6 Load Testing and Observability
- M7. K6 부하 테스트 및 Observability

## Completion Summary
## 완료 요약

Write this section after implementation is complete.
이 섹션은 구현 완료 후 작성한다.

Summary:
요약:

TODO

What worked well:
잘 된 점:

TODO

What should be improved:
개선할 점:

TODO

Next action:
다음 액션:

TODO
