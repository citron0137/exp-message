# Admin Domain Policy Draft / 어드민 도메인 정책 초안

## 1. Summary / 요약

- This document defines the target operating model for three actors: platform admin, channel user, and end user.
- 이 문서는 운영 어드민, 고객사 사용자, 엔드유저의 3가지 존재를 기준으로 목표 운영 모델을 정의한다.

- The tenant boundary is `Channel`, and one customer company owns exactly one channel.
- 테넌트 경계는 `Channel`이며, 고객사 하나는 정확히 하나의 채널을 가진다.

- Backoffice users and end users are separate domain concepts and should not share the same identity model.
- 백오피스 사용자와 엔드유저는 서로 다른 도메인 개념이며 동일한 식별 모델을 공유하지 않는다.

- The current monolith already has a useful starting point, but its role model is still too flat for a ChannelTalk-like product.
- 현재 모놀리식 구조는 출발점으로는 충분하지만, 채널톡 같은 제품을 만들기에는 role 모델이 아직 평평하다.

## 2. Three Actors / 세 가지 존재

### 2.1 Platform Admin / 운영 어드민

- A platform admin is an internal operator of our service.
- 운영 어드민은 우리 서비스의 내부 운영자다.

- This actor can create channels, manage platform-level policies, and access all tenant data.
- 이 존재는 채널 생성, 플랫폼 레벨 정책 관리, 전체 테넌트 데이터 접근이 가능하다.

- Domain mapping: `User` with global role `PLATFORM_ADMIN`.
- 도메인 매핑: 전역 역할 `PLATFORM_ADMIN`을 가진 `User`.

### 2.2 Channel User / 고객사 사용자

- A channel user is a backoffice user who works for a customer company.
- 고객사 사용자는 고객사에서 근무하는 백오피스 사용자다.

- This actor belongs to exactly one `Channel`.
- 이 존재는 정확히 하나의 `Channel`에 소속된다.

- There are two channel-level roles: `CHANNEL_ADMIN` and `AGENT`.
- 채널 레벨 역할은 `CHANNEL_ADMIN`과 `AGENT` 두 가지다.

- `CHANNEL_ADMIN` manages members and channel settings within allowed scope.
- `CHANNEL_ADMIN`은 허용 범위 안에서 멤버 관리와 채널 설정을 담당한다.

- `AGENT` handles conversations and can update their own availability status.
- `AGENT`는 대화를 처리하고 본인 상태를 수정할 수 있다.

- Domain mapping: `User` with global role `CHANNEL_USER`, plus a channel role in membership.
- 도메인 매핑: 전역 역할 `CHANNEL_USER`를 가진 `User`이며, 채널 역할은 membership에서 관리한다.

### 2.3 EndUser / 엔드유저

- An end user is the customer who enters through the web widget.
- 엔드유저는 웹 위젯을 통해 들어오는 최종 고객이다.

- End users are not backoffice users and should not be stored in the same table as `User`.
- 엔드유저는 백오피스 사용자가 아니므로 `User`와 같은 테이블에 저장하면 안 된다.

- End users are identified by email within a channel.
- 엔드유저는 채널 내부에서 이메일로 식별한다.

- Domain mapping: separate `EndUser` aggregate.
- 도메인 매핑: 별도 `EndUser` aggregate.

## 3. Current State / 현재 구조

### 3.1 Existing Resources / 현재 리소스

- `User` exists as the authenticated actor for the monolith.
- 현재 모놀리식에는 인증 주체로 `User`가 존재한다.

- `UserRole` currently has only `ADMIN` and `USER`.
- `UserRole`은 현재 `ADMIN`, `USER` 두 개만 가진다.

- `CommonAuthRole` also has only `ADMIN` and `USER`.
- `CommonAuthRole` 역시 `ADMIN`, `USER` 두 개만 가진다.

- `ChannelOperator` exists as a per-channel user mapping, but it does not yet represent a full channel-level permission model.
- `ChannelOperator`가 채널별 사용자 매핑으로 존재하지만, 아직 채널 권한 모델 전체를 표현하지는 못한다.

- `ChannelConversation` currently stores `customerId`, which is closer to a generic user identity than a dedicated end-user model.
- `ChannelConversation`은 현재 `customerId`를 저장하는데, 이는 전용 엔드유저 모델이라기보다 범용 사용자 식별자에 가깝다.

### 3.2 Current Limitation / 현재 한계

- The current role model distinguishes only admin vs non-admin.
- 현재 role 모델은 admin과 non-admin 정도만 구분한다.

- Customer company admin and agent are not represented as first-class permission roles.
- 고객사 관리자와 상담원은 일급 권한 역할로 표현되지 않는다.

- End users are not yet separated as a dedicated domain concept.
- 엔드유저 역시 아직 별도 도메인 개념으로 분리되어 있지 않다.

## 4. Target Model / 목표 구조

### 4.1 Global Roles / 전역 역할

| Global Role | 설명 |
| --- | --- |
| `PLATFORM_ADMIN` | Internal operator of the platform / 플랫폼 내부 운영자 |
| `CHANNEL_USER` | Backoffice user of a customer channel / 고객사 채널 백오피스 사용자 |

