# 패치노트 - 2026-01-12

## ChatRoom 권한 검증 추가

### 목표

채팅방 수정/삭제 시 생성자만 가능하도록 권한 검증을 추가하고, Application Service 레이어에서 권한 검증을 수행하도록 개선합니다.

### 구현 내용

#### 1. Application 레이어 - 권한 검증 추가

**수정된 파일:**
- `ChatRoomApplicationService.kt`: 권한 검증 로직 추가
  - `update()`: 채팅방 수정 시 생성자 권한 검증
  - `delete()`: 채팅방 삭제 시 생성자 권한 검증
  - 권한이 없을 경우 `UNAUTHORIZED_ACCESS` 에러 발생 (HTTP 403)

**변경 사항:**
- `ChatRoomCriteria.kt`: 모든 필요한 정보를 Criteria에 포함
  - `Create`: `createdByUserId` 필드 추가
  - `Update`: `chatRoomId`, `userId` 필드 추가
  - `Delete`: 새로 추가 (`chatRoomId`, `userId` 포함)
  - `toCommand()`: 파라미터 없이 호출 가능하도록 변경

#### 2. Controller 레이어 - Criteria 생성 방식 변경

**수정된 파일:**
- `ChatRoomRequest.kt`: Criteria 변환 시 필요한 정보 전달
  - `Create.toCriteria(createdByUserId: String)`: `createdByUserId` 전달
  - `Update.toCriteria(chatRoomId: String, userId: String)`: `chatRoomId`, `userId` 전달
- `ChatRoomController.kt`: Criteria 생성 방식 변경
  - `create()`: `request.toCriteria(authInfo.userId)` 호출
  - `update()`: `request.toCriteria(id, authInfo.userId)` 호출
  - `delete()`: `ChatRoomCriteria.Delete` 직접 생성

#### 3. Domain 레이어 - 순수성 유지

**수정된 파일:**
- `ChatRoomDomainService.kt`: 권한 검증 로직 제거
  - `update()`: `userId` 파라미터 제거, 비즈니스 로직에만 집중
  - `delete()`: `userId` 파라미터 제거, 비즈니스 로직에만 집중

#### 4. 테스트 - 권한 검증 테스트 케이스 추가

**수정된 파일:**
- `ChatRoomControllerE2ETest.kt`: 권한 검증 테스트 추가
  - `채팅방 수정 실패 - 권한 없음`: 다른 사용자가 수정 시도 시 403 반환 확인
  - `채팅방 삭제 실패 - 권한 없음`: 다른 사용자가 삭제 시도 시 403 반환 및 채팅방 유지 확인

### 설계 결정

1. **권한 검증을 Application Service 레이어에서 수행**
   - UseCase 단위로 권한 검증을 수행하여 일관성 유지
   - Domain Service는 순수한 비즈니스 로직에만 집중
   - 추후 관리자 기능 추가 시 Application Service만 수정하면 됨

2. **Criteria에 모든 필요한 정보 포함**
   - UseCase의 모든 입력을 Criteria에 포함하여 명확성 향상
   - Application Service 메서드 시그니처 단순화
   - `UserCriteria`와 동일한 패턴 적용으로 일관성 유지

3. **레이어 책임 분리**
   - Domain Service: 비즈니스 로직만 담당
   - Application Service: UseCase 조율 및 권한 검증 담당
   - Controller: HTTP 요청/응답 처리 및 Criteria 생성

### 변경 전후 비교

#### 변경 전
```kotlin
// Application Service
fun update(criteria: ChatRoomCriteria.Update, chatRoomId: String, userId: String): ChatRoomInfo.Detail {
    val command = criteria.toCommand(chatRoomId)
    return chatRoomDomainService.update(command)
}

// Domain Service
fun update(command: ChatRoomCommand.Update, userId: String): ChatRoomInfo.Detail {
    // 권한 검증 로직
    // ...
}
```

