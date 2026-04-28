# Milestone 3. Outbox Pattern and Kafka Publishing
# 마일스톤 3. Outbox Pattern 및 Kafka 발행

This document is a working completion log for Milestone 3.
이 문서는 Milestone 3을 진행하면서 채워 넣는 완료 기록이다.

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

Separate durable core writes from asynchronous side effects using the Outbox Pattern and Kafka.
Outbox Pattern과 Kafka를 사용해 durable core write와 비동기 side effect를 분리한다.

This milestone proves that message persistence is not coupled to Kafka availability while still producing reliable events.
이 마일스톤은 Kafka 가용성에 message persistence가 결합되지 않으면서도 신뢰 가능한 event를 생성함을 증명한다.

## Scenario Summary
## 시나리오 요약

A message is stored in the database, an outbox event is stored in the same transaction, and a publisher later sends the event to Kafka.
메시지가 데이터베이스에 저장되고 같은 트랜잭션 안에서 outbox event가 저장되며, publisher가 이후 해당 event를 Kafka로 발행한다.

Kafka may be unavailable during message creation, but message persistence must still succeed.
메시지 생성 시점에 Kafka가 사용할 수 없는 상태여도 message persistence는 성공해야 한다.

Pending events are published after Kafka becomes available again.
Kafka가 다시 사용 가능해지면 pending event가 발행된다.

## Scope
## 범위

### Included
### 포함

- [ ] `cv_outbox_event` table
- [ ] `cv_outbox_event` table
- [ ] Outbox event model
- [ ] outbox event model
- [ ] Event payload versioning
- [ ] event payload versioning
- [ ] Outbox event repository or port
- [ ] outbox event repository 또는 port
- [ ] Publisher worker
- [ ] publisher worker
- [ ] Kafka producer
- [ ] Kafka producer
- [ ] Publish retry state
- [ ] publish retry state
- [ ] Publish failure handling
- [ ] publish failure handling
- [ ] Basic Kafka topic documentation
- [ ] 기본 Kafka topic 문서
- [ ] At least `conversation.message.created` event
- [ ] 최소 `conversation.message.created` event

### Excluded
### 제외

- [ ] Elasticsearch indexing
- [ ] Elasticsearch indexing
- [ ] Notification policy
- [ ] notification policy
- [ ] WebSocket scale-out
- [ ] WebSocket scale-out
- [ ] DLQ operations UI
- [ ] DLQ 운영 UI
- [ ] Full schema registry integration unless explicitly planned
- [ ] 명시적으로 계획되지 않은 full schema registry integration

## Design Note
## 설계 메모

Write this section before implementation.
이 섹션은 구현 전에 작성한다.

### Proposed Approach
### 제안 방식

TODO

### Transaction Boundaries
### 트랜잭션 경계

TODO

### Outbox Table Design
### Outbox Table 설계

TODO

### Event Contract
### Event 계약

TODO

### Publisher Strategy
### Publisher 전략

TODO

### Retry and Failure Handling
### Retry 및 실패 처리

TODO

### Kafka Topic Plan
### Kafka Topic 계획

TODO

### Test Plan
### 테스트 계획

TODO

## Architecture Notes
## 아키텍처 메모

Do not directly write the database and publish Kafka in the same application flow as an unsafe dual-write.
안전하지 않은 dual-write 방식으로 같은 application flow에서 database write와 Kafka publish를 직접 함께 수행하지 않는다.

Outbox event payloads must include `eventId`, `eventType`, `aggregateId`, `occurredAt`, and `payloadVersion`.
Outbox event payload는 `eventId`, `eventType`, `aggregateId`, `occurredAt`, `payloadVersion`을 포함해야 한다.

Kafka-specific code must live in infrastructure or application-level adapters, not in domain objects.
Kafka-specific 코드는 domain object가 아니라 infrastructure 또는 application-level adapter에 위치해야 한다.

The database is the durable handoff point between synchronous writes and asynchronous publishing.
database는 synchronous write와 asynchronous publishing 사이의 durable handoff point다.

## Main Flows
## 주요 흐름

### Message Creation with Outbox Event
### Outbox Event를 포함한 메시지 생성

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Application validates the message send command.
1. application은 message send command를 검증한다.
2. Message is persisted in the database.
2. message를 database에 저장한다.
3. Outbox event is created in the same transaction.
3. 같은 transaction 안에서 outbox event를 생성한다.
4. Transaction commits both message and outbox event.
4. transaction은 message와 outbox event를 함께 commit한다.
5. Kafka publishing happens later outside the user request path.
5. Kafka publishing은 user request path 밖에서 나중에 수행된다.

Implementation notes:
구현 메모:

TODO