### 4.2 Channel Roles / 채널 역할

| Channel Role | 설명 |
| --- | --- |
| `CHANNEL_ADMIN` | Customer company admin with member and channel setting permissions / 고객사 관리자 |
| `AGENT` | Customer company staff who handles conversations / 고객사 상담원 |

### 4.3 Domain Objects / 도메인 객체

| Domain | Purpose / 목적 |
| --- | --- |
| `User` | Platform admin and customer backoffice users / 운영 어드민과 고객사 백오피스 사용자 |
| `ChannelMembership` | Channel-scoped role and belonging / 채널 소속과 채널 권한 |
| `EndUser` | Widget customer identity / 위젯 고객 식별 주체 |
| `Conversation` | Long-lived relationship between a channel and an end user / 채널과 엔드유저 간 장기 대화 단위 |
| `Ticket` | Internal operational marker inside a conversation / 대화 내부 운영용 구분 단위 |

## 5. Resource Transformation / 기존 리소스 변형 방향

### 5.1 `UserRole`

- `ADMIN` should become `PLATFORM_ADMIN`.
- `ADMIN`은 `PLATFORM_ADMIN`으로 전환하는 것이 적절하다.

- `USER` should become `CHANNEL_USER`.
- `USER`는 `CHANNEL_USER`로 전환하는 것이 적절하다.

- The global role should only answer platform-level questions.
- 전역 역할은 플랫폼 레벨 질문에만 답해야 한다.

### 5.2 `CommonAuthRole`

- `CommonAuthRole` should follow the same global-role semantics as `UserRole`.
- `CommonAuthRole`도 `UserRole`과 동일한 전역 권한 의미를 따라야 한다.

- It should not contain customer-company roles such as admin or agent.
- 고객사 관리자나 상담원 같은 역할은 포함하지 않아야 한다.

### 5.3 `ChannelOperator`

- `ChannelOperator` should evolve into `ChannelMembership`.
- `ChannelOperator`는 `ChannelMembership`로 발전시키는 것이 맞다.

- The current structure already has `channelId` and `userId`, so it is a good base for a membership model.
- 현재 구조도 `channelId`와 `userId`를 가지고 있어 membership 모델의 기반으로 적합하다.

- A new `role` field should be added at this level.
- 이 레벨에 새로운 `role` 필드를 추가해야 한다.

- Recommended role values: `CHANNEL_ADMIN`, `AGENT`.
- 추천 role 값은 `CHANNEL_ADMIN`, `AGENT`다.

### 5.4 `ChannelConversation`

- `customerId` should eventually be replaced by `endUserId`.
- `customerId`는 장기적으로 `endUserId`로 대체하는 것이 좋다.

- The conversation should be attached to an `EndUser`, not to a backoffice `User`.
- 대화는 백오피스 `User`가 아니라 `EndUser`에 연결되어야 한다.

### 5.5 New `EndUser`

- A dedicated `EndUser` resource should be introduced.
- 전용 `EndUser` 리소스를 도입해야 한다.

- End users are channel-scoped and identified by email.
- 엔드유저는 채널 단위로 존재하며 이메일로 식별한다.

- If the same email re-enters the same channel, the system should resolve to the same end user.
- 같은 이메일이 같은 채널에 다시 들어오면 동일 엔드유저로 해석해야 한다.

## 6. Permission Matrix / 권한 매트릭스

| Action / 작업 | `PLATFORM_ADMIN` | `CHANNEL_ADMIN` | `AGENT` | `EndUser` |
| --- | --- | --- | --- | --- |
| Create channel / 채널 생성 | Yes | No | No | No |
| Access all tenant data / 전체 채널 데이터 접근 | Yes | No | No | No |
| Manage own channel settings / 자기 채널 설정 관리 | No | Yes | No | No |
| Invite channel staff / 채널 직원 초대 | No | Yes | No | No |
| Remove channel staff / 채널 직원 삭제 | No | Yes | No | No |
| Update own status / 본인 상태 수정 | No | Yes | Yes | No |
| Participate in conversations / 대화 참여 | Possible for operations / 가능 | Yes | Yes | Yes |
| Create internal ticket / 내부 티켓 생성 | Possible for operations / 가능 | Yes | Yes | No |

## 7. Channel Policy / 채널 정책

- A channel is the tenant boundary and also the billing boundary.
- 채널은 테넌트 경계이자 과금 경계다.

- A customer company owns exactly one channel.
- 고객사 하나는 정확히 하나의 채널을 가진다.

- A `User` belongs to only one channel.
- 하나의 `User`는 하나의 채널에만 속한다.

- Channel creation is platform-admin only.
- 채널 생성은 운영 어드민 전용이다.

- Customer company admins can manage channel configuration only within allowed scope.
- 고객사 관리자는 허용 범위 안에서만 채널 설정을 관리할 수 있다.

### Allowed Customer Configuration / 고객사 수정 가능 범위

- Branding and basic profile / 브랜딩과 기본 프로필
- Greeting message / 인사말
- Operating hours / 운영 시간
- Auto-reply text / 자동응답 문구
- Agent/member management / 상담원 및 멤버 관리

