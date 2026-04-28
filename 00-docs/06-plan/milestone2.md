# Milestone 2. DB Concurrency and Idempotency
# 마일스톤 2. DB 동시성 및 멱등성

This document is a working completion log for Milestone 2.
이 문서는 Milestone 2를 진행하면서 채워 넣는 완료 기록이다.

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

Make core write flows safe under duplicate requests and concurrent user actions.
중복 요청과 동시 사용자 액션 상황에서도 core write flow가 안전하게 동작하도록 만든다.

This milestone proves that core conversation writes can survive retries, races, and invalid state transitions before asynchronous infrastructure is added.
이 마일스톤은 비동기 인프라를 추가하기 전에 core conversation write가 retry, race, invalid state transition을 견딜 수 있음을 증명한다.

## Scenario Summary
## 시나리오 요약

A visitor retries the same message request due to a network timeout and receives the same result instead of creating duplicates.
방문자가 네트워크 타임아웃으로 같은 메시지 요청을 재시도해도 중복 생성 대신 같은 결과를 받는다.

Two admins attempt to assign the same conversation at the same time, and only one assignment succeeds.
두 명의 관리자가 같은 conversation을 동시에 배정하려고 할 때 하나의 배정만 성공한다.

Invalid conversation state transitions are rejected with explicit application errors.
유효하지 않은 conversation 상태 전이는 명시적인 application error로 거부된다.

## Scope
## 범위

### Included
### 포함

- [ ] `clientMessageId` for visitor message send
- [ ] visitor message send를 위한 `clientMessageId`
- [ ] `clientMessageId` for admin message send if needed
- [ ] 필요 시 admin message send를 위한 `clientMessageId`
- [ ] Idempotent message send result
- [ ] 멱등 message send result
- [ ] Database unique constraints for duplicate prevention
- [ ] 중복 방지를 위한 database unique constraint
- [ ] Conversation assignment concurrency handling
- [ ] conversation 배정 동시성 처리
- [ ] Conversation state transition validation
- [ ] conversation 상태 전이 검증
- [ ] Integration tests for duplicate requests
- [ ] 중복 요청 integration test
- [ ] Integration tests for concurrent assignment
- [ ] 동시 배정 integration test

### Excluded
### 제외

- [ ] Kafka event idempotency
- [ ] Kafka event 멱등성
- [ ] Elasticsearch indexing idempotency
- [ ] Elasticsearch indexing 멱등성
- [ ] Distributed locks unless proven necessary
- [ ] 필요성이 증명되지 않은 distributed lock
- [ ] WebSocket delivery deduplication
- [ ] WebSocket delivery deduplication
- [ ] Load test scale validation
- [ ] load test 규모 검증

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

### Idempotency Key Design
### 멱등 키 설계

TODO

### Database Constraints
### 데이터베이스 제약

TODO

### Concurrency Strategy
### 동시성 전략

TODO

### Failure Handling
### 실패 처리

TODO

### Test Plan
### 테스트 계획

TODO

## Architecture Notes
## 아키텍처 메모

Prefer database constraints and transaction boundaries before introducing distributed locks.
distributed lock을 도입하기 전에 database constraint와 transaction boundary를 우선한다.

Expected application failures should use core-specific errors, not generic exceptions.
예상 가능한 application failure는 generic exception이 아니라 core-specific error를 사용해야 한다.

Application facades should define the consistency boundary for each write use case.
Application facade는 각 write use case의 consistency boundary를 정의해야 한다.

Repository methods should expose explicit create/update semantics when the use case already knows the intent.
use case가 의도를 알고 있다면 repository method는 명시적인 create/update semantic을 노출해야 한다.

## Main Flows
## 주요 흐름

### Idempotent Visitor Message Send
### 멱등 방문자 메시지 전송

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Request includes `clientMessageId`.
1. 요청은 `clientMessageId`를 포함한다.
2. Application validates visitor session and message content.
2. application은 visitor session과 message content를 검증한다.
3. Application checks whether the same message was already stored.
3. application은 같은 message가 이미 저장되었는지 확인한다.
4. Existing message result is returned for duplicate requests.
4. 중복 요청에는 기존 message result를 반환한다.
5. New message is stored only once.
5. 새 message는 한 번만 저장한다.

Implementation notes:
구현 메모:

TODO

### Idempotent Admin Message Send
### 멱등 관리자 메시지 전송

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Request includes `clientMessageId` if admin retry behavior is supported.
1. admin retry behavior를 지원한다면 요청은 `clientMessageId`를 포함한다.
2. Application validates admin membership and message content.
2. application은 admin membership과 message content를 검증한다.
3. Existing message result is returned for duplicate requests.
3. 중복 요청에는 기존 message result를 반환한다.
4. New message is stored only once.
4. 새 message는 한 번만 저장한다.

