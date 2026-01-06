# Monolithic Service

모놀리식 채팅 서버입니다. Phase 1 단계로 모든 기능을 하나의 서비스에 구현합니다.

## 개요

빠른 프로토타이핑 및 핵심 기능 구현을 목표로 하는 모놀리식 서비스입니다.

## 기술 스택

- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.5.9
- **Java**: 21
- **빌드 도구**: Gradle (Kotlin DSL)

## 프로젝트 구조

```
00-monolitic/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── site/rahoon/message/__monolitic/
│   │   │       ├── Application.kt
│   │   │       ├── user/              # User Context
│   │   │       │   ├── controller/   # 컨트롤러, Request/Response DTO
│   │   │       │   ├── application/   # 어플리케이션 서비스, Criteria, Result
│   │   │       │   ├── domain/        # 도메인 서비스, 도메인 객체, Repository interface, Command
│   │   │       │   └── repository/   # Repository 구현체
│   │   │       ├── chat/              # Chat Context
│   │   │       │   ├── controller/
│   │   │       │   ├── application/
│   │   │       │   ├── domain/
│   │   │       │   └── repository/
│   │   │       └── config/            # 설정
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── build.gradle.kts
```

## 실행 방법

### 로컬 실행

```bash
./gradlew bootRun
```

### 빌드

```bash
./gradlew build
```

### 테스트 실행

```bash
./gradlew test
```

## 바운더리 컨텍스트

### User Context
- 사용자 인증/인가
- 사용자 정보 관리

### Chat Context
- 채팅방 관리
- 멤버 관리
- 메시지 전송/수신

자세한 데이터 구조는 [데이터 구조 설계 문서](../../../00-docs/patch/2026-01-06-05-데이터구조설계.md)를 참고하세요.

## 개발 전략

이 서비스는 부하 테스트를 통해 병목 지점을 파악한 후, 필요에 따라 점진적으로 마이크로서비스로 분리됩니다.

자세한 전략은 [백엔드 서비스 설계 전략 문서](../../../00-docs/patch/2026-01-06-04-백엔드서비스설계전략.md)를 참고하세요.

