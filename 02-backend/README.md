# Backend Services

백엔드 서비스 디렉토리입니다. 채팅 서버의 모든 백엔드 서비스를 관리합니다.

## 개발 전략

이 프로젝트는 **단일 서비스에서 시작하여 부하 테스트를 통해 병목 지점을 파악하고, 필요에 따라 점진적으로 마이크로서비스로 분리**하는 전략을 따릅니다.

### 현재 단계: Phase 1 - 모놀리식 시작

**목표**: 빠른 프로토타이핑 및 핵심 기능 구현

**구조**:
- 모든 기능을 하나의 서비스에 구현
- 단일 데이터베이스 (MySQL)
- 기본적인 Redis 캐싱
- Kafka는 제외 (초기 단계에서는 불필요)

**구현 범위**:
- 사용자 인증/인가
- 채팅방 관리
- 메시지 전송/수신
- 기본적인 검색 기능 (DB 기반)
- WebSocket 실시간 통신

자세한 전략은 [백엔드 서비스 설계 전략 문서](../../00-docs/patch/2026-01-06-04-백엔드서비스설계전략.md)를 참고하세요.

## 디렉토리 구조

### 현재 구조 (Phase 1: 모놀리식)

```text
02-backend/
├── 00-monolithic/           # 모놀리식 서비스 (현재 단계)
│   ├── src/                 # 소스 코드
│   │   ├── main/            # 메인 애플리케이션
│   │   │   ├── handlers/    # HTTP 핸들러
│   │   │   ├── services/    # 비즈니스 로직
│   │   │   ├── repositories/# 데이터 접근 계층
│   │   │   ├── models/      # 데이터 모델
│   │   │   └── config/       # 설정
│   │   └── test/            # 테스트 코드
│   ├── Dockerfile           # Docker 이미지 빌드
│   └── README.md            # 서비스별 README
└── shared/                  # 공유 라이브러리/유틸리티
    ├── common/              # 공통 코드
    ├── config/              # 공통 설정
    └── proto/               # gRPC 프로토콜 정의 (선택)
```

### 향후 구조 (Phase 3: 마이크로서비스 분리 후)

부하 테스트를 통해 병목 지점을 파악한 후, 필요에 따라 점진적으로 서비스를 분리합니다.

**예상 서비스 목록** (병목 지점에 따라 조정):
- `01-api-gateway/` - API Gateway 서비스
- `02-user-service/` - 사용자 서비스
- `03-chat-room-service/` - 채팅방 서비스
- `04-message-service/` - 메시지 서비스
- `05-search-service/` - 검색 서비스
- `06-notification-service/` - 알림 서비스
- `07-websocket-service/` - WebSocket 서비스

> **참고**: 모든 서비스를 미리 만들지 않고, 실제 필요할 때만 분리합니다.

## 서비스별 역할 (향후 분리 목표)

> **참고**: 현재는 모놀리식(`00-monolithic/`)으로 시작합니다. 아래는 부하 테스트를 통해 병목 지점을 파악한 후, 점진적으로 분리될 수 있는 서비스들의 목표 역할입니다.

### 1. API Gateway
- 모든 클라이언트 요청의 진입점
- 라우팅, 인증, 로드 밸런싱

### 2. User Service
- 사용자 인증/인가
- 사용자 정보 관리
- **DB**: RDBMS (샤딩 가능)

### 3. Chat Room Service
- 채팅방 생성/수정/삭제
- 채팅방 멤버 관리
- **DB**: RDBMS (샤딩 가능)

### 4. Message Service
- 메시지 전송/수신
- 메시지 저장
- **DB**: RDBMS (샤딩/파티셔닝 - 메시지 데이터는 시간/채팅방 기준으로 파티셔닝)
- **Kafka**: 메시지 이벤트 발행
- **Redis**: 실시간 메시지 캐싱

### 5. Search Service
- 메시지 검색
- **Elasticsearch**: 메시지 인덱싱 및 검색
- **Kafka**: 메시지 이벤트 구독하여 Elasticsearch에 인덱싱

### 6. Notification Service
- 푸시 알림
- 알림 설정 관리
- **Kafka**: 이벤트 구독

