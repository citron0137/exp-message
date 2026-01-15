# 패치노트 - Soft Delete 도입

## 문제 상황 / 결정 사항

### 1) 물리 삭제로 인한 데이터 복구 불가

- **문제 상황**: 현재 모든 삭제는 물리 삭제(실제 DB 레코드 삭제)로, 삭제된 데이터 복구가 불가능하고 감사 추적이 제한적
- **결정 사항**: **Soft Delete(논리 삭제)를 기본 삭제 전략으로 도입**

### 2) Soft Delete 구현 방식

- **문제 상황**: Soft Delete 구현 방식 선택 (JPA 어노테이션 vs 수동 처리 vs 공통 인터페이스)
- **초기 결정**: **공통 인터페이스 + 확장 함수 + Hibernate Filter (프로그래밍 방식)** 채택
  - `EntityBase` 인터페이스: Entity에 `deletedAt` 필드 정의 (확장 가능한 구조)
  - 확장 함수: JpaRepository에 `softDeleteById()` 헬퍼 제공
  - **Hibernate Filter**: EntityManagerFactory 설정에서 프로그래밍 방식으로 필터 등록
  - **이유**: 
    - Repository 패턴과 잘 맞고, 중복 코드 최소화, 유연성 확보
    - Entity에 Hibernate 어노테이션 추가 불필요 (인프라 의존성 분리)
    - Hibernate Filter로 조회 쿼리 수동 수정 불필요 (자동 적용)
    - 필요 시 필터 비활성화로 삭제된 데이터 조회 가능

- **최종 결정 (변경)**: **공통 베이스 클래스 + `@SQLRestriction` 어노테이션** 채택
  - `JpaEntityBase` 클래스: `@MappedSuperclass`로 공통 필드 정의, `deletedAt` 필드 포함
  - `@SQLRestriction("deleted_at IS NULL")`: Entity에 직접 어노테이션 추가
  - **변경 이유**:
    - **설정 복잡도**: Hibernate Filter는 설정이 복잡하고, 트랜잭션별로 활성화/비활성화 관리가 번거로움
    - **쿼리 복잡성**: Hibernate Filter를 사용하면 생성되는 쿼리가 복잡해지고, 예상치 못한 쿼리 형태로 변형될 가능성이 있음
    - **트랜잭션 관리 복잡성**: Hibernate Filter는 트랜잭션이 적용된 세션에서만 필터를 등록할 수 있어, 필터 활성화/비활성화 관리가 번거로움 (Spring Data JPA 사용 시 대부분의 메서드가 이미 트랜잭션 내에서 실행되므로 실제 성능 영향은 크지 않을 수 있으나, 관리 복잡도가 증가함)
    - **명시성**: `@SQLRestriction`은 더 단순하고 명시적이며, Entity 레벨에서 직접 제어 가능
    - **안정성**: Hibernate 6.4+에서 `@SQLRestriction`이 안정적으로 동작하며, 모든 쿼리에 자동 적용됨
    - **삭제된 데이터 조회**: 삭제된 데이터 조회가 필요한 경우, 별도의 `RawEntity`(필터 없는 엔티티)를 사용하여 조회 가능
  - **구현 방식**:
    - Entity는 `JpaEntityBase`를 상속하고 `@SQLRestriction("deleted_at IS NULL")` 추가
    - 삭제된 데이터 조회가 필요한 경우: `RawEntity` 클래스를 별도로 정의하여 사용 (같은 테이블, 필터 없음)

### 3) 삭제된 데이터 조회 정책

- **문제 상황**: 삭제된 데이터 조회가 필요한 경우(복구, 감사 등)와 일반 조회를 구분해야 함
- **결정 사항**:
  - 기본 조회: 삭제된 데이터 제외 (`deletedAt IS NULL`)
  - 관리자/특수 조회: 삭제된 데이터 포함 옵션 제공 (필요 시)

### 4) 삭제된 데이터 정리 전략

- **문제 상황**: Soft Delete된 데이터가 계속 누적되면 저장 공간 및 성능 이슈 발생 가능
- **결정 사항**:
  - 주기적 물리 삭제 배치 작업 작성 (예: 90일 이상 삭제된 데이터)
  - 배치 작업은 별도 모듈(`01-db-migrations` 또는 새로운 배치 모듈)에서 관리

### 5) 적용 우선순위

- **문제 상황**: 모든 도메인에 한 번에 적용하면 영향 범위가 큼
- **결정 사항**: 아래 순서로 단계 적용
  - Message → ChatRoomMember → ChatRoom → User

