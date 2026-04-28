# Milestone 1. Core Conversation MVP
# 마일스톤 1. Core Conversation MVP

This document is a working completion log for Milestone 1.
이 문서는 Milestone 1을 진행하면서 채워 넣는 완료 기록이다.

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

Deliver the basic Channel Talk-like conversation flow from visitor widget to admin inbox.
방문자 위젯에서 관리자 인박스까지 이어지는 기본 채널톡형 상담 흐름을 완성한다.

This milestone proves that the product has a working end-to-end conversation loop before adding Kafka, Elasticsearch, Redis, or scale-out concerns.
이 마일스톤은 Kafka, Elasticsearch, Redis, scale-out concern을 추가하기 전에 제품의 end-to-end conversation loop가 동작함을 증명한다.

## Scenario Summary
## 시나리오 요약

A visitor opens the widget, gets or restores a visitor session, sends a message, and sees the admin reply in real time.
방문자가 위젯을 열고 visitor session을 생성하거나 복구한 뒤 메시지를 보내고, 관리자 답장을 실시간으로 확인한다.

An admin opens the inbox, sees the visitor conversation, sends a reply, and sees read/unread state reflected.
관리자는 인박스를 열어 방문자 상담을 확인하고 답장을 보내며 읽음/안 읽음 상태가 반영되는 것을 확인한다.

## Scope
## 범위

### Included
### 포함

- [ ] Visitor session bootstrap
- [ ] Visitor session bootstrap
- [ ] Visitor message send
- [ ] Visitor message send
- [ ] Admin message send
- [ ] Admin message send
- [ ] Conversation creation or reopen
- [ ] Conversation 생성 또는 재오픈
- [ ] Message persistence
- [ ] Message persistence
- [ ] Basic WebSocket push within a single application instance
- [ ] 단일 application instance 안에서의 기본 WebSocket push
- [ ] Admin inbox query
- [ ] Admin inbox query
- [ ] Basic read/unread state
- [ ] 기본 읽음/안 읽음 상태

### Excluded
### 제외

- [ ] Kafka delivery
- [ ] Kafka delivery
- [ ] Elasticsearch search
- [ ] Elasticsearch search
- [ ] Multi-node WebSocket scale-out
- [ ] Multi-node WebSocket scale-out
- [ ] Redis presence
- [ ] Redis presence
- [ ] Load testing
- [ ] Load testing
- [ ] Message send idempotency
- [ ] Message send 멱등성
- [ ] Advanced assignment concurrency handling
- [ ] 고급 배정 동시성 처리

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

### Failure Handling
### 실패 처리

TODO

### Dependency Direction
### 의존성 방향

TODO

### Test Plan
### 테스트 계획

TODO

### Demo Plan
### 데모 계획

TODO

## Architecture Notes
## 아키텍처 메모

The implementation should keep presentation concerns outside the core domain.
구현은 presentation concern을 core domain 밖에 유지해야 한다.

Application facades should own use-case orchestration and transaction boundaries.
Application facade는 use-case orchestration과 transaction boundary를 소유해야 한다.

Domain objects should remain independent from Spring WebSocket, JPA repositories, and transport DTOs.
Domain object는 Spring WebSocket, JPA repository, transport DTO로부터 독립적이어야 한다.

Infrastructure adapters may depend on application ports and domain models.
Infrastructure adapter는 application port와 domain model에 의존할 수 있다.

Presentation adapters should call application facades or query services.
Presentation adapter는 application facade 또는 query service를 호출해야 한다.

## Main Flows
## 주요 흐름

### Visitor Bootstrap
### 방문자 Bootstrap

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Request validates channel integration.
1. 요청은 channel integration을 검증한다.
2. Visitor is created or restored.
2. visitor를 생성하거나 복구한다.
3. Visitor session is created or restored.
3. visitor session을 생성하거나 복구한다.
4. Session token is returned to the widget.
4. session token을 widget에 반환한다.

Implementation notes:
구현 메모:

TODO

### Visitor Message Send
### 방문자 메시지 전송

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Visitor session is validated.
1. visitor session을 검증한다.
2. Conversation is created or reopened.
2. conversation을 생성하거나 재오픈한다.
3. Message content is validated.
3. message content를 검증한다.
4. Message is persisted.
4. message를 저장한다.
5. Connected admins receive WebSocket delivery.
5. 연결된 admin은 WebSocket delivery를 받는다.

Implementation notes:
구현 메모:

TODO

### Admin Message Send
### 관리자 메시지 전송

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Admin identity is extracted by presentation.
1. admin identity는 presentation에서 추출한다.
2. Admin channel membership is validated.
2. admin channel membership을 검증한다.
3. Message content is validated.
3. message content를 검증한다.
4. Message is persisted.
4. message를 저장한다.
5. Connected visitor receives WebSocket delivery.
5. 연결된 visitor는 WebSocket delivery를 받는다.

Implementation notes:
구현 메모:

TODO

