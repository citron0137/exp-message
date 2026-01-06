# 개발 가이드

모놀리식 서비스 개발 가이드입니다.

## 개발 환경 설정

### 필요 사항

- JDK 21
- Gradle 8.x
- MySQL 8.x
- Redis 7.x

### 로컬 환경 설정

1. MySQL 및 Redis 실행
2. `src/main/resources/application.properties` 설정
3. 애플리케이션 실행

## 코딩 컨벤션

### 패키지 구조

```
site.rahoon.message.__monolitic/
├── user/
│   ├── controller/          # 컨트롤러, Request/Response DTO
│   ├── application/         # 어플리케이션 서비스, Criteria, Result
│   ├── domain/              # 도메인 서비스, 도메인 객체, Repository interface, Command
│   └── repository/         # Repository 구현체
├── chat/
│   ├── controller/          # 컨트롤러, Request/Response DTO
│   ├── application/         # 어플리케이션 서비스, Criteria, Result
│   ├── domain/              # 도메인 서비스, 도메인 객체, Repository interface, Command
│   └── repository/         # Repository 구현체
└── config/
```

### 레이어별 역할

#### Controller
- REST API 엔드포인트
- Request/Response DTO
- HTTP 요청/응답 처리

#### Application
- 어플리케이션 서비스 (유스케이스 오케스트레이션)
- Criteria (명령 인자)
- Result (명령 결과)

#### Domain
- 도메인 서비스 (비즈니스 로직)
- 도메인 객체 (Entity, Value Object)
- Repository interface
- Command (서비스 명령 객체)

#### Repository
- Repository 구현체
- 데이터 접근 로직

### 네이밍 규칙

- **Controller**: `{Entity}Controller`
- **Application Service**: `{Entity}ApplicationService`
- **Domain Service**: `{Entity}DomainService`
- **Domain**: `{Entity}` (도메인 객체)
- **Repository Interface**: `{Entity}Repository` (domain 패키지)
- **Repository Implementation**: `{Entity}RepositoryImpl` (repository 패키지)
- **DTO**: `{Entity}Request.{Action}`, `{Entity}Response.{Action}` (예: UserRequest.Create, UserResponse.Detail)
- **Criteria**: `{Entity}Criteria.{Action}` (예: UserCriteria.FindAll, ChatRoomCriteria.FindById)
- **Result**: `{Entity}Result.{Action}` (예: UserResult.Detail, ChatRoomResult.List)
- **Command**: `{Entity}Command.{Action}` (예: UserCommand.Create, ChatRoomCommand.Join)

### 예외 처리

- **DomainException**: 도메인 레벨에서만 사용 (비즈니스 규칙 위반)
  - 도메인 서비스에서 발생
  - 각 컨텍스트의 Error enum과 함께 사용
- **ApplicationException**: 어플리케이션 레벨에서 사용 (유효성 검증, 권한 등)
  - 어플리케이션 서비스에서 발생
- 전역 예외 핸들러에서 두 예외 모두 처리

## 테스트

### 단위 테스트

- Service 레이어 테스트
- Repository 레이어 테스트

### 통합 테스트

- API 엔드포인트 테스트
- WebSocket 연결 테스트

## 데이터베이스 마이그레이션

Flyway 또는 Liquibase를 사용하여 스키마 버전 관리

## 트랜잭션 격리 수준

- **READ COMMITTED 사용**
  - MySQL 기본값(REPEATABLE READ) 대신 READ COMMITTED 사용
  - 이유: 중복 체크와 생성 사이에 다른 트랜잭션의 커밋된 변경사항을 확인 가능
  - 예: 이메일 중복 체크 시 최신 커밋된 데이터를 확인하여 중복 생성 방지
  - `Tx.writable`, `Tx.readOnly`, `Tx.writableNew` 모두 READ COMMITTED 적용

## 로깅

- SLF4J + Logback 사용
- 로그 레벨: 개발 환경(DEBUG), 프로덕션(INFO)

