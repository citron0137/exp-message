# 패치노트 - 2026-01-12

## ChatRoomMember 도메인 구현

### 목표

채팅방 멤버 도메인을 구현하여 채팅방 참가/나가기 기능과 멤버 목록 조회 기능을 제공합니다. 또한 채팅방 생성 시 생성자가 자동으로 멤버로 추가되도록 구현합니다.

### 구현 내용

#### 1. Domain 레이어

**생성된 파일:**
- `ChatRoomMember.kt`: 채팅방 멤버 도메인 엔티티
  - `create()`: 새로운 채팅방 멤버 생성
- `ChatRoomMemberCommand.kt`: 도메인 명령 객체
  - `Join`: 채팅방 참가 명령
  - `Leave`: 채팅방 나가기 명령
- `ChatRoomMemberInfo.kt`: 도메인 정보 객체
  - `Detail`: 채팅방 멤버 상세 정보
- `ChatRoomMemberError.kt`: 채팅방 멤버 관련 에러 정의
  - `CHAT_ROOM_MEMBER_NOT_FOUND`: 채팅방 멤버를 찾을 수 없음
  - `ALREADY_JOINED`: 이미 참가한 멤버
  - `NOT_A_MEMBER`: 멤버가 아님
- `ChatRoomMemberRepository.kt`: 채팅방 멤버 저장소 인터페이스
  - `findByChatRoomIdAndUserId()`: 채팅방 ID와 사용자 ID로 조회
  - `findByChatRoomId()`: 채팅방 ID로 멤버 목록 조회
  - `findByUserId()`: 사용자 ID로 참가한 채팅방 목록 조회
- `ChatRoomMemberDomainService.kt`: 채팅방 멤버 도메인 서비스
  - `join()`: 채팅방 참가 (중복 참가 방지, 채팅방 존재 확인)
  - `leave()`: 채팅방 나가기 (멤버 여부 확인)
  - `getByChatRoomId()`: 특정 채팅방의 멤버 목록 조회
  - `getByUserId()`: 특정 사용자가 참가한 채팅방 목록 조회
  - `isMember()`: 멤버 여부 확인

#### 2. Infrastructure 레이어

**생성된 파일:**
- `ChatRoomMemberEntity.kt`: JPA 엔티티
  - `chat_room_members` 테이블 매핑
  - `chat_room_id`, `user_id` 인덱스 설정
  - `chat_room_id, user_id` 복합 유니크 인덱스 설정
- `ChatRoomMemberJpaRepository.kt`: Spring Data JPA Repository
  - `findByChatRoomIdAndUserId()`: 채팅방 ID와 사용자 ID로 조회
  - `findByChatRoomId()`: 채팅방 ID로 멤버 목록 조회
  - `findByUserId()`: 사용자 ID로 참가한 채팅방 목록 조회
  - `deleteByChatRoomIdAndUserId()`: 채팅방 ID와 사용자 ID로 삭제
- `ChatRoomMemberRepositoryImpl.kt`: Repository 구현체
  - 도메인 객체와 JPA 엔티티 간 변환 로직

#### 3. Application 레이어

**생성된 파일:**
- `ChatRoomMemberApplicationService.kt`: 애플리케이션 서비스
  - `join()`: 채팅방 참가
  - `leave()`: 채팅방 나가기
  - `getByChatRoomId()`: 특정 채팅방의 멤버 목록 조회
  - `getByUserId()`: 특정 사용자가 참가한 채팅방 목록 조회
- `ChatRoomMemberCriteria.kt`: 애플리케이션 레이어 입력 DTO
  - `Join`: 채팅방 참가 요청
  - `Leave`: 채팅방 나가기 요청

**수정된 파일:**
- `ChatRoomApplicationService.kt`: 채팅방 생성 시 생성자 자동 추가
  - `create()`: 채팅방 생성 후 생성자를 자동으로 멤버로 추가

#### 4. Controller 레이어

