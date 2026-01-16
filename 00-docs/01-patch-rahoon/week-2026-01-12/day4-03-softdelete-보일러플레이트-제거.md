# 패치노트 - Soft Delete 보일러플레이트 제거 방안

## 문제 상황

### 현재 상황
- `TestRepositoryImpl`에 소프트 삭제가 구현되어 있음
- 다른 리포지터리들(`UserRepositoryImpl`, `ChatRoomRepositoryImpl`, `MessageRepositoryImpl`, `ChatRoomMemberRepositoryImpl` 등)에도 동일한 패턴을 적용해야 함
- 각 리포지터리마다 반복되는 보일러플레이트 코드가 발생할 예정

---

## 문제 상황별 해결 방안

### 문제 1: `findById()` 메서드의 보일러플레이트

#### Before
각 리포지터리마다 `findById()` 메서드에서 동일한 소프트 삭제 필터링 로직이 반복됨.
`SoftDeleteContext.isDisabled() || it.deletedAt == null` 조건과 도메인 변환 로직이 각 리포지터리마다 중복 구현됨.

#### Goal
보일러플레이트 코드 완전 제거 및 공통 로직 중앙화.
각 리포지터리에서 `findById()` 오버라이드 불필요하게 하고, 공통 로직 변경 시 한 곳만 수정하도록 개선.

#### KeyDecision
`JpaRepositoryAdapterBase` 베이스 클래스에 `findById()` 메서드를 직접 구현하여 소프트 삭제 필터링과 도메인 변환을 자동화.
헬퍼 클래스 방식(상속 제약 없지만 주입 필요)과 확장 함수 방식(Spring Bean 주입 불가)은 제외.

#### Impact
코드 중복 완전 제거 및 유지보수성 향상. 각 리포지터리에서 `findById()` 오버라이드 불필요.
단, 베이스 클래스 상속이 필요하여 단일 상속 제약이 있음.

---

### 문제 2: `delete()` 메서드의 보일러플레이트

#### Before
각 리포지터리마다 `delete()` 메서드에서 동일한 소프트 삭제 로직이 반복됨.
`jpaRepository.softDeleteById(id, LocalDateTime.now())` 호출이 각 리포지터리마다 중복 구현됨.

#### Goal
보일러플레이트 코드 제거 및 공통 삭제 로직 중앙화.
`LocalDateTime.now()` 호출과 소프트 삭제 로직을 한 곳에서 관리하도록 개선.

#### KeyDecision
(해결 방안 미정)

#### Impact
(해결 방안 미정)

