# Core Roadmap TODO
# Core 로드맵 TODO

This roadmap defines scenario-based milestones for replacing the legacy backend core with the new `core` architecture.
이 로드맵은 legacy backend core를 새로운 `core` 아키텍처로 대체하기 위한 시나리오 기반 마일스톤을 정의한다.

The project goal is to build a Channel Talk-like messaging platform while demonstrating production-grade backend engineering skills.
프로젝트 목표는 채널톡과 유사한 메시징 플랫폼을 만들면서 실무 수준의 백엔드 엔지니어링 역량을 보여주는 것이다.

Each milestone should be delivered as a demonstrable product/system scenario, not as an isolated technology task.
각 마일스톤은 고립된 기술 과제가 아니라 시연 가능한 제품/시스템 시나리오로 완성되어야 한다.

Implementation details may be proposed by the owner, but goals, scope, architecture constraints, acceptance criteria, and metrics are fixed.
구현 세부 방식은 담당자가 제안할 수 있지만 목표, 범위, 아키텍처 제약, 완료 기준, 관측 지표는 고정한다.

## Working Agreement
## 작업 합의

Every milestone must start with a short design note before implementation.
모든 마일스톤은 구현 전에 짧은 설계 메모로 시작한다.

The design note must explain transaction boundaries, failure handling, idempotency, tests, and operational metrics.
설계 메모는 트랜잭션 경계, 실패 처리, 멱등성, 테스트, 운영 지표를 설명해야 한다.

The implementation must follow the current architecture rule: presentation calls application, application owns use-case orchestration, and domain remains framework-independent.
구현은 현재 아키텍처 규칙을 따라야 한다: presentation은 application을 호출하고, application은 유스케이스 오케스트레이션을 소유하며, domain은 프레임워크에 독립적이어야 한다.

Core domain code must not depend on Kafka, Elasticsearch, Redis, Spring WebSocket, JPA, or transport-specific DTOs.
Core domain 코드는 Kafka, Elasticsearch, Redis, Spring WebSocket, JPA, transport-specific DTO에 의존하면 안 된다.

Database schema changes must be managed through Flyway migrations in `02-backend/01-db-migrations/src/main/resources/db/migration`.
데이터베이스 스키마 변경은 `02-backend/01-db-migrations/src/main/resources/db/migration`의 Flyway migration으로 관리해야 한다.

Each completed milestone must include tests, documentation updates, and a local demo path.
완료된 각 마일스톤은 테스트, 문서 업데이트, 로컬 데모 경로를 포함해야 한다.

## M1. Core Conversation MVP
## M1. Core Conversation MVP

### Goal
### 목표

Deliver the basic Channel Talk-like conversation flow from visitor widget to admin inbox.
방문자 위젯에서 관리자 인박스까지 이어지는 기본 채널톡형 상담 흐름을 완성한다.

### Scenario
### 시나리오

A visitor opens the widget, gets or restores a visitor session, sends a message, and sees the admin reply in real time.
방문자가 위젯을 열고 visitor session을 생성하거나 복구한 뒤 메시지를 보내고, 관리자 답장을 실시간으로 확인한다.

An admin opens the inbox, sees the visitor conversation, sends a reply, and sees read/unread state reflected.
관리자는 인박스를 열어 방문자 상담을 확인하고 답장을 보내며 읽음/안 읽음 상태가 반영되는 것을 확인한다.

### Scope
### 범위

Included: visitor session bootstrap, visitor message send, admin message send, conversation creation or reopen, message persistence, basic WebSocket push, admin inbox query, and read/unread basics.
포함: visitor session bootstrap, visitor message send, admin message send, conversation 생성 또는 재오픈, message persistence, 기본 WebSocket push, admin inbox query, 기본 읽음/안 읽음 처리.

Excluded: Kafka delivery, Elasticsearch search, multi-node WebSocket scale-out, Redis presence, and load testing.
제외: Kafka delivery, Elasticsearch search, multi-node WebSocket scale-out, Redis presence, load testing.

### Acceptance Criteria
### 완료 기준

Visitor message send stores a conversation message in the database.
방문자 메시지 전송은 conversation message를 데이터베이스에 저장한다.

Admin message send stores a conversation message in the database.
관리자 메시지 전송은 conversation message를 데이터베이스에 저장한다.

Visitor and admin clients receive basic WebSocket message delivery when connected to the same application instance.
방문자와 관리자 클라이언트는 같은 애플리케이션 인스턴스에 연결되어 있을 때 기본 WebSocket 메시지 전달을 받는다.

