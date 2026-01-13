# 패치노트 - 2026-01-13

## 목표했던 내용 1

- 로그인 실패 추적 기능의 Race Condition 문제 확인 및 수정

## 변경사항 1

### 테스트 코드 작성

- `LoginFailureE2ETest.kt`: Race condition E2E 테스트 작성
  - 초기 실패 횟수 4에서 20개 스레드 동시 요청
  - HTTP 응답 Body 기준 아래 결과를 목표함 (assert)
    - USER_001(로그인 실패) 1개
    - LOGIN_FAILUIR_001(반복 요청 차단) 19개
  - Redis 결과 아래 결과를 목표함
    - 실패 카운트 5 이상,
    - TTL은 "테스트 시작 전 시간 + 15분"과 "테스트 마지막 시간 +15분" 사이

### 1차 테스트 결과

- HTTP 응답 통계:
  - USER_001(로그인 실패): 20개 (예상: 1개)
  - LOGIN_FAILURE_001(잠금): 0개 (예상: 19개)
  - 기타 에러: 0개
- Redis 결과:
  - 실패 카운트: 9 (예상: 5 이상)
  - TTL: 899초
- 문제점: 모든 요청이 USER_001로 응답되어 잠금이 작동하지 않음. 실패 카운트가 9까지 증가하여 race condition 발생 확인

### 1차 테스트 분석

- Redis 실패 카운트 분석:
  - 초기 실패 횟수: 4
  - 동시 요청 스레드: 20개
  - 예상 최종 카운트: 24 (4 + 20)
  - 실제 최종 카운트: 9
  - 누락된 증가: 15개 (24 - 9)
- 원인 추정:
  - read-modify-write 패턴에서 여러 스레드가 동시에 같은 값(4)을 읽음
  - 각 스레드가 독립적으로 증가시켜 저장하면서 이전 증가가 덮어써짐
  - 예: Thread 1~5가 모두 count=4를 읽고 각각 5로 저장 → 실제로는 1번만 증가한 것으로 기록됨

### 1차 수정

- read-modify-write 패턴 제거:
  - `LoginFailureRepository`에 `incrementAndGet(key: String, ttl: Duration): Int` 메서드 추가
  - Redis의 원자적 증가 연산(`INCR`) 사용: `RedisTemplate<String, Long>.opsForValue().increment()`
  - `LoginFailureTracker.incrementFailureCount()`에서 새 메서드 사용하여 원자적 증가 보장

### 2차 테스트 결과

- HTTP 응답 통계:
  - USER_001(로그인 실패): 20개 (예상: 1개)
  - LOGIN_FAILURE_001(잠금): 0개 (예상: 19개)
  - 기타 에러: 0개
- Redis 결과:
  - 실패 카운트: 24 (예상: 24, 정확히 일치)
  - TTL: 899초
- 분석:
  - Redis 실패 카운트는 정확히 24로 증가하여 race condition 해결 확인
  - 하지만 HTTP 응답은 여전히 모든 요청이 USER_001로 응답되어 잠금이 작동하지 않음
  - 원인 추정: `checkAndThrowIfLocked()` 호출 시점과 `incrementFailureCount()` 호출 시점 사이의 타이밍 이슈 가능성