### Outbox Publisher Worker
### Outbox Publisher Worker

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Worker selects pending outbox events.
1. worker는 pending outbox event를 조회한다.
2. Worker marks or locks selected events for publishing.
2. worker는 선택된 event를 publishing 대상으로 표시하거나 lock한다.
3. Worker publishes events to Kafka.
3. worker는 event를 Kafka로 발행한다.
4. Successful events are marked as published.
4. 성공한 event는 published로 표시한다.
5. Failed events keep retry metadata.
5. 실패한 event는 retry metadata를 유지한다.

Implementation notes:
구현 메모:

TODO

### Kafka Outage Recovery
### Kafka 장애 복구

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Kafka is unavailable.
1. Kafka를 사용할 수 없다.
2. Message creation still succeeds.
2. message creation은 여전히 성공한다.
3. Outbox events remain pending or failed with retry metadata.
3. outbox event는 retry metadata와 함께 pending 또는 failed 상태로 남는다.
4. Kafka becomes available.
4. Kafka가 사용 가능해진다.
5. Publisher sends pending events.
5. publisher는 pending event를 발행한다.

Implementation notes:
구현 메모:

TODO

### Event Contract Documentation
### Event 계약 문서화

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Define event type names.
1. event type name을 정의한다.
2. Define payload version.
2. payload version을 정의한다.
3. Define partitioning key.
3. partitioning key를 정의한다.
4. Define compatibility rules.
4. compatibility rule을 정의한다.

Implementation notes:
구현 메모:

TODO

## Acceptance Criteria
## 완료 기준

- [ ] Message creation stores an outbox event in the same database transaction.
- [ ] 메시지 생성은 같은 데이터베이스 트랜잭션 안에서 outbox event를 저장한다.
- [ ] Kafka outage does not fail message creation.
- [ ] Kafka 장애는 메시지 생성을 실패시키지 않는다.
- [ ] Pending events are published after Kafka becomes available again.
- [ ] Kafka가 다시 사용 가능해지면 pending event가 발행된다.
- [ ] Publisher retry behavior is deterministic and visible in the database.
- [ ] publisher retry 동작은 결정적이며 데이터베이스에서 확인 가능해야 한다.
- [ ] At least `conversation.message.created` is published to Kafka.
- [ ] 최소한 `conversation.message.created`가 Kafka로 발행된다.
- [ ] Event payload includes `eventId`, `eventType`, `aggregateId`, `occurredAt`, and `payloadVersion`.
- [ ] event payload는 `eventId`, `eventType`, `aggregateId`, `occurredAt`, `payloadVersion`을 포함한다.

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

1. Start the database and application.
1. database와 application을 시작한다.
2. Stop or block Kafka if testing outage behavior.
2. outage behavior를 테스트한다면 Kafka를 중지하거나 차단한다.
3. Send a visitor message.
3. visitor message를 전송한다.
4. Confirm message persistence succeeds.
4. message persistence가 성공하는지 확인한다.
5. Confirm an outbox event is stored.
5. outbox event가 저장되었는지 확인한다.
6. Start or restore Kafka.
6. Kafka를 시작하거나 복구한다.
7. Confirm the publisher sends the pending event.
7. publisher가 pending event를 발행하는지 확인한다.
8. Confirm the outbox event status changes.
8. outbox event status가 변경되는지 확인한다.

Evidence:
증거:

- API response: TODO
- API response: TODO
- Outbox rows: TODO
- Outbox rows: TODO
- Kafka topic output: TODO
- Kafka topic output: TODO
- Application logs: TODO
- Application logs: TODO

## Metrics
## 지표

| Metric | Result | Notes |
| --- | ---: | --- |
| Outbox pending count | TODO | TODO |
| Oldest pending event age | TODO | TODO |
| Publish success count | TODO | TODO |
| Publish failure count | TODO | TODO |
| Publish retry count | TODO | TODO |

| 지표 | 결과 | 메모 |
| --- | ---: | --- |
| outbox pending 수 | TODO | TODO |
| 가장 오래된 pending event age | TODO | TODO |
| publish 성공 수 | TODO | TODO |
| publish 실패 수 | TODO | TODO |
| publish retry 수 | TODO | TODO |

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

### Kafka
### Kafka

- TODO
- TODO

### Migrations
### Migrations

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

- Elasticsearch indexing is handled in Milestone 4.
- Elasticsearch indexing은 Milestone 4에서 다룬다.
- WebSocket scale-out is handled in a later milestone.
- WebSocket scale-out은 이후 마일스톤에서 다룬다.
- DLQ operations UI is not implemented in this milestone.
- DLQ 운영 UI는 이 마일스톤에서 구현하지 않는다.
- Consumer-side idempotency should be handled by each consumer milestone.
- consumer-side 멱등성은 각 consumer milestone에서 다뤄야 한다.

Additional gaps found during implementation:
구현 중 발견한 추가 한계:

- TODO
- TODO

## Follow-up Milestones
## 후속 마일스톤

- M4. Elasticsearch Message Search Projection
- M4. Elasticsearch 메시지 검색 Projection
- M5. WebSocket Scale-out
- M5. WebSocket Scale-out

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