Admin inbox returns conversations ordered by recent activity with cursor-based pagination.
관리자 인박스는 최근 활동 기준 정렬과 cursor 기반 pagination으로 conversation을 반환한다.

Read and unread state is visible through admin-facing query responses.
읽음/안 읽음 상태는 관리자용 query response에서 확인할 수 있다.

### Metrics
### 지표

Track message send count, message send failure count, active WebSocket connections, and inbox query latency.
message send count, message send failure count, active WebSocket connections, inbox query latency를 추적한다.

## M2. DB Concurrency and Idempotency
## M2. DB 동시성 및 멱등성

### Goal
### 목표

Make core write flows safe under duplicate requests and concurrent user actions.
중복 요청과 동시 사용자 액션 상황에서도 core write flow가 안전하게 동작하도록 만든다.

### Scenario
### 시나리오

A visitor retries the same message request due to a network timeout and receives the same result instead of creating duplicates.
방문자가 네트워크 타임아웃으로 같은 메시지 요청을 재시도해도 중복 생성 대신 같은 결과를 받는다.

Two admins attempt to assign the same conversation at the same time, and only one assignment succeeds.
두 명의 관리자가 같은 conversation을 동시에 배정하려고 할 때 하나의 배정만 성공한다.

### Scope
### 범위

Included: `clientMessageId`, idempotent message send, unique constraints, assignment concurrency handling, conversation state transition validation, and concurrency tests.
포함: `clientMessageId`, 멱등 message send, unique constraint, 배정 동시성 처리, conversation 상태 전이 검증, 동시성 테스트.

Excluded: Kafka event idempotency, Elasticsearch indexing idempotency, and distributed locks unless proven necessary.
제외: Kafka event 멱등성, Elasticsearch indexing 멱등성, 필요성이 증명되지 않은 distributed lock.

### Architecture Constraints
### 아키텍처 제약

Prefer database constraints and transaction boundaries before introducing distributed locks.
distributed lock을 도입하기 전에 database constraint와 transaction boundary를 우선한다.

Expected application failures must use core-specific errors, not generic exceptions.
예상 가능한 application failure는 generic exception이 아니라 core-specific error를 사용해야 한다.

### Acceptance Criteria
### 완료 기준

Duplicate `clientMessageId` requests return the original stored message result.
중복 `clientMessageId` 요청은 기존에 저장된 message result를 반환한다.

Concurrent assignment requests cannot assign the same conversation to multiple admins.
동시 배정 요청은 같은 conversation을 여러 관리자에게 배정할 수 없다.

Closed or invalid conversation state transitions are rejected with explicit application errors.
종료되었거나 유효하지 않은 conversation 상태 전이는 명시적인 application error로 거부된다.

Integration tests prove duplicate request and concurrent assignment scenarios.
integration test는 중복 요청과 동시 배정 시나리오를 증명한다.

### Metrics
### 지표

Track idempotency hit count, optimistic lock failure count, duplicate request count, and assignment conflict count.
idempotency hit count, optimistic lock failure count, duplicate request count, assignment conflict count를 추적한다.

## M3. Outbox Pattern and Kafka Publishing
## M3. Outbox Pattern 및 Kafka 발행

### Goal
### 목표

Separate durable core writes from asynchronous side effects using the Outbox Pattern and Kafka.
Outbox Pattern과 Kafka를 사용해 durable core write와 비동기 side effect를 분리한다.

### Scenario
### 시나리오

A message is stored in the database, an outbox event is stored in the same transaction, and a publisher later sends the event to Kafka.
메시지가 데이터베이스에 저장되고 같은 트랜잭션 안에서 outbox event가 저장되며, publisher가 이후 해당 event를 Kafka로 발행한다.

Kafka may be unavailable during message creation, but message persistence must still succeed.
메시지 생성 시점에 Kafka가 사용할 수 없는 상태여도 message persistence는 성공해야 한다.

### Scope
### 범위

Included: `cv_outbox_event` table, outbox event model, event payload versioning, publisher worker, Kafka producer, retry state, publish failure handling, and basic topic documentation.
포함: `cv_outbox_event` table, outbox event model, event payload versioning, publisher worker, Kafka producer, retry state, publish failure handling, 기본 topic 문서.

Excluded: Elasticsearch indexing, notification policy, WebSocket scale-out, and DLQ operations UI.
제외: Elasticsearch indexing, notification policy, WebSocket scale-out, DLQ 운영 UI.

### Architecture Constraints
### 아키텍처 제약

Do not directly write the database and publish Kafka in the same application flow as an unsafe dual-write.
안전하지 않은 dual-write 방식으로 같은 application flow에서 database write와 Kafka publish를 직접 함께 수행하지 않는다.