## 테스트 계획

- [ ] 단위 테스트: Soft Delete 후 조회 시 제외되는지 검증
- [ ] 단위 테스트: 삭제된 데이터 복구 기능 검증
- [ ] 통합 테스트: Soft Delete 후 관련 데이터 조회 시 영향 검증
- [ ] 배치 테스트: 주기적 물리 삭제 배치 작업 검증

## 최종 정리(현재 합의된 스펙)

- **삭제 방식**: Soft Delete(논리 삭제) 기본, 물리 삭제는 배치 작업에서만 수행
- **구현 방식**: 공통 베이스 클래스 + `@SQLRestriction` 어노테이션
  - `JpaEntityBase` 클래스: `@MappedSuperclass`로 공통 필드 정의, `deletedAt: LocalDateTime?` 필드 포함
  - `@SQLRestriction("deleted_at IS NULL")`: Entity에 직접 어노테이션 추가하여 Soft Delete 필터 적용
  - RepositoryImpl에서 `delete()` → `softDeleteById()` 호출로 변경 (Native Query 사용)
  - **조회 쿼리 수정 불필요**: `@SQLRestriction`이 모든 쿼리에 자동으로 조건 추가
  - **삭제된 데이터 조회**: 별도의 `RawEntity` 클래스를 사용하여 조회 (같은 테이블, 필터 없음)
- **조회 정책**
  - 기본 조회: 삭제된 데이터 제외 (`deletedAt IS NULL`)
  - 관리자 조회: 삭제된 데이터 포함 옵션 (필요 시 별도 메서드 제공)
- **정리 전략**
  - 주기적 배치 작업으로 오래된 삭제 데이터 물리 삭제 (예: 90일 이상)
- **적용 우선순위**: Message → ChatRoomMember → ChatRoom → User

## 구현 상세

### 1. 공통 인터페이스 및 확장 함수

```kotlin
// common/infrastructure/EntityBase.kt
interface EntityBase {
    var deletedAt: LocalDateTime?
}

// common/infrastructure/JpaRepositoryExtensions.kt
fun <T : EntityBase, ID> JpaRepository<T, ID>.softDeleteById(id: ID) {
    findById(id)?.let { entity ->
        entity.deletedAt = LocalDateTime.now()
        save(entity)
    }
}
```

### 2. @SQLRestriction 어노테이션 사용

- **Entity 레벨 적용**: 각 Entity에 `@SQLRestriction("deleted_at IS NULL")` 어노테이션 추가
- **장점**: 
  - 설정이 단순하고 명시적
  - Entity 레벨에서 직접 제어 가능
  - Hibernate 6.4+에서 안정적으로 동작
- **삭제된 데이터 조회**: 
  - 별도의 `RawEntity` 클래스를 정의하여 사용 (같은 테이블, `@SQLRestriction` 없음)
  - 예: `TestEntity` (필터 있음) vs `RawTestEntity` (필터 없음)

### 3. 주요 변경 사항

- **Entity**: 
  - `JpaEntityBase` 클래스 상속, `deletedAt` 필드 자동 포함
  - `@SQLRestriction("deleted_at IS NULL")` 어노테이션 추가
- **JpaRepository**: 
  - 쿼리 메서드 수정 불필요 (`@SQLRestriction`이 자동 적용)
  - `softDeleteById()`: Native Query로 `deleted_at` 업데이트
- **RepositoryImpl**: `delete()` → `softDeleteById()` 호출로 변경
- **커스텀 쿼리**: JPQL 수정 불필요 (`@SQLRestriction`이 자동 적용)
- **삭제된 데이터 조회**: `RawEntity` + `RawJpaRepository`를 별도로 정의하여 사용

## 피드백 (구현 후 작성)

### 잘한 점

- (구현 후 작성)

### 아쉬운 점

- (구현 후 작성)

## 다음으로 해볼 것

- **DB 마이그레이션 추가**: `deletedAt` 컬럼 추가 마이그레이션 작성
- **Repository 수정**: 각 도메인 Repository에 Soft Delete 로직 적용
- **복구 기능 추가**: 삭제된 데이터 복구 API (필요 시)
- **배치 작업 작성**: 주기적 물리 삭제 배치 작업 구현
- **인덱스 추가**: `deletedAt` 컬럼 인덱스 추가 (조회 성능 최적화)
- **관리자 조회 옵션**: 삭제된 데이터 포함 조회 기능 (필요 시 `RawEntity` 사용)
