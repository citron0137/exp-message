# 패치노트 - 2026-01-12

## Message RESTful API 개선

### 목표

Message API 엔드포인트를 RESTful 설계 원칙에 맞게 개선하여 리소스 계층 구조를 명확히 하고, 확장 가능한 API 설계를 제공합니다.

### 구현 내용

#### 1. Controller 레이어 - 엔드포인트 변경

**수정된 파일:**
- `MessageController.kt`: 채팅방별 메시지 목록 조회 엔드포인트 변경
  - `GET /messages/chat-rooms/{chatRoomId}` → `GET /messages?chatRoomId={chatRoomId}`
  - `@PathVariable` → `@RequestParam`으로 변경
  - `@GetMapping(params = ["chatRoomId"])`로 쿼리 파라미터 기반 매핑 설정

**변경 사항:**
- `@RequestParam` import 추가
- `getByChatRoomId()` 메서드의 파라미터 타입 변경: `@PathVariable` → `@RequestParam`
- `@GetMapping`에 `params = ["chatRoomId"]` 속성 추가하여 경로 충돌 방지

### 설계 결정

1. **Message를 독립적인 리소스로 취급**
   - Message는 ChatRoomMember와 달리 독립적으로 사용될 수 있는 리소스
   - 이미 `POST /messages`, `GET /messages/{id}`로 독립적인 리소스로 다루고 있음
   - 컬렉션 필터링 방식이 더 적합함

2. **쿼리 파라미터를 통한 필터링**
   - RESTful API에서 컬렉션의 하위 집합을 조회하는 표준 방식
   - 향후 확장성 고려: `GET /messages?userId={userId}`, `GET /messages?keyword={keyword}` 등 추가 가능
   - 복합 필터링도 자연스럽게 지원 가능: `GET /messages?chatRoomId={id}&userId={id}`

3. **도메인 특성에 따른 차별화**
   - ChatRoomMember: 채팅방에 종속적인 리소스 → 하위 리소스 패턴 (`/chat-rooms/{id}/members`)
   - Message: 독립적이면서도 다양한 관점에서 조회 가능 → 컬렉션 필터링 패턴 (`/messages?chatRoomId={id}`)

### 변경 전후 비교

#### 변경 전
```kotlin
/**
 * 채팅방별 메시지 목록 조회
 * GET /messages/chat-rooms/{chatRoomId}
 */
@GetMapping("/chat-rooms/{chatRoomId}")
@AuthInfoAffect(required = true)
fun getByChatRoomId(
    @PathVariable chatRoomId: String,
    authInfo: AuthInfo
): ResponseEntity<ApiResponse<List<MessageResponse.Detail>>>
```

**문제점:**
- 리소스 계층 구조 위반: 메시지의 하위에 채팅방이 있는 것처럼 보임
- ChatRoomMember와의 일관성 부족
- 의미론적으로 혼란스러운 구조

#### 변경 후
```kotlin
/**
 * 채팅방별 메시지 목록 조회
 * GET /messages?chatRoomId={chatRoomId}
 */
@GetMapping(params = ["chatRoomId"])
@AuthInfoAffect(required = true)
fun getByChatRoomId(
    @RequestParam chatRoomId: String,
    authInfo: AuthInfo
): ResponseEntity<ApiResponse<List<MessageResponse.Detail>>>
```

**개선점:**
- RESTful 설계 원칙 준수: 리소스는 명사로 표현, 필터링은 쿼리 파라미터로
- Message의 독립성 명확히 표현
- 향후 확장성 확보

### API 엔드포인트

#### 변경 전
- `POST /messages` - 메시지 전송
- `GET /messages/{id}` - 메시지 조회
- `GET /messages/chat-rooms/{chatRoomId}` - 채팅방별 메시지 목록 조회 ❌

#### 변경 후
- `POST /messages` - 메시지 전송
- `GET /messages/{id}` - 메시지 조회
- `GET /messages?chatRoomId={chatRoomId}` - 채팅방별 메시지 목록 조회 ✅

### RESTful 설계 원칙 준수

1. **리소스는 명사로 표현**: `/messages` ✅
2. **필터링은 쿼리 파라미터로**: `?chatRoomId={id}` ✅
3. **리소스 계층 구조 명확성**: Message는 독립적인 리소스 ✅
4. **일관성**: 기존 `POST /messages`, `GET /messages/{id}`와 일관된 패턴 ✅

### 향후 확장 가능성

다음과 같은 엔드포인트를 자연스럽게 추가할 수 있습니다:

```
GET /messages?chatRoomId={chatRoomId}        # 채팅방별 (현재)
GET /messages?userId={userId}                # 사용자별 (향후)
GET /messages?keyword={keyword}               # 검색 (향후)
GET /messages?chatRoomId={id}&userId={id}    # 복합 필터 (향후)
```

### 해결된 이슈

1. **RESTful 설계 원칙 위반** ✅
   - 리소스 계층 구조를 명확히 하여 RESTful 원칙 준수
   - 의미론적으로 명확한 API 구조

2. **확장성 부족** ✅
   - 쿼리 파라미터 방식으로 다양한 필터링 조건 추가 가능
   - 복합 필터링도 자연스럽게 지원

3. **일관성 부족** ✅
   - Message의 독립성을 명확히 표현
   - 기존 API 패턴과 일관성 유지

### 테스트 결과

- ✅ 기존 Application Service 테스트는 변경 없이 통과 (Controller 레이어 변경이므로 영향 없음)
- ✅ 린터 오류 없음
- ⚠️ 향후 E2E 테스트 추가 시 새로운 엔드포인트로 작성 필요

### AI 피드백

#### 잘한 점

1. **RESTful 설계 원칙 준수**
   - 리소스 계층 구조를 명확히 하여 RESTful 원칙을 준수
   - 쿼리 파라미터를 통한 필터링으로 표준적인 API 설계

2. **도메인 특성 반영**
   - Message의 독립성을 고려한 설계 결정
   - ChatRoomMember와의 차별화를 통해 각 도메인의 특성 반영

3. **확장성 고려**
   - 향후 다양한 필터링 조건 추가 가능
   - 복합 필터링도 자연스럽게 지원 가능한 구조

4. **일관성 유지**
   - 기존 `POST /messages`, `GET /messages/{id}`와 일관된 패턴
   - Message를 독립적인 리소스로 일관되게 다룸

#### 아쉬운 점

1. **E2E 테스트 부재**
   - Controller 레이어 변경에 대한 E2E 테스트가 없음
   - 향후 E2E 테스트 추가 필요

2. **API 문서화**
   - Swagger/OpenAPI 문서 업데이트 필요
   - 엔드포인트 변경에 대한 API 문서 반영 필요

3. **하위 호환성**
   - 기존 클라이언트가 사용 중이라면 마이그레이션 가이드 제공 필요
   - API 버저닝 고려 (현재는 개발 단계이므로 영향 없음)

### 다음 단계

1. **E2E 테스트 추가**
   - 새로운 엔드포인트에 대한 E2E 테스트 작성
   - `GET /messages?chatRoomId={chatRoomId}` 테스트 케이스 추가

2. **API 문서 업데이트**
   - Swagger/OpenAPI 문서에 변경된 엔드포인트 반영
   - 쿼리 파라미터 설명 추가

3. **추가 필터링 기능 구현**
   - `GET /messages?userId={userId}` - 사용자별 메시지 조회
   - `GET /messages?keyword={keyword}` - 메시지 검색
   - 복합 필터링 지원