**생성된 파일:**
- `ChatRoomMemberController.kt`: REST API 컨트롤러
  - `POST /chat-rooms/{chatRoomId}/members`: 채팅방 참가
  - `DELETE /chat-rooms/{chatRoomId}/members`: 채팅방 나가기
  - `GET /chat-rooms/{chatRoomId}/members`: 채팅방 멤버 목록 조회
- `ChatRoomMemberRequest.kt`: 요청 DTO
  - `toJoinCriteria()`: 참가 Criteria 변환
  - `toLeaveCriteria()`: 나가기 Criteria 변환
- `ChatRoomMemberResponse.kt`: 응답 DTO
  - `Member`: 채팅방 참가/나가기 응답
  - `ListItem`: 채팅방 멤버 목록 항목 응답

#### 5. 테스트

**생성된 파일:**
- `ChatRoomMemberControllerE2ETest.kt`: E2E 테스트
  - 채팅방 참가 성공/실패 테스트
  - 채팅방 나가기 성공/실패 테스트
  - 채팅방 멤버 목록 조회 테스트
  - 채팅방 생성 시 생성자 자동 추가 테스트
  - 중복 참가 방지 테스트
  - 존재하지 않는 채팅방 참가 시도 테스트
  - 멤버가 아닌 사용자의 나가기 시도 테스트

### API 엔드포인트

- `POST /chat-rooms/{chatRoomId}/members` - 채팅방 참가 (인증 필요)
- `DELETE /chat-rooms/{chatRoomId}/members` - 채팅방 나가기 (인증 필요)
- `GET /chat-rooms/{chatRoomId}/members` - 채팅방 멤버 목록 조회 (인증 필요)

### 설계 결정

1. **ChatRoom 도메인과 동일한 패턴 적용**
   - Domain → Application → Controller 레이어 구조
   - Command/Criteria/Request/Response DTO 분리
   - 일관된 아키텍처 패턴 유지

2. **채팅방 존재 확인**
   - `ChatRoomMemberDomainService`에서 `ChatRoomRepository`를 의존하여 채팅방 존재 여부 확인
   - 참가/나가기 시 채팅방이 존재하지 않으면 에러 발생

3. **중복 참가 방지**
   - `chat_room_id, user_id` 복합 유니크 인덱스로 데이터베이스 레벨 중복 방지
   - 도메인 서비스에서 사전 검증으로 사용자 친화적 에러 메시지 제공

4. **채팅방 생성 시 생성자 자동 추가**
   - `ChatRoomApplicationService.create()`에서 채팅방 생성 후 생성자를 자동으로 멤버로 추가
   - 트랜잭션 내에서 처리하여 일관성 보장

5. **데이터베이스 설계**
   - `chat_room_members` 테이블 생성
   - `chat_room_id`, `user_id` 인덱스로 조회 성능 최적화
   - `chat_room_id, user_id` 복합 유니크 인덱스로 중복 방지

### 해결된 이슈

1. **채팅방 참가/나가기 기능** ✅
   - 사용자가 채팅방에 참가하고 나갈 수 있는 기능 구현
   - 중복 참가 방지 및 멤버 여부 확인 로직 구현

2. **채팅방 멤버 목록 조회** ✅
   - 특정 채팅방의 멤버 목록 조회 기능 구현
   - 특정 사용자가 참가한 채팅방 목록 조회 기능 구현

3. **채팅방 생성 시 생성자 자동 추가** ✅
   - 채팅방 생성 시 생성자가 자동으로 멤버로 추가되도록 구현
   - `ChatRoomApplicationService.create()`의 TODO 해결

### 테스트 결과

모든 E2E 테스트 통과:
- ✅ 채팅방 참가 성공
- ✅ 채팅방 참가 실패 - 이미 참가한 멤버
- ✅ 채팅방 참가 실패 - 존재하지 않는 채팅방
- ✅ 채팅방 나가기 성공
- ✅ 채팅방 나가기 실패 - 멤버가 아님
- ✅ 채팅방 멤버 목록 조회 성공
- ✅ 채팅방 생성 시 생성자 자동 추가 확인

