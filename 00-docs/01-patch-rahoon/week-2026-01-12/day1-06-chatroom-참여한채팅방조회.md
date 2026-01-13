# 패치노트 - 2026-01-12

## ChatRoom 참여한 채팅방 조회 기능 개선

### 목표

`GET /chat-rooms`를 "내가 생성한 채팅방"에서 "내가 참여한 채팅방"으로 변경하고, N+1 문제를 해결합니다.

### 구현 내용

**Domain 레이어:**
- `ChatRoomRepository`: `findByIds()` 배치 조회 메서드 추가
- `ChatRoomDomainService`: `getByIds()` 메서드 추가

**Application 레이어:**
- `ChatRoomApplicationService`: `getByMemberUserId()` 추가
  - 멤버 정보 조회 → 채팅방 ID 추출 → 배치 조회로 N+1 문제 해결

**Controller 레이어:**
- `ChatRoomController`: `getMyChatRooms()` 동작 변경
  - `getByCreatedByUserId()` → `getByMemberUserId()` 호출

**테스트:**
- `ChatRoomApplicationServiceTest`: 단위 테스트 추가
- `ChatRoomControllerE2ETest`: E2E 테스트 추가

### API 변경

- `GET /chat-rooms`: 내가 생성한 채팅방 → 내가 참여한 채팅방 목록 조회

### 성능 개선

- **변경 전**: 멤버 조회 1번 + 채팅방 조회 N번 = N+1번 쿼리
- **변경 후**: 멤버 조회 1번 + 채팅방 배치 조회 1번 = 2번 쿼리

### 설계 결정

1. RESTful 설계: 인증된 사용자의 참여 채팅방만 반환 (일반적인 메신저 앱 패턴)
2. N+1 해결: `findByIds()` 배치 조회로 쿼리 수 최적화

---

## AI 피드백

### 잘한 점

1. **N+1 문제 해결**: 배치 조회(`findByIds`)로 쿼리 수를 N+1에서 2로 줄여 성능 개선
2. **레이어 분리**: Domain, Application, Controller 레이어 책임 분리 유지
3. **확장성 고려**: Repository 인터페이스에 `findByIds()` 추가로 재사용성 확보
4. **테스트 커버리지**: 단위 테스트와 E2E 테스트 모두 작성
5. **명확한 문서화**: 성능 개선 효과를 수치로 명시

### 아쉬운 점

1. **빈 리스트 처리**: `chatRoomIds`가 비어있을 때의 처리 로직이 명시되지 않음 (현재는 빈 리스트 반환하지만 명시적 처리 권장)
2. **정렬 순서**: 반환되는 채팅방 목록의 정렬 기준이 불명확 (최신 참여순? 생성일순?)
3. **페이징 부재**: 참여한 채팅방이 많을 경우 성능 이슈 가능성
4. **기존 메서드 유지**: `getByCreatedByUserId()` 메서드가 남아있어 혼란 가능성 (deprecated 표시 또는 제거 고려)
5. **에러 처리**: `findByIds()`에서 일부 ID가 존재하지 않을 경우의 처리 로직 불명확

---

## 추가로 하면 좋을 것

### 단기 개선 (우선순위 높음)

1. **빈 리스트 명시적 처리**
   - `getByMemberUserId()`에서 `chatRoomIds.isEmpty()` 체크 추가
   - 조기 반환으로 불필요한 쿼리 방지

2. **정렬 기준 명확화**
   - 비즈니스 요구사항에 맞는 정렬 기준 정의 (예: 최신 메시지 순, 참여일 순)
   - `ChatRoomRepository.findByIds()`에 정렬 파라미터 추가 또는 Application 레이어에서 정렬

3. **기존 메서드 정리**
   - `getByCreatedByUserId()` 메서드에 `@Deprecated` 어노테이션 추가
   - 또는 사용처가 없다면 제거 고려

### 중기 개선 (확장성 고려)

4. **페이징 처리**
   - `GET /chat-rooms?page=0&size=20` 형태로 페이징 파라미터 추가
   - `ChatRoomMemberRepository`에 페이징 지원 메서드 추가
   - 응답에 페이징 메타데이터 포함 (totalCount, hasNext 등)

5. **정렬 옵션 제공**
   - `GET /chat-rooms?sort=latestMessage,desc` 형태로 정렬 옵션 제공
   - 다양한 정렬 기준 지원 (최신 메시지, 참여일, 채팅방명 등)

6. **에러 처리 강화**
   - `findByIds()`에서 일부 ID가 존재하지 않을 경우 로깅
   - 데이터 불일치 감지 및 알림 메커니즘

### 장기 개선 (성능 최적화)

7. **캐싱 전략**
   - 사용자별 참여 채팅방 목록 캐싱 (Redis 등)
   - 채팅방 멤버 변경 시 캐시 무효화

8. **JOIN 쿼리 최적화**
   - 현재는 2번의 쿼리지만, 필요시 JOIN을 통한 단일 쿼리로 최적화 가능
   - `ChatRoomMemberRepository`에 JOIN 기반 조회 메서드 추가 검토

9. **인덱스 최적화**
   - `ChatRoomMember` 테이블의 `userId` 컬럼에 인덱스 확인
   - 복합 인덱스 필요성 검토 (userId + createdAt 등)