### Admin Inbox Query
### 관리자 인박스 조회

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Admin identity is extracted by presentation.
1. admin identity는 presentation에서 추출한다.
2. Admin channel access is validated.
2. admin channel access를 검증한다.
3. Conversations are loaded by recent activity.
3. conversation을 최근 활동 기준으로 조회한다.
4. Cursor pagination is applied.
4. cursor pagination을 적용한다.
5. Read/unread state is included in the response.
5. response에 읽음/안 읽음 상태를 포함한다.

Implementation notes:
구현 메모:

TODO

### Read and Unread State
### 읽음 및 안 읽음 상태

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. New visitor messages affect admin unread state.
1. 새 visitor message는 admin unread state에 영향을 준다.
2. New admin messages affect visitor unread state if visitor read state is tracked.
2. visitor read state를 추적한다면 새 admin message는 visitor unread state에 영향을 준다.
3. Read action updates the relevant read marker.
3. read action은 관련 read marker를 갱신한다.
4. Query responses expose read/unread state.
4. query response는 읽음/안 읽음 상태를 노출한다.

Implementation notes:
구현 메모:

TODO

## Acceptance Criteria
## 완료 기준

- [ ] Visitor message send stores a conversation message in the database.
- [ ] 방문자 메시지 전송은 conversation message를 데이터베이스에 저장한다.
- [ ] Admin message send stores a conversation message in the database.
- [ ] 관리자 메시지 전송은 conversation message를 데이터베이스에 저장한다.
- [ ] Visitor and admin clients receive basic WebSocket message delivery when connected to the same application instance.
- [ ] 방문자와 관리자 client는 같은 application instance에 연결되어 있을 때 기본 WebSocket message delivery를 받는다.
- [ ] Admin inbox returns conversations ordered by recent activity with cursor-based pagination.
- [ ] 관리자 인박스는 최근 활동 기준 정렬과 cursor 기반 pagination으로 conversation을 반환한다.
- [ ] Read and unread state is visible through admin-facing query responses.
- [ ] 읽음/안 읽음 상태는 관리자용 query response에서 확인할 수 있다.

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

1. Create or select a channel integration.
1. channel integration을 생성하거나 선택한다.
2. Bootstrap visitor session from widget API.
2. widget API로 visitor session을 bootstrap한다.
3. Connect visitor WebSocket client.
3. visitor WebSocket client를 연결한다.
4. Connect admin WebSocket client.
4. admin WebSocket client를 연결한다.
5. Send visitor message.
5. visitor message를 전송한다.
6. Open or query admin inbox.
6. admin inbox를 열거나 조회한다.
7. Send admin reply.
7. admin reply를 전송한다.
8. Confirm WebSocket delivery and database records.
8. WebSocket delivery와 database record를 확인한다.

Evidence:
증거:

- API response: TODO
- API response: TODO
- WebSocket frame: TODO
- WebSocket frame: TODO
- Database rows: TODO
- Database rows: TODO
- Screenshot or recording: TODO
- Screenshot 또는 recording: TODO

## Metrics
## 지표

| Metric | Result | Notes |
| --- | ---: | --- |
| Message send count | TODO | TODO |
| Message send failure count | TODO | TODO |
| Active WebSocket connections | TODO | TODO |
| Inbox query latency | TODO | TODO |

| 지표 | 결과 | 메모 |
| --- | ---: | --- |
| 메시지 전송 수 | TODO | TODO |
| 메시지 전송 실패 수 | TODO | TODO |
| 활성 WebSocket 연결 수 | TODO | TODO |
| 인박스 조회 지연 시간 | TODO | TODO |

## Key Files
## 주요 파일

### Presentation
### Presentation

- TODO
- TODO

### Application
### Application

- TODO
- TODO

### Domain
### Domain

- TODO
- TODO

### Infrastructure
### Infrastructure

- TODO
- TODO

### Tests
### Tests

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

- Kafka-based delivery is not implemented in this milestone.
- Kafka 기반 delivery는 이 마일스톤에서 구현하지 않는다.
- WebSocket delivery is limited to a single application instance.
- WebSocket delivery는 단일 application instance로 제한된다.
- Message send idempotency is handled in Milestone 2.
- message send 멱등성은 Milestone 2에서 다룬다.
- Elasticsearch message search is handled in a later milestone.
- Elasticsearch message search는 이후 마일스톤에서 다룬다.
- Redis presence and distributed rate limiting are handled in a later milestone.
- Redis presence와 distributed rate limiting은 이후 마일스톤에서 다룬다.

Additional gaps found during implementation:
구현 중 발견한 추가 한계:

- TODO
- TODO

## Follow-up Milestones
## 후속 마일스톤

- M2. DB Concurrency and Idempotency
- M2. DB 동시성 및 멱등성
- M3. Outbox Pattern and Kafka Publishing
- M3. Outbox Pattern 및 Kafka 발행
- M4. Elasticsearch Message Search Projection
- M4. Elasticsearch 메시지 검색 Projection

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