Implementation notes:
구현 메모:

TODO

### Concurrent Conversation Assignment
### 동시 Conversation 배정

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Two admins attempt assignment for the same conversation.
1. 두 admin이 같은 conversation에 대해 배정을 시도한다.
2. Application validates channel membership and conversation state.
2. application은 channel membership과 conversation state를 검증한다.
3. Database constraint, optimistic locking, or conditional update allows only one success.
3. database constraint, optimistic locking, conditional update 중 하나로 하나의 성공만 허용한다.
4. Losing request receives an explicit conflict error.
4. 실패한 요청은 명시적인 conflict error를 받는다.

Implementation notes:
구현 메모:

TODO

### Conversation State Transition Validation
### Conversation 상태 전이 검증

Entry points:
진입점:

- TODO
- TODO

Flow:
흐름:

1. Application loads the conversation state.
1. application은 conversation state를 조회한다.
2. Domain or application policy validates the requested transition.
2. domain 또는 application policy가 요청된 전이를 검증한다.
3. Invalid transitions are rejected before persistence.
3. 유효하지 않은 전이는 persistence 전에 거부한다.
4. Valid transitions are persisted in the same transaction.
4. 유효한 전이는 같은 transaction 안에서 저장한다.

Implementation notes:
구현 메모:

TODO

## Acceptance Criteria
## 완료 기준

- [ ] Duplicate `clientMessageId` requests return the original stored message result.
- [ ] 중복 `clientMessageId` 요청은 기존에 저장된 message result를 반환한다.
- [ ] Duplicate request handling is backed by a database-level uniqueness guarantee.
- [ ] 중복 요청 처리는 database-level uniqueness guarantee로 뒷받침된다.
- [ ] Concurrent assignment requests cannot assign the same conversation to multiple admins.
- [ ] 동시 배정 요청은 같은 conversation을 여러 admin에게 배정할 수 없다.
- [ ] Losing concurrent assignment requests receive an explicit application error.
- [ ] 동시 배정에서 실패한 요청은 명시적인 application error를 받는다.
- [ ] Closed or invalid conversation state transitions are rejected with explicit application errors.
- [ ] 종료되었거나 유효하지 않은 conversation 상태 전이는 명시적인 application error로 거부된다.
- [ ] Integration tests prove duplicate request and concurrent assignment scenarios.
- [ ] integration test는 중복 요청과 동시 배정 시나리오를 증명한다.

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

1. Send the same visitor message request twice with the same `clientMessageId`.
1. 같은 `clientMessageId`로 같은 visitor message request를 두 번 보낸다.
2. Confirm that only one database message row is created.
2. database message row가 하나만 생성되었는지 확인한다.
3. Confirm that both responses point to the same message.
3. 두 response가 같은 message를 가리키는지 확인한다.
4. Trigger two concurrent assignment requests for the same conversation.
4. 같은 conversation에 대해 두 개의 동시 배정 요청을 발생시킨다.
5. Confirm that only one assignment succeeds.
5. 하나의 배정만 성공했는지 확인한다.
6. Trigger an invalid state transition.
6. 유효하지 않은 상태 전이를 발생시킨다.
7. Confirm that a stable application error is returned.
7. 안정적인 application error가 반환되는지 확인한다.

Evidence:
증거:

- API response: TODO
- API response: TODO
- Database rows: TODO
- Database rows: TODO
- Test output: TODO
- Test output: TODO

## Metrics
## 지표

| Metric | Result | Notes |
| --- | ---: | --- |
| Idempotency hit count | TODO | TODO |
| Optimistic lock failure count | TODO | TODO |
| Duplicate request count | TODO | TODO |
| Assignment conflict count | TODO | TODO |

| 지표 | 결과 | 메모 |
| --- | ---: | --- |
| 멱등 hit 수 | TODO | TODO |
| optimistic lock 실패 수 | TODO | TODO |
| 중복 요청 수 | TODO | TODO |
| 배정 conflict 수 | TODO | TODO |

## Key Files
## 주요 파일

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

### Migrations
### Migrations

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

- Kafka consumer idempotency is handled in Milestone 3 or later.
- Kafka consumer 멱등성은 Milestone 3 또는 이후에 다룬다.
- Elasticsearch indexing idempotency is handled in Milestone 4.
- Elasticsearch indexing 멱등성은 Milestone 4에서 다룬다.
- Distributed lock usage is intentionally avoided unless database-level protection is insufficient.
- database-level protection이 충분하지 않은 경우가 아니라면 distributed lock 사용은 의도적으로 피한다.

Additional gaps found during implementation:
구현 중 발견한 추가 한계:

- TODO
- TODO

## Follow-up Milestones
## 후속 마일스톤

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