### 7. WebSocket Service
- 실시간 메시지 전송
- WebSocket 연결 관리
- **Redis**: Pub/Sub을 통한 메시지 브로드캐스팅

## 기술 스택

### 언어/프레임워크
- **Kotlin Spring Boot** (주요)
- **추가 검토 중**: Go, NestJS (TypeScript)

### 데이터베이스
- **RDBMS**: MySQL (샤딩/파티셔닝)
- **Redis**: 캐싱, 세션 관리, 실시간 메시지 큐
- **Elasticsearch**: 메시지 검색 (향후 도입)

### 메시징/이벤트
- **Kafka**: 이벤트 스트리밍, 메시지 큐 (향후 도입)

자세한 기술 스택은 [아키텍처 문서](../../00-docs/architecture.md)를 참고하세요.

## 개발 가이드

### 초기 개발 (모놀리식)

1. **모놀리식 서비스 구현**: `00-monolithic/` 디렉토리에서 모든 기능을 구현
2. **기능 구현**: 사용자 인증, 채팅방, 메시지, WebSocket 등 핵심 기능 구현
3. **테스트 작성**: 단위 테스트 및 통합 테스트 작성

### 부하 테스트

부하 테스트는 `04-test/load-test/` 디렉토리에서 관리합니다.

- **도구**: k6
- **목표**: 병목 지점 파악 및 성능 메트릭 수집
- **단계**: 100 → 500 → 1,000 → 5,000 → 10,000 동시 사용자

### 점진적 분리

부하 테스트 결과를 바탕으로 병목 지점을 파악하고, 필요에 따라 서비스를 분리합니다.

**분리 우선순위**:
1. Message Service (메시지 처리 병목 시)
2. Search Service (검색 부하 시)
3. WebSocket Service (연결 관리 복잡 시)
4. Notification Service (알림 로직 복잡 시)
5. User Service, Chat Room Service (독립적 스케일링 필요 시)

## 공유 라이브러리 (shared/)

서비스 간 공통으로 사용되는 코드를 관리합니다.

### common/
- 공통 에러 정의
- 로깅 유틸리티
- 유틸리티 함수

### config/
- 공통 설정
- 환경 변수 관리

### proto/
- gRPC 프로토콜 정의 (선택)
- 서비스 간 통신을 위한 프로토콜 버퍼 정의

## 데이터베이스 전략

### Database per Service 패턴

각 마이크로서비스마다 독립적인 데이터 저장소 인스턴스를 운영합니다.

- 서비스 간 데이터 결합도 최소화
- 서비스별 독립적인 스케일링 가능
- 장애 격리: 한 서비스의 데이터 저장소 장애가 다른 서비스에 영향 없음

### 샤딩/파티셔닝 전략

- **User Service**: 사용자 ID 기준 샤딩
- **Chat Room Service**: 채팅방 ID 기준 샤딩
- **Message Service**: 채팅방 ID 기준 샤딩 + 시간 기반 파티셔닝 (월별/년별)

## 배포

배포 관련 설정은 `01-infrastructure/03-deployment/` 디렉토리에서 관리합니다.

- **로컬 개발**: Docker Compose
- **프로덕션**: Kubernetes (Helm)

## 모니터링

모니터링 설정은 `01-infrastructure/04-monitoring/` 디렉토리에서 관리합니다.

- **메트릭**: Prometheus
- **시각화**: Grafana
- **로깅**: Loki

## 참고 문서

- [백엔드 서비스 설계 전략](../../00-docs/patch/2026-01-06-04-백엔드서비스설계전략.md)
- [아키텍처 문서](../../00-docs/architecture.md)
- [프로젝트 구조](../../00-docs/project-structure.md)
- [인프라 구조](../../01-infrastructure/README.md)

## 개발 원칙

1. **YAGNI (You Aren't Gonna Need It)**: 실제 필요할 때만 기능 추가
2. **데이터 기반 의사결정**: 추측이 아닌 부하 테스트 결과로 병목 파악
3. **점진적 개선**: 한 번에 모든 것을 구축하지 않고 단계적으로 개선
4. **서비스 독립성**: 분리된 서비스는 독립적으로 배포 및 스케일링 가능해야 함