Outbox event payloads must include `eventId`, `eventType`, `aggregateId`, `occurredAt`, and `payloadVersion`.
Outbox event payload는 `eventId`, `eventType`, `aggregateId`, `occurredAt`, `payloadVersion`을 포함해야 한다.

Kafka-specific code must live in infrastructure or application-level adapters, not in domain objects.
Kafka-specific 코드는 domain object가 아니라 infrastructure 또는 application-level adapter에 위치해야 한다.

### Acceptance Criteria
### 완료 기준

Message creation stores an outbox event in the same database transaction.
메시지 생성은 같은 데이터베이스 트랜잭션 안에서 outbox event를 저장한다.

Kafka outage does not fail message creation.
Kafka 장애는 메시지 생성을 실패시키지 않는다.

Pending events are published after Kafka becomes available again.
Kafka가 다시 사용 가능해지면 pending event가 발행된다.

Publisher retry behavior is deterministic and visible in the database.
publisher retry 동작은 결정적이며 데이터베이스에서 확인 가능해야 한다.

At least `conversation.message.created` is published to Kafka.
최소한 `conversation.message.created`가 Kafka로 발행된다.

### Metrics
### 지표

Track outbox pending count, oldest pending event age, publish success count, publish failure count, and publish retry count.
outbox pending count, oldest pending event age, publish success count, publish failure count, publish retry count를 추적한다.

## M4. Elasticsearch Message Search Projection
## M4. Elasticsearch 메시지 검색 Projection

### Goal
### 목표

Build a search projection for conversation messages using Kafka consumers and Elasticsearch.
Kafka consumer와 Elasticsearch를 사용해 conversation message 검색 projection을 만든다.

### Scenario
### 시나리오

An admin searches conversation messages by keyword, channel, conversation, sender type, and date range.
관리자는 keyword, channel, conversation, sender type, date range로 conversation message를 검색한다.

Message creation events are consumed from Kafka and indexed into Elasticsearch asynchronously.
메시지 생성 event는 Kafka에서 소비되어 Elasticsearch에 비동기로 색인된다.

### Scope
### 범위

Included: Kafka indexing consumer, Elasticsearch index mapping, Korean analyzer evaluation, message search API, filters, highlight, idempotent indexing, and reindex job.
포함: Kafka indexing consumer, Elasticsearch index mapping, Korean analyzer 검토, message search API, filter, highlight, 멱등 indexing, reindex job.

Excluded: advanced ranking, semantic search, cross-channel analytics, and admin search UI unless explicitly planned.
제외: advanced ranking, semantic search, cross-channel analytics, 명시적으로 계획되지 않은 admin search UI.

### Architecture Constraints
### 아키텍처 제약

The relational database remains the source of truth.
관계형 데이터베이스는 source of truth로 남는다.

Elasticsearch is a projection and may be rebuilt from database state.
Elasticsearch는 projection이며 database state로부터 재구축될 수 있어야 한다.

Consumers must be idempotent by `eventId` or deterministic document ID.
consumer는 `eventId` 또는 deterministic document ID를 기준으로 멱등해야 한다.

### Acceptance Criteria
### 완료 기준

New messages are searchable after Kafka consumption and Elasticsearch indexing.
새 메시지는 Kafka 소비와 Elasticsearch 색인 이후 검색 가능해야 한다.

Search supports keyword, channel ID, conversation ID, sender type, and date range filters.
검색은 keyword, channel ID, conversation ID, sender type, date range filter를 지원한다.

Duplicate event consumption does not create duplicate search documents.
중복 event 소비는 중복 search document를 생성하지 않는다.

Reindex can rebuild the message index from database records.
reindex는 database record로부터 message index를 재구축할 수 있다.

### Metrics
### 지표

Track indexing success count, indexing failure count, indexing lag, search latency, and reindex duration.
indexing success count, indexing failure count, indexing lag, search latency, reindex duration을 추적한다.

## M5. WebSocket Scale-out
## M5. WebSocket Scale-out

### Goal
### 목표

Support real-time message delivery when visitors and admins are connected to different application instances.
방문자와 관리자가 서로 다른 application instance에 연결되어 있어도 실시간 메시지 전달을 지원한다.

### Scenario
### 시나리오

A visitor connects to instance A, an admin connects to instance B, and messages are delivered across instances.
방문자는 instance A에 연결되고 관리자는 instance B에 연결되어도 메시지가 instance 간 전달된다.

