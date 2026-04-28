# exp-message

<p align="center">
  <img src="00-docs/assets/exp-message-logo.png" alt="exp-message logo" width="220" />
</p>

## Project Overview
## 프로젝트 소개

exp-message is a messaging platform prototype inspired by Channel Talk.
exp-message는 채널톡에서 영감을 받은 메시징 플랫폼 프로토타입입니다.

It provides an embeddable communication widget for side projects, portfolios, and small B2C services.
사이드 프로젝트, 포트폴리오, 소규모 B2C 서비스를 위한 삽입형 커뮤니케이션 위젯을 제공합니다.

Service owners can communicate with visitors in real time without forcing users to leave the product experience.
서비스 제공자는 사용자가 제품 경험을 벗어나지 않아도 방문자와 실시간으로 소통할 수 있습니다.

The original goal is to provide a Channel Talk-like connection point between service providers and users.
기존 목표는 서비스 제공자와 이용자를 연결하는 채널톡과 유사한 소통 창구를 제공하는 것입니다.

It can be used for customer inquiries, feedback collection, lightweight campaigns, and user engagement features.
고객 문의, 피드백 수집, 가벼운 이벤트, 사용자 참여 기능에 사용할 수 있습니다.

## Why This Project Exists
## 이 프로젝트의 목적

This project is both a product clone and a backend engineering portfolio.
이 프로젝트는 제품 클론이면서 동시에 백엔드 엔지니어링 포트폴리오입니다.

The product goal is to reproduce the core experience of Channel Talk: visitor messaging, admin inbox, real-time delivery, and customer context.
제품 목표는 visitor messaging, admin inbox, real-time delivery, customer context 같은 채널톡의 핵심 경험을 재현하는 것입니다.

The engineering goal is to demonstrate practical experience with modular monolith design, transaction boundaries, concurrency control, event-driven architecture, Kafka, Redis, Elasticsearch, WebSocket scale-out, and load testing.
엔지니어링 목표는 modular monolith design, transaction boundary, concurrency control, event-driven architecture, Kafka, Redis, Elasticsearch, WebSocket scale-out, load testing에 대한 실전 역량을 보여주는 것입니다.

The project intentionally starts as a modular monolith so that service boundaries can be discovered from real product scenarios.
이 프로젝트는 실제 제품 시나리오에서 service boundary를 발견하기 위해 의도적으로 modular monolith로 시작합니다.

Future service extraction should be driven by bounded contexts, operational pressure, and team ownership.
향후 service extraction은 bounded context, operational pressure, team ownership에 의해 결정되어야 합니다.

## Product Direction
## 제품 방향

The product is designed around a visitor widget and an admin workspace.
제품은 visitor widget과 admin workspace를 중심으로 설계됩니다.

Visitors should be able to ask questions, send feedback, and continue conversations without signing up first.
방문자는 먼저 가입하지 않아도 질문하고, 피드백을 보내고, 대화를 이어갈 수 있어야 합니다.

Admins should be able to manage conversations, reply in real time, understand visitor context, and handle multiple channels.
관리자는 conversation을 관리하고, 실시간으로 답장하고, visitor context를 이해하며, 여러 channel을 다룰 수 있어야 합니다.

The long-term direction is to support real-time customer communication, searchable conversation history, presence, assignment, and operational observability.
장기 방향은 real-time customer communication, searchable conversation history, presence, assignment, operational observability를 지원하는 것입니다.

## Core Features
## 핵심 기능

1. Feedback channel: users can communicate with service owners in real time without leaving the service.
1. 피드백 창구: 유저가 서비스를 벗어나지 않고도 서비스 제공자와 실시간으로 소통할 수 있는 창구입니다.

2. Engagement channel: service owners can run lightweight events or interaction features to increase user participation.
2. 참여형 콘텐츠 창구: 서비스 제공자가 간단한 이벤트나 상호작용 기능으로 유저의 유입과 활동을 높일 수 있는 창구입니다.

3. Admin inbox: admins can view, filter, and respond to visitor conversations.
3. 관리자 인박스: 관리자는 visitor conversation을 조회, 필터링, 응답할 수 있습니다.

4. Real-time messaging: visitors and admins can exchange messages through WebSocket-based delivery.
4. 실시간 메시징: 방문자와 관리자는 WebSocket 기반 delivery를 통해 메시지를 주고받을 수 있습니다.