#### 변경 후
```kotlin
// Application Service
fun update(criteria: ChatRoomCriteria.Update): ChatRoomInfo.Detail {
    // 권한 검증: 생성자만 수정 가능
    val chatRoomInfo = chatRoomDomainService.getById(criteria.chatRoomId)
    if (chatRoomInfo.createdByUserId != criteria.userId) {
        throw DomainException(...)
    }
    
    val command = criteria.toCommand()
    return chatRoomDomainService.update(command)
}

// Domain Service
fun update(command: ChatRoomCommand.Update): ChatRoomInfo.Detail {
    // 비즈니스 로직만 처리
    // ...
}
```

### 해결된 이슈

1. **채팅방 수정/삭제 권한 검증** ✅
   - 생성자만 수정/삭제 가능하도록 권한 검증 추가
   - 권한이 없는 사용자는 403 Forbidden 응답

### 테스트 결과

모든 E2E 테스트 통과:
- ✅ 채팅방 생성 성공
- ✅ 채팅방 생성 실패 - 인증 없음
- ✅ 채팅방 생성 실패 - 이름 누락
- ✅ 채팅방 조회 성공
- ✅ 내가 생성한 채팅방 목록 조회 성공
- ✅ 채팅방 수정 성공
- ✅ 채팅방 수정 실패 - 권한 없음 (신규)
- ✅ 채팅방 삭제 성공
- ✅ 채팅방 삭제 실패 - 권한 없음 (신규)

### AI 피드백

#### 잘한 점

1. **명확한 레이어 책임 분리**
   - Domain Service를 순수한 비즈니스 로직에만 집중하도록 설계하여 도메인 모델의 순수성 유지
   - Application Service에서 권한 검증을 수행하여 UseCase 단위로 일관된 보안 정책 적용
   - 각 레이어의 책임이 명확하여 유지보수성 향상

2. **일관된 패턴 적용**
   - `UserCriteria`와 동일한 패턴으로 `ChatRoomCriteria`를 설계하여 코드베이스 전반의 일관성 유지
   - Criteria에 모든 필요한 정보를 포함시켜 메서드 시그니처를 단순화하고 가독성 향상

3. **충분한 테스트 커버리지**
   - 권한 검증 실패 케이스를 명시적으로 테스트하여 보안 요구사항 검증 완료
   - E2E 테스트를 통해 전체 플로우 검증

4. **설계 결정 문서화**
   - 설계 결정 사항을 명확히 문서화하여 향후 유지보수 시 의도 파악 용이

#### 아쉬운 점

1. **권한 검증 로직의 확장성**
   - 현재 권한 검증 로직이 Application Service에 하드코딩되어 있어, 추후 관리자 권한이나 다양한 권한 체계 추가 시 코드 중복 가능성
   - 권한 검증 전략을 추상화하거나 별도의 권한 검증 서비스로 분리 고려 필요

2. **에러 처리 일관성**
   - `UNAUTHORIZED_ACCESS` 에러에 대한 상세한 설명이나 에러 코드 체계에 대한 언급 부족
   - 에러 메시지의 일관성 및 사용자 친화성 개선 여지

3. **권한 검증 로직의 재사용성**
   - `update()`와 `delete()` 메서드에서 유사한 권한 검증 로직이 반복됨
   - 공통 권한 검증 메서드 추출을 통해 DRY 원칙 적용 고려

4. **성능 최적화**
   - 권한 검증을 위해 `getById()`를 호출하는데, 이미 조회된 엔티티가 있다면 중복 조회 방지 고려
   - 권한 검증과 비즈니스 로직에서 동일한 엔티티를 조회하는 경우 최적화 가능

### 다음 단계

1. **ChatRoomMember 도메인 구현**
   - 채팅방 참가/나가기 기능
   - 채팅방 멤버 목록 조회
   - 관리자 권한 검증 로직 추가 (생성자 외에도 관리자 수정/삭제 가능)

2. **Message 도메인 구현**
   - 메시지 전송
   - 메시지 조회 (페이징)
   - 채팅방별 메시지 목록

3. **DB 마이그레이션 도입**
   - Flyway 또는 Liquibase 설정
   - `chat_rooms` 테이블 마이그레이션 작성