A disconnected client reconnects and fetches missed messages safely.
연결이 끊긴 client는 재연결 후 누락된 메시지를 안전하게 조회한다.

### Scope
### 범위

Included: connection registry, subscription model, cross-node fanout via Kafka or Redis Pub/Sub, delivery consumer, reconnect handling, and missed message recovery.
포함: connection registry, subscription model, Kafka 또는 Redis Pub/Sub 기반 cross-node fanout, delivery consumer, reconnect handling, missed message recovery.

Excluded: mobile push notification, offline email notification, and global multi-region delivery.
제외: mobile push notification, offline email notification, global multi-region delivery.

### Architecture Constraints
### 아키텍처 제약

Connection state must not be assumed to exist only inside a single JVM when scale-out is enabled.
scale-out이 활성화된 경우 connection state가 단일 JVM 내부에만 존재한다고 가정하면 안 된다.

Delivery must tolerate at-least-once event handling.
delivery는 at-least-once event handling을 견딜 수 있어야 한다.

Reconnect must rely on persisted messages for recovery, not only transient delivery state.
reconnect 복구는 transient delivery state만이 아니라 persisted message에 의존해야 한다.

### Acceptance Criteria
### 완료 기준

Visitor-to-admin delivery works across two local application instances.
visitor-to-admin delivery는 두 개의 local application instance 사이에서 동작한다.

Admin-to-visitor delivery works across two local application instances.
admin-to-visitor delivery는 두 개의 local application instance 사이에서 동작한다.

Reconnect clients can fetch messages missed during disconnection.
reconnect client는 연결 해제 중 누락된 메시지를 조회할 수 있다.

Cross-node delivery behavior is documented with a local runbook.
cross-node delivery 동작은 local runbook으로 문서화된다.

### Metrics
### 지표

Track active sessions per node, delivery success count, delivery failure count, reconnect count, and missed message recovery count.
active sessions per node, delivery success count, delivery failure count, reconnect count, missed message recovery count를 추적한다.

## M6. Redis Presence and Rate Limiting
## M6. Redis Presence 및 Rate Limiting

### Goal
### 목표

Use Redis for distributed presence tracking and abuse-resistant message rate limiting.
Redis를 사용해 distributed presence tracking과 abuse-resistant message rate limiting을 구현한다.

### Scenario
### 시나리오

An admin sends heartbeat updates, and the system marks the admin online or offline using Redis TTL.
관리자가 heartbeat update를 보내면 시스템은 Redis TTL을 사용해 관리자를 online 또는 offline으로 표시한다.

A visitor sends too many messages in a short period and is rejected by a distributed rate limiter.
방문자가 짧은 시간에 너무 많은 메시지를 보내면 distributed rate limiter에 의해 거부된다.

### Scope
### 범위

Included: admin presence heartbeat, Redis TTL state, presence query, rate limit policy, visitor message rate limiting, channel quota basics, and Redis integration tests.
포함: admin presence heartbeat, Redis TTL state, presence query, rate limit policy, visitor message rate limiting, channel quota basics, Redis integration test.

Excluded: billing-grade quota enforcement, advanced abuse detection, and automatic bot detection.
제외: billing-grade quota enforcement, advanced abuse detection, automatic bot detection.

### Architecture Constraints
### 아키텍처 제약

Presence is transient state and must not become the source of truth for conversation ownership.
presence는 transient state이며 conversation ownership의 source of truth가 되면 안 된다.

Rate limit decisions must be explicit application policy, not hidden in controllers.
rate limit decision은 controller에 숨겨진 로직이 아니라 명시적인 application policy여야 한다.

Redis adapter code must remain outside domain objects.
Redis adapter 코드는 domain object 밖에 있어야 한다.

### Acceptance Criteria
### 완료 기준

Admin heartbeat creates or refreshes online state with TTL.
admin heartbeat는 TTL이 있는 online state를 생성하거나 갱신한다.

Missing heartbeat causes the admin to become offline after TTL expiry.
heartbeat가 없으면 TTL 만료 이후 admin은 offline 상태가 된다.

Visitor message rate limit rejects excessive sends across application instances.
visitor message rate limit은 application instance 간에도 과도한 전송을 거부한다.

Rate limit rejection returns a stable application error.
rate limit rejection은 안정적인 application error를 반환한다.

### Metrics
### 지표

Track online admin count, heartbeat count, presence expiry count, rate limit allow count, and rate limit reject count.
online admin count, heartbeat count, presence expiry count, rate limit allow count, rate limit reject count를 추적한다.

## M7. K6 Load Testing and Observability
## M7. K6 부하 테스트 및 Observability

### Goal
### 목표

