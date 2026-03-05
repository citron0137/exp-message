# R-Message Notification System Design

## 0. Overview 

R-Message는 실시간 메시징 시스템이며, 메시지 이벤트를 기반으로 다양한 알림을 전달할 수 있는 알림 시스템을 구축하는 것을 목표로 한다.

본 문서는 R-Message에서 메시지 이벤트 기반 알림 시스템을 설계하고 구현하는 과정을 설명한다.

## Table of Contents

- [1. Problem](#1-problem)
- [2. Requirements](#2-requirements)
- [3. Traffic Assumptions](#3-traffic-assumptions)
- [4. Architecture Overview](#4-architecture-overview)
- [5. Domain Design](#5-domain-design)
- [6. Event Design](#6-event-design)
- [7. Data Model](#7-data-model)
- [8. Failure Handling](#8-failure-handling)
- [9. Observability](#9-observability)
- [10. Scaling Strategy](#10-scaling-strategy)
- [11. Load Testing](#11-load-testing)
- [12. Retrospective](#12-retrospective)

## 1. Problem

### 1.1. WebSocket 의존적인 메시지 전달 구조

- 현재 메시지 수신은 WebSocket 연결 상태에 의존한다.
- 사용자가 WebSocket 연결을 유지하지 않는 경우 메시지를 인지할 수 없다.
- 애플리케이션을 보고 있지 않은 경우 메시지 이벤트를 놓칠 수 있다.

### 1.2. 통합 알림 도메인 부재

- 메시지 외 이벤트를 처리할 공통 알림 시스템이 존재하지 않는다.

예시 이벤트

- 메시지 멘션
- 채팅방 초대
- 시스템 공지

### 1.3. 알림 전달 채널 부재

- 현재 시스템에는 사용자에게 알림을 전달할 채널이 존재하지 않는다.
- Email, Push Notification 등 다양한 전달 수단을 지원할 구조가 없다.

### 1.4. 메시지 시스템과 알림 시스템의 강한 결합 가능성

- 메시지 생성 과정에서 알림 처리가 직접 수행될 경우 시스템 간 결합도가 증가한다.
- 알림 처리 실패가 메시지 처리 흐름에 영향을 줄 수 있다.

## 2. Requirements

### 2.1 Functional Requirements

알림 시스템은 다음 기능을 제공해야 한다.

1. 메시지 이벤트 발생 시 사용자에게 알림을 생성할 수 있어야 한다.
2. 메시지 외 다양한 이벤트에 대해 알림을 생성할 수 있어야 한다.
3. 생성된 알림을 사용자에게 전달할 수 있어야 한다.
4. 알림 전달 채널은 확장 가능해야 한다.

지원해야 할 알림 채널 (초기)

- Email

향후 확장 가능한 채널

- Push Notification
- SMS
- Webhook

### 2.2 Non-Functional Requirements

알림 시스템은 다음 비기능 요구사항을 만족해야 한다.

1. 알림 처리 실패 시 재시도할 수 있어야 한다.
2. 동일 이벤트에 대한 알림 중복 처리를 방지할 수 있어야 한다.
3. 알림 처리 과정에서 메시지 시스템과의 결합도를 최소화해야 한다.
4. 트래픽 증가에 대응할 수 있도록 수평 확장이 가능해야 한다.
5. 알림 처리 상태를 관측할 수 있어야 한다.


## 3. Traffic Assumptions

본 섹션의 수치는 현재 Message 도메인의 실제 처리량을 의미하지 않는다.  
알림 도메인(Notification BC)의 설계/확장성/장애대응을 검증하기 위한 **목표 부하 가정**이며,
Assumptions는 측정 전 가정이며, Load Testing에서 실제 측정값을 명시한다.

### 3.1 User Traffic

| Metric | Value |
|------|------|
| DAU | 10,000 |
| Peak Concurrent Users | 2,000 |

### 3.2 Message Traffic

| Metric | Value |
|------|------|
| Average Message Rate | 50 msg/s |
| Peak Message Rate | 200 msg/s |

메시지 생성 시 `MessageCreated` 이벤트가 발생하며,  
알림 시스템은 해당 이벤트를 기반으로 알림을 생성한다.

### 3.3 Notification Traffic

메시지 1건이 여러 사용자에게 알림을 발생시킬 수 있다.

예시

- 채팅방 참여자 수
- 멘션 이벤트

평균 알림 Fan-out을 다음과 같이 가정한다.

| Metric | Value |
|------|------|
| Average Fan-out | 3 |
| Peak Fan-out | 10 |

이를 기반으로 예상 알림 처리량을 계산하면 다음과 같다.

| Metric | Value |
|------|------|
| Average Notification Rate | 150 notif/s |
| Peak Notification Rate | 2000 notif/s |

### 3.4 Latency Goals

알림 시스템의 목표 처리 지연은 다음과 같다.

| Metric | Target |
|------|------|
| P95 Notification Latency | < 2 seconds |
| P99 Notification Latency | < 5 seconds |

### 3.5 Burst Traffic

특정 이벤트 발생 시 알림 트래픽이 순간적으로 증가할 수 있다.

예시

- 인기 채팅방 메시지 폭증
- 시스템 공지
- 대규모 멘션 이벤트

이를 고려하여 시스템은 **Burst 트래픽을 처리할 수 있어야 한다.**

## 4. Architecture Overview

### Step 1

기본적인 이메일 전송 및 실패시 재처리 로직 구현

```
- 모듈(패키지) 분리: Message / Notification
- Message 모듈: MessageCreated 로컬 이벤트 발행 (현행 유지)
- Notification 모듈: 
    1. MessageCreated 로컬 이벤트 수신 하여 아래 과정을 비동기로 처리
    2. Notification 생성 
    3. 관련된 유저 Email 조회
    4. NotificationJob 생성
    5. NotificationJob 기반으로 이메일 전송 및 실패시 재시도 로직 작성
        - 레이트리밋/백오프/서킷브레이커 로직 적용
- Notification 모듈은 영속성 DB를 사용하지 않는다.
```

### Step 2

자체 DB 구축으로 멱등성 보장 및 전송 상태 저장

```
Step 1에서 아래와 같은 변경 적용

- Notification 전용 DB 구축
- Notification 생성시 테이블에 Notification 저장
- NotificationJob 생성시 테이블에 저장 
    - status, 재시도 정보, 실패 원인을 함께 저장
- 이메일 전송 및 실패시 재시도 로직은 Worker가 NotificationJob을 기준으로 동작

이를 통해 아래와 같은 이점을 가져간다

- 유저가 Notification 조회 가능
- NotifcationJob 전송 상태 추적 가능

그럼에도 부족한 부분
- LocalEvent를 비동기로 처리함에 따른 이벤트 유실 가능성
- 트래픽이 높아졌을 때 Scale-out 처리 필요
- DB Queue(폴링) 기반 워커의 경합/락/성능 병목 가능성
- Notification과 NotificationJob의 DB가 아주 커질 가능성
```

### Step 3

STEP2를 테스트하고 발생하는 문제에 대해 개선하도록 함

## 5. Domain Design

## 6. Event Design

## 7. Data Model

## 8. Failure Handling

## 9. Observability

## 10. Scaling Strategy

## 11. Load Testing

## 12. Retrospective