5. Searchable history: conversation messages are planned to be projected into Elasticsearch for full-text search.
5. 검색 가능한 이력: conversation message는 full-text search를 위해 Elasticsearch projection으로 확장될 예정입니다.

## Architecture Direction
## 아키텍처 방향

The backend is written in Kotlin with Spring Boot.
백엔드는 Kotlin과 Spring Boot로 작성됩니다.

The current backend module is `02-backend/00-monolitic`.
현재 backend module은 `02-backend/00-monolitic`입니다.

The new backend core lives under `02-backend/00-monolitic/src/main/kotlin/site/rahoon/message/monolithic/core`.
새로운 backend core는 `02-backend/00-monolitic/src/main/kotlin/site/rahoon/message/monolithic/core` 아래에 위치합니다.

The `core` package is intended to replace legacy feature packages over time through scenario-based migration.
`core` 패키지는 시나리오 기반 migration을 통해 기존 legacy feature package를 점진적으로 대체하기 위한 영역입니다.

The target architecture separates presentation adapters, bounded-context core logic, and common technical platform code.
목표 아키텍처는 presentation adapter, bounded-context core logic, common technical platform code를 분리합니다.

Core application services own use-case orchestration and transaction boundaries.
Core application service는 use-case orchestration과 transaction boundary를 소유합니다.

Domain objects should remain independent from Spring, JPA, Redis, Kafka, Elasticsearch, WebSocket, and transport-specific DTOs.
Domain object는 Spring, JPA, Redis, Kafka, Elasticsearch, WebSocket, transport-specific DTO로부터 독립적이어야 합니다.

Database schema changes are managed through Flyway migrations in `02-backend/01-db-migrations`.
데이터베이스 스키마 변경은 `02-backend/01-db-migrations`의 Flyway migration으로 관리합니다.

## Technical Focus
## 기술적 초점

Outbox Pattern is planned to connect database transactions with Kafka event publishing safely.
Outbox Pattern은 database transaction과 Kafka event publishing을 안전하게 연결하기 위해 도입될 예정입니다.

Kafka is used to decouple message persistence from side effects such as delivery, notification, and search indexing.
Kafka는 message persistence를 delivery, notification, search indexing 같은 side effect와 분리하기 위해 사용합니다.

Elasticsearch is planned as a read projection for fast full-text search over conversation messages.
Elasticsearch는 conversation message에 대한 빠른 full-text search를 위한 read projection으로 계획되어 있습니다.

Redis is planned for distributed presence, visitor rate limiting, and other short-lived coordination state.
Redis는 distributed presence, visitor rate limiting, 기타 short-lived coordination state를 위해 계획되어 있습니다.

WebSocket scale-out is planned so visitors and admins can communicate in real time across multiple application instances.
WebSocket scale-out은 여러 application instance 사이에서도 방문자와 관리자가 실시간으로 소통할 수 있도록 계획되어 있습니다.

k6 load testing is planned to measure throughput, latency, error rate, and bottlenecks with reproducible scenarios.
k6 load testing은 재현 가능한 시나리오로 throughput, latency, error rate, bottleneck을 측정하기 위해 계획되어 있습니다.

## Roadmap
## 로드맵

The detailed backend core roadmap is maintained in `00-docs/06-plan/todo.md`.
상세 backend core 로드맵은 `00-docs/06-plan/todo.md`에서 관리합니다.

Current milestone themes include core conversation flows, database concurrency, Outbox Pattern, Kafka publishing, Elasticsearch projection, Redis presence, WebSocket scale-out, and k6 load testing.
현재 마일스톤 주제는 core conversation flow, database concurrency, Outbox Pattern, Kafka publishing, Elasticsearch projection, Redis presence, WebSocket scale-out, k6 load testing입니다.

Each milestone should be delivered as a demonstrable product or system scenario.
각 마일스톤은 시연 가능한 제품 또는 시스템 시나리오로 완성되어야 합니다.

## Use Cases
## 사용 사례

- Planned adoption for Dohun's portfolio site: [rahoon.site](https://rahoon.site/profile)
- 도훈 포트폴리오 도입 예정: [rahoon.site](https://rahoon.site/profile)
- Planned adoption for Duwon's portfolio.

## Contributing
## 기여하기

If you are interested in development or contribution, please read [CONTRIBUTING.md](./CONTRIBUTING.md).
개발 또는 기여에 관심이 있다면 [CONTRIBUTING.md](./CONTRIBUTING.md)를 참고해 주세요.