Measure system behavior under load and make performance, reliability, and bottlenecks visible.
부하 상황에서 시스템 동작을 측정하고 성능, 신뢰성, 병목을 관측 가능하게 만든다.

### Scenario
### 시나리오

Many visitors send messages while admins stay connected through WebSocket and Kafka consumers process asynchronous work.
많은 방문자가 메시지를 보내는 동안 관리자들은 WebSocket에 연결되어 있고 Kafka consumer는 비동기 작업을 처리한다.

The team can explain throughput, p95 latency, error rate, Kafka lag, and database bottlenecks with evidence.
팀은 throughput, p95 latency, error rate, Kafka lag, database bottleneck을 근거와 함께 설명할 수 있다.

### Scope
### 범위

Included: k6 HTTP tests, k6 WebSocket tests, baseline load scenario, Micrometer metrics, Grafana dashboard, Kafka consumer lag monitoring, and performance report.
포함: k6 HTTP test, k6 WebSocket test, baseline load scenario, Micrometer metric, Grafana dashboard, Kafka consumer lag monitoring, performance report.

Excluded: cloud-scale benchmark claims, multi-region tests, and premature microservice extraction.
제외: cloud-scale benchmark claim, multi-region test, premature microservice extraction.

### Architecture Constraints
### 아키텍처 제약

Performance claims must be backed by reproducible scripts and documented environment conditions.
성능 주장은 재현 가능한 script와 문서화된 환경 조건으로 뒷받침되어야 한다.

Load tests must not require production secrets or unmanaged external services.
부하 테스트는 production secret이나 관리되지 않는 외부 서비스에 의존하면 안 된다.

Metrics must use stable names so dashboard panels remain useful across refactors.
metric은 refactor 이후에도 dashboard panel이 유지될 수 있도록 안정적인 이름을 사용해야 한다.

### Acceptance Criteria
### 완료 기준

k6 can run a baseline visitor message send scenario.
k6는 baseline visitor message send 시나리오를 실행할 수 있다.

k6 can run a baseline WebSocket connection and message receive scenario.
k6는 baseline WebSocket connection 및 message receive 시나리오를 실행할 수 있다.

The report includes throughput, p95 latency, p99 latency, error rate, Kafka lag, and database observations.
report는 throughput, p95 latency, p99 latency, error rate, Kafka lag, database observation을 포함한다.

At least one bottleneck is identified and either fixed or documented as future work.
최소 하나의 병목이 식별되고 수정되거나 future work로 문서화된다.

### Metrics
### 지표

Track request throughput, p95 latency, p99 latency, error rate, active WebSocket connections, Kafka consumer lag, database query latency, and JVM resource usage.
request throughput, p95 latency, p99 latency, error rate, active WebSocket connections, Kafka consumer lag, database query latency, JVM resource usage를 추적한다.

## Suggested Ownership Split
## 추천 담당 분리

Owner A focuses on core application flows, domain rules, database transactions, persistence, migrations, and concurrency tests.
담당자 A는 core application flow, domain rule, database transaction, persistence, migration, concurrency test에 집중한다.

Owner B focuses on Kafka, Elasticsearch, Redis, WebSocket infrastructure, local runtime, k6, and observability.
담당자 B는 Kafka, Elasticsearch, Redis, WebSocket infrastructure, local runtime, k6, observability에 집중한다.

Both owners must review each other's design notes and PRs to avoid isolated knowledge ownership.
두 담당자는 지식이 한쪽에 고립되지 않도록 서로의 design note와 PR을 리뷰해야 한다.

## PR Checklist
## PR 체크리스트

The PR explains which scenario is covered.
PR은 어떤 시나리오를 다루는지 설명한다.

The PR explains important architecture decisions.
PR은 중요한 아키텍처 결정을 설명한다.

The PR identifies transaction boundaries.
PR은 transaction boundary를 식별한다.

The PR documents failure cases and retry behavior.
PR은 failure case와 retry behavior를 문서화한다.

The PR includes tests for success and failure paths.
PR은 success path와 failure path 테스트를 포함한다.

The PR includes metrics, logs, or traces needed to operate the feature.
PR은 기능 운영에 필요한 metric, log, trace를 포함한다.

The PR updates related docs when behavior, invariants, tables, events, or operational flows change.
PR은 behavior, invariant, table, event, operational flow가 바뀔 때 관련 문서를 업데이트한다.


## PR Template
```aiignore
## What
## Scenario Covered
## Architecture Decisions
## Failure Cases Covered
## Tests
## Metrics/Logs
## Docs Updated
## Remaining Work
```