### Platform-Only Configuration / 운영자 전용 설정

- Billing and plan / 결제와 플랜
- Hard platform flags / 플랫폼 운영 플래그
- Critical technical integration settings / 핵심 기술 연동 설정
- Channel force suspend or administrative actions / 강제 정지 등 운영 조치

## 8. EndUser Policy / 엔드유저 정책

- End users may enter the widget anonymously at first.
- 엔드유저는 처음에는 익명 상태로 위젯에 진입할 수 있다.

- Before a real conversation starts, email must be collected.
- 본격적인 대화가 시작되기 전에는 이메일을 반드시 수집해야 한다.

- If email is missing, the UI should show a blocking message such as "Email is required to continue."
- 이메일이 없으면 UI에서 "이메일은 필수입니다" 성격의 차단 알림을 띄워야 한다.

- We do not need email/phone merge logic because the conversation starts only after email capture.
- 본격 대화 전에 이메일을 받기 때문에 복잡한 병합 로직은 우선 필요하지 않다.

- The pre-conversation context may still be summarized as a system note.
- 다만 대화 시작 전 맥락은 시스템 요약 노트로 남길 수 있다.

## 9. Conversation Policy / 대화 정책

- One end user has one ongoing conversation per channel.
- 한 엔드유저는 같은 채널에서 하나의 ongoing conversation을 가진다.

- When the same end user returns, the conversation is resumed instead of creating a new one.
- 같은 엔드유저가 재방문하면 새 대화를 만드는 대신 기존 대화를 이어간다.

- A conversation is not auto-closed.
- 대화는 자동 종료하지 않는다.

- If no message is exchanged for 7 days, the conversation becomes `DORMANT`.
- 마지막 메시지 이후 7일 동안 무응답이면 대화는 `DORMANT` 상태가 된다.

- A new message reactivates the same conversation.
- 새 메시지가 들어오면 같은 대화가 다시 활성화된다.

## 10. Ticket Policy / 티켓 정책

- Tickets are internal objects for channel staff only.
- 티켓은 채널 내부 직원만을 위한 내부 객체다.

- Tickets are not shown to end users.
- 티켓은 엔드유저에게 노출하지 않는다.

- Any channel staff member can create a ticket.
- 채널에 속한 직원이면 누구나 티켓을 생성할 수 있다.

- A single conversation may contain multiple tickets.
- 하나의 대화 안에는 여러 개의 티켓이 존재할 수 있다.

- In the UI, ticket boundaries should be visible to channel staff as timeline events.
- UI에서는 채널 직원이 티켓 경계를 타임라인 이벤트처럼 볼 수 있어야 한다.

## 11. Invitation Policy / 초대 정책

- Only `CHANNEL_ADMIN` can invite new customer-company staff.
- 새로운 고객사 직원을 초대할 수 있는 것은 `CHANNEL_ADMIN`뿐이다.

- Invitation method is email-based.
- 초대 방식은 이메일 기반이다.

- The inviter chooses the target role during invitation.
- 초대 시 초대자가 대상 역할을 선택한다.

- Recommended roles at invitation time: `CHANNEL_ADMIN`, `AGENT`.
- 초대 시 선택 가능한 권장 역할은 `CHANNEL_ADMIN`, `AGENT`다.

## 12. Recommended Rollout / 권장 전환 순서

### Phase 1 / 1단계

- Rename global roles from `ADMIN/USER` to `PLATFORM_ADMIN/CHANNEL_USER`.
- 전역 role을 `ADMIN/USER`에서 `PLATFORM_ADMIN/CHANNEL_USER`로 정리한다.

- Align `CommonAuthRole` with the same meaning.
- `CommonAuthRole`도 같은 의미로 정렬한다.

### Phase 2 / 2단계

- Evolve `ChannelOperator` into `ChannelMembership`.
- `ChannelOperator`를 `ChannelMembership`으로 발전시킨다.

- Add membership role and enforce channel-level authorization.
- membership role을 추가하고 채널 권한 검증을 도입한다.

### Phase 3 / 3단계

- Introduce a dedicated `EndUser` model.
- 전용 `EndUser` 모델을 도입한다.

- Move conversation ownership from generic `customerId` to `endUserId`.
- 대화 소유 기준을 범용 `customerId`에서 `endUserId`로 옮긴다.

### Phase 4 / 4단계

- Introduce ticket timeline semantics in the conversation model and UI.
- 대화 모델과 UI에 티켓 타임라인 의미를 도입한다.

- Add dormant-state lifecycle handling.
- dormant 상태 라이프사이클을 도입한다.

## 13. Open Follow-Ups / 후속 논의 항목

- Invitation expiration and resend policy / 초대 만료 및 재발송 정책
- Agent status values such as `ONLINE`, `AWAY`, `OFFLINE` / Agent 상태값 정의
- Ticket status model such as `OPEN`, `RESOLVED`, `CLOSED` / 티켓 상태 모델
- Whether platform admins need explicit audit logging for customer-data access / 운영자 데이터 접근 감사 로그 필요 여부