### AI 피드백

#### 잘한 점

1. **일관된 아키텍처 패턴**
   - ChatRoom 도메인과 동일한 레이어 구조와 패턴을 적용하여 코드베이스 전반의 일관성 유지
   - Command/Criteria/Request/Response DTO 분리로 레이어 간 의존성 관리 우수

2. **도메인 로직 캡슐화**
   - `ChatRoomMember.create()`로 비즈니스 로직이 도메인 엔티티에 잘 캡슐화됨
   - 도메인 서비스에서 채팅방 존재 확인, 중복 참가 방지 등 비즈니스 규칙 적용

3. **데이터베이스 설계**
   - 복합 유니크 인덱스로 데이터베이스 레벨 중복 방지
   - 적절한 인덱스 설정으로 조회 성능 최적화

4. **트랜잭션 관리**
   - 채팅방 생성과 멤버 추가를 동일 트랜잭션에서 처리하여 일관성 보장
   - `@Transactional` 어노테이션 적절히 사용

5. **포괄적인 테스트**
   - 성공 케이스뿐만 아니라 실패 케이스(중복 참가, 멤버 아님, 존재하지 않는 채팅방)도 잘 테스트됨
   - 채팅방 생성 시 생성자 자동 추가 기능도 테스트로 검증

6. **에러 처리**
   - `DomainException`을 통한 일관된 에러 처리
   - 명확한 에러 코드와 메시지로 사용자 친화적 에러 제공

#### 아쉬운 점

1. **권한 검증 부재**
   - 채팅방 멤버 목록 조회 시 참가자 여부 확인 없이 누구나 조회 가능
   - 채팅방 참가 시 공개/비공개 채팅방 구분 없음

2. **역할(Role) 관리 미구현**
   - 현재는 모든 멤버가 동일한 권한을 가짐
   - 관리자 역할 추가 시 권한 검증 로직 확장 필요

3. **채팅방 삭제 시 멤버 처리**
   - 채팅방 삭제 시 멤버 데이터 처리 방식이 명시되지 않음
   - 외래키 제약조건 또는 CASCADE 설정 고려 필요

4. **페이징 미지원**
   - 멤버 목록 조회 시 페이징이 없어 대량의 멤버가 있는 채팅방에서 성능 문제 가능
   - 사용자가 참가한 채팅방 목록 조회도 페이징 필요

5. **멤버 수 제한 미구현**
   - 채팅방 참가 시 최대 멤버 수 제한이 없음
   - 비즈니스 요구사항에 따라 최대 멤버 수 제한 고려 필요

6. **채팅방 나가기 후 재참가**
   - 채팅방을 나간 후 다시 참가할 수 있는지에 대한 명확한 정책 부재
   - 현재는 나간 후에도 다시 참가 가능하지만, 비즈니스 요구사항에 따라 제한 필요할 수 있음

7. **멤버 정보 확장성**
   - 현재는 `id`, `chatRoomId`, `userId`, `joinedAt`만 저장
   - 추후 역할(role), 초대자 정보, 마지막 접속 시간 등 추가 정보 필요 시 확장 필요

### 다음 단계

1. **Message 도메인 구현**
   - 메시지 전송
   - 메시지 조회 (페이징)
   - 채팅방별 메시지 목록
   - 멤버만 메시지 전송 가능하도록 권한 검증

2. **권한 검증 강화**
   - 채팅방 멤버 목록 조회 시 참가자만 조회 가능하도록 권한 검증
   - 공개/비공개 채팅방 구분 및 참가 정책 구현

3. **역할(Role) 관리**
   - 관리자 역할 추가
   - 관리자 권한 검증 로직 구현 (생성자 외에도 관리자 수정/삭제 가능)

4. **DB 마이그레이션 도입**
   - Flyway 또는 Liquibase 설정
   - `chat_room_members` 테이블 마이그레이션 작성

5. **페이징 지원**
   - 멤버 목록 조회 페이징
   - 사용자가 참가한 채팅방 목록 조회 페이징
