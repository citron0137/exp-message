# 패치노트 - Soft Delete 도입

## 문제 상황 / 결정 사항

### 1) 물리 삭제로 인한 데이터 복구 불가

- **문제 상황**: 현재 모든 삭제는 물리 삭제(실제 DB 레코드 삭제)로, 삭제된 데이터 복구가 불가능하고 감사 추적이 제한적
- **결정 사항**: **Soft Delete(논리 삭제)를 기본 삭제 전략으로 도입**

### 2) Soft Delete 구현 방식

- **문제 상황**: Soft Delete 구현 방식 선택 (JPA 어노테이션 vs 수동 처리 vs 공통 인터페이스)
- **결정 사항**: **공통 인터페이스 + 확장 함수 + Hibernate Filter (프로그래밍 방식)** 채택
  - `SoftDeletableEntity` 인터페이스: Entity에 `deletedAt` 필드 정의
  - 확장 함수: JpaRepository에 `softDeleteById()` 헬퍼 제공
  - **Hibernate Filter**: EntityManagerFactory 설정에서 프로그래밍 방식으로 필터 등록
  - **이유**: 
    - Repository 패턴과 잘 맞고, 중복 코드 최소화, 유연성 확보
    - Entity에 Hibernate 어노테이션 추가 불필요 (인프라 의존성 분리)
    - Hibernate Filter로 조회 쿼리 수동 수정 불필요 (자동 적용)
    - 필요 시 필터 비활성화로 삭제된 데이터 조회 가능

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
- **구현 방식**: 공통 인터페이스 + 확장 함수 + Hibernate Filter
  - `SoftDeletableEntity` 인터페이스: Entity에 `deletedAt: LocalDateTime?` 필드 정의
  - 확장 함수: JpaRepository에 `softDeleteById()` 헬퍼 제공
  - Hibernate Filter: EntityManagerFactory 설정에서 프로그래밍 방식으로 필터 등록
  - RepositoryImpl에서 `delete()` → `softDeleteById()` 호출로 변경
  - **조회 쿼리 수정 불필요**: Hibernate Filter가 자동으로 조건 추가
- **조회 정책**
  - 기본 조회: 삭제된 데이터 제외 (`deletedAt IS NULL`)
  - 관리자 조회: 삭제된 데이터 포함 옵션 (필요 시 별도 메서드 제공)
- **정리 전략**
  - 주기적 배치 작업으로 오래된 삭제 데이터 물리 삭제 (예: 90일 이상)
- **적용 우선순위**: Message → ChatRoomMember → ChatRoom → User

## 구현 상세

### 1. 공통 인터페이스 및 확장 함수

```kotlin
// common/infrastructure/SoftDeletableEntity.kt
interface SoftDeletableEntity {
    var deletedAt: LocalDateTime?
}

// common/infrastructure/JpaRepositoryExtensions.kt
fun <T : SoftDeletableEntity, ID> JpaRepository<T, ID>.softDeleteById(id: ID) {
    findById(id)?.let { entity ->
        entity.deletedAt = LocalDateTime.now()
        save(entity)
    }
}
```

### 2. Hibernate Filter 설정

- **JpaConfig**: EntityManagerFactory 설정에서 Filter 프로그래밍 방식으로 등록
- **장점**: Entity에 Hibernate 어노테이션 추가 불필요 (인프라 의존성 분리)
- **Filter 활성화/비활성화**: 필요 시 필터 비활성화하여 삭제된 데이터 조회 가능

### 3. 주요 변경 사항

- **Entity**: `SoftDeletableEntity` 인터페이스 구현, `deletedAt` 필드 추가 (Hibernate 어노테이션 불필요)
- **JpaConfig**: EntityManagerFactory 설정에서 Filter 프로그래밍 방식으로 등록
- **JpaRepository**: 쿼리 메서드 수정 불필요 (Filter가 자동 적용)
- **RepositoryImpl**: `delete()` → `softDeleteById()` 호출로 변경
- **커스텀 쿼리**: JPQL 수정 불필요 (Filter가 자동 적용)

## 피드백 (구현 후 작성)

### 잘한 점

- (구현 후 작성)

### 아쉬운 점

- (구현 후 작성)

## 다음으로 해볼 것

- **DB 마이그레이션 추가**: `deletedAt` 컬럼 추가 마이그레이션 작성
- **Hibernate Filter 설정**: EntityManagerFactory 설정에서 Filter 프로그래밍 방식으로 등록
- **Repository 수정**: 각 도메인 Repository에 Soft Delete 로직 적용
- **복구 기능 추가**: 삭제된 데이터 복구 API (필요 시)
- **배치 작업 작성**: 주기적 물리 삭제 배치 작업 구현
- **인덱스 추가**: `deletedAt` 컬럼 인덱스 추가 (조회 성능 최적화)
- **관리자 조회 옵션**: 삭제된 데이터 포함 조회 기능 (필요 시)
