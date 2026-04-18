# Core-first Admin Implementation Plan / Core 우선 Admin 구현 계획

## 1. Purpose / 목적

This document defines the implementation phases for completing the admin and messaging plan with a core-first architecture.
이 문서는 core 우선 아키텍처로 admin 및 messaging 계획을 완성하기 위한 구현 phase를 정의한다.

The final goal is to implement the admin product plan while gradually moving new and related backend work into `core`.
최종 목표는 admin 제품 계획을 구현하면서, 새로 만들거나 연관 리팩터링하는 백엔드 작업을 점진적으로 `core`로 이동하는 것이다.

The migration must be use-case driven, not a broad package rewrite.
마이그레이션은 전체 패키지 재작성 방식이 아니라 use case 중심으로 진행해야 한다.

Existing feature packages may remain during the transition.
전환 기간 동안 기존 feature package는 유지될 수 있다.

New or substantially refactored behavior should enter through `core` application services.
새로 만들거나 의미 있게 리팩터링하는 동작은 `core` application service를 통해 들어가야 한다.

Controllers should stay thin and delegate to application facades or query services.
Controller는 얇게 유지하고 application facade 또는 query service에 위임해야 한다.

## 2. Target Direction / 목표 방향

The target backend structure follows the architecture described in `AGENTS.md`.
목표 백엔드 구조는 `AGENTS.md`에 정의된 아키텍처를 따른다.

`presentation` owns delivery concerns such as HTTP controllers, WebSocket handlers, request DTOs, and response DTOs.
`presentation`은 HTTP controller, WebSocket handler, request DTO, response DTO 같은 delivery 관심사를 소유한다.

`core` owns bounded-context business logic.
`core`는 bounded context의 business logic을 소유한다.

`common` remains a technical platform area and should not become a business shared bucket.
`common`은 기술 플랫폼 영역으로 유지하며 business 공용 저장소가 되어서는 안 된다.

The first implementation line should introduce `iam` and `conversation` as separate bounded contexts.
첫 구현 라인은 `iam`과 `conversation`을 별도 bounded context로 도입해야 한다.

Admin channel management, channel membership, channel end users, conversations, messages, and realtime delivery belong primarily to the conversation context.
Admin channel 관리, channel membership, channel end user, conversation, message, realtime delivery는 주로 conversation context에 속한다.

IAM identity owns user lifecycle and global roles.
IAM identity는 user lifecycle과 global role을 소유한다.

IAM access owns login, token issuing, token verification, refresh, logout, and login failure policy.
IAM access는 login, token issuing, token verification, refresh, logout, login failure policy를 소유한다.

The new core line should use only core-owned models, ports, facades, and tables.
새 core 라인은 core가 소유한 model, port, facade, table만 사용해야 한다.

## 3. Migration Principles / 마이그레이션 원칙

Move coherent use-case slices, not isolated files.
고립된 파일이 아니라 일관된 use-case slice 단위로 이동한다.

Do not move every existing package just for architectural purity.
아키텍처 순수성만을 위해 기존 모든 package를 이동하지 않는다.

Avoid touching existing controllers unless a separate compatibility task requires it.
별도 compatibility 작업이 필요하지 않다면 기존 controller는 수정하지 않는다.

When adding new admin endpoints, prefer placing them under `presentation/http/admin`.
새 admin endpoint를 추가할 때는 `presentation/http/admin` 아래에 두는 방식을 우선한다.

Domain code in `core` should be pure Kotlin.
`core`의 domain code는 순수 Kotlin이어야 한다.

New domain code should not depend on Spring, JPA, Redis, Web, WebSocket, or serialization frameworks.
새 domain code는 Spring, JPA, Redis, Web, WebSocket, serialization framework에 의존하지 않아야 한다.

Facades should own orchestration entry points and transaction boundaries for new write flows.
새 write flow의 orchestration entry point와 transaction boundary는 facade가 소유해야 한다.

New core infrastructure should use new core tables and should not call legacy feature repositories.
새 core infrastructure는 새 core table을 사용해야 하며 legacy feature repository를 호출하지 않아야 한다.

Facade classes are the only application entry points that controllers may call.
Facade class는 controller가 호출할 수 있는 유일한 application entry point다.

Transaction boundaries should be declared only on facades.
Transaction boundary는 facade에만 선언해야 한다.

Application services and domain services should run inside the facade transaction.
Application service와 domain service는 facade transaction 안에서 실행되어야 한다.

Database table prefixes should follow bounded-context ownership.
Database table prefix는 bounded context 소유권을 따라야 한다.

Initial prefixes are `iam_` for IAM and `cv_` for conversation.
초기 prefix는 IAM의 `iam_`, conversation의 `cv_`다.

## 4. Phase 0: Core Entry Foundation / Phase 0: Core 진입 기반

Goal: create the new core-owned admin line across IAM, conversation, and presentation.
목표: IAM, conversation, presentation 전반에 새 core 소유 admin 라인을 만든다.

Phase 0 is split into smaller foundation slices because IAM, conversation, and presentation now move together.
IAM, conversation, presentation이 함께 이동하므로 Phase 0은 더 작은 기반 slice로 나눈다.

## 4.1 Phase 0A: Architecture Skeleton / Phase 0A: 아키텍처 골격

Goal: create the package boundaries for the new implementation line.
목표: 새 구현 라인의 package boundary를 만든다.

Create package roots only when they contain real code.
실제 code가 들어갈 때만 package root를 만든다.

```text
site.rahoon.message.monolithic
  core/
    shared/
    iam/
      exception/
      identity/
        application/
          facade/
          service/
          port/
        domain/
        infrastructure/
          persistence/
            user/
          security/
          bootstrap/
      access/
        application/
          facade/
          service/
          port/
          model/
        domain/
        infrastructure/
          persistence/
            refreshtoken/
            loginfailure/
          identity/
          security/
    conversation/
      exception/
      application/
        facade/
        service/
        port/
      domain/
      infrastructure/
        persistence/
          channel/
          membership/
        iam/
  presentation/
    http/
      auth/
      admin/
      exception/
      shared/
    websocket/
```

Do not create `core/{boundedContext}/shared` until a real bounded-context-local shared concept appears.
실제 bounded-context-local shared 개념이 생기기 전에는 `core/{boundedContext}/shared`를 만들지 않는다.

Persistence adapters should live under `infrastructure/persistence/{aggregate}`.
Persistence adapter는 `infrastructure/persistence/{aggregate}` 아래에 둔다.

Non-persistence adapters should live under infrastructure folders named by adapter kind or target, such as `security`, `bootstrap`, or `iam`.
Persistence가 아닌 adapter는 `security`, `bootstrap`, `iam`처럼 adapter 종류 또는 대상 이름을 가진 infrastructure folder 아래에 둔다.

Keep legacy packages in place, but do not use them from the new core line.
Legacy package는 남겨두되, 새 core 라인에서는 사용하지 않는다.

Reason: this gives the project a clear architectural landing zone without blocking feature delivery.
이유: 기능 개발을 막지 않으면서 명확한 아키텍처 착지점을 제공하기 때문이다.

## 4.2 Phase 0B: Shared Error Contract / Phase 0B: 공통 Error 계약

Goal: standardize exception shape across bounded contexts.
목표: bounded context 전반의 exception 형태를 표준화한다.

Create core shared error contracts.
Core shared error contract를 만든다.

```text
core/shared/error/CoreError.kt
core/shared/error/CoreException.kt
core/shared/error/BoundedContext.kt
core/shared/error/ErrorCategory.kt
```

The error contract should include bounded context, application code, user message, developer message, category, and optional details.
Error contract는 bounded context, application code, user message, developer message, category, optional details를 포함해야 한다.

Use `ErrorCategory` instead of direct HTTP status in core.
Core에서는 직접 HTTP status 대신 `ErrorCategory`를 사용한다.

Map `ErrorCategory` to HTTP status in presentation exception handling.
Presentation exception handling에서 `ErrorCategory`를 HTTP status로 변환한다.

Reason: core errors should be consistent but should not depend on HTTP or Spring types.
이유: core error는 일관되어야 하지만 HTTP 또는 Spring type에 의존해서는 안 되기 때문이다.

## 4.3 Phase 0C: IAM Identity / Phase 0C: IAM Identity

Goal: create the new core-owned backoffice user model.
목표: core가 소유하는 새 backoffice user 모델을 만든다.

Create identity tables with the IAM prefix.
IAM prefix를 사용해 identity table을 만든다.

```text
iam_users
```

Initial `iam_users` fields should include id, email, password hash, nickname, global role, created time, and updated time.
초기 `iam_users` field는 id, email, password hash, nickname, global role, created time, updated time을 포함해야 한다.

Use only the target global roles.
목표 global role만 사용한다.

```text
GlobalRole: PLATFORM_ADMIN, CHANNEL_USER
```

Store password hash directly on `iam_users` for Phase 0.
Phase 0에서는 password hash를 `iam_users`에 직접 저장한다.

Create or load customer admin identity from an identity facade or identity application service.
Identity facade 또는 identity application service에서 customer admin identity를 생성하거나 조회한다.

Reason: IAM access should authenticate against a core-owned IAM identity model, not legacy users.
이유: IAM access는 legacy users가 아니라 core가 소유한 IAM identity model을 기준으로 인증해야 하기 때문이다.

## 4.4 Phase 0D: IAM Access / Phase 0D: IAM Access

Goal: create the new core-owned authentication and session line.
목표: core가 소유하는 새 authentication 및 session 라인을 만든다.

Create access tables with the IAM prefix.
IAM prefix를 사용해 access table을 만든다.

```text
iam_refresh_tokens
iam_login_failures
```

Create access facades for login, refresh, logout, and access token verification.
Login, refresh, logout, access token verification을 위한 access facade를 만든다.

Create a new authenticated principal model for presentation and core facades.
Presentation과 core facade가 사용할 새 authenticated principal model을 만든다.

Support refresh token in both cookie and request body.
Refresh token은 cookie와 request body 방식을 모두 지원한다.

Create a new auth argument resolver.
새 auth argument resolver를 만든다.

The new resolver should verify tokens through the access facade.
새 resolver는 access facade를 통해 token을 검증해야 한다.

The new admin controllers should not use legacy `CommonAuthInfo`.
새 admin controller는 legacy `CommonAuthInfo`를 사용하지 않아야 한다.

Reason: admin auth should be implemented through the IAM boundary used by the rest of the new admin line.
이유: admin auth는 새 admin 라인의 나머지 부분이 사용하는 IAM boundary를 통해 구현되어야 하기 때문이다.

## 4.5 Phase 0E: Conversation Core Channel and Membership / Phase 0E: Conversation Core Channel 및 Membership

Goal: create new core-owned tenant and membership tables.
목표: core가 소유하는 새 tenant 및 membership table을 만든다.

Create conversation tables with the conversation prefix.
Conversation prefix를 사용해 conversation table을 만든다.

```text
cv_channels
cv_channel_memberships
```

Do not add widget API keys to `cv_channels` in Phase 0.
Phase 0에서는 `cv_channels`에 widget API key를 추가하지 않는다.

Add widget integration credentials later through a dedicated integration table.
Widget integration credential은 나중에 별도 integration table을 통해 추가한다.

Recommended future table:
향후 추천 table:

```text
cv_channel_integrations
```

Use channel membership roles.
Channel membership role을 사용한다.

```text
ChannelMembershipRole: CHANNEL_ADMIN, AGENT
AgentStatus: ONLINE, AWAY, OFFLINE
```

Create the initial customer admin membership when a platform admin creates a channel.
Platform admin이 channel을 생성할 때 초기 customer admin membership을 함께 생성한다.

Generate a temporary password on the server for the initial customer admin.
초기 customer admin을 위한 temporary password는 server에서 생성한다.

Return the temporary password only once in the channel creation response.
Temporary password는 channel 생성 response에서 한 번만 반환한다.

Defer invitation flow to a later phase.
Invitation flow는 이후 phase로 미룬다.

Reason: the new admin model needs a clean tenant boundary and channel role model before inbox and reply workflows.
이유: 새 admin 모델은 inbox와 reply workflow 전에 깨끗한 tenant boundary와 channel role model이 필요하기 때문이다.

## 4.6 Phase 0F: First Presentation Slice / Phase 0F: 첫 Presentation Slice

Goal: expose the first new admin endpoints through presentation.
목표: 첫 새 admin endpoint를 presentation을 통해 노출한다.

Create new presentation controllers.
새 presentation controller를 만든다.

```text
presentation/http/auth/AdminAuthController.kt
presentation/http/admin/AdminChannelController.kt
```

Expose the initial auth endpoints.
초기 auth endpoint를 노출한다.

```text
POST /admin/auth/login
POST /admin/auth/refresh
POST /admin/auth/logout
```

Expose the initial channel management endpoints.
초기 channel management endpoint를 노출한다.

```text
POST /admin/channels
GET /admin/channels
GET /admin/channels/{id}
```

Controllers should call only facades.
Controller는 facade만 호출해야 한다.

Use a new presentation response wrapper instead of relying on legacy response wrappers.
Legacy response wrapper에 의존하지 말고 새 presentation response wrapper를 사용한다.

Reason: this proves the new architecture line works end to end.
이유: 새 architecture 라인이 end-to-end로 동작한다는 것을 증명하기 때문이다.

## 5. Phase 1A: Admin Channel Integration Management / Phase 1A: Admin Channel Integration 관리

Goal: allow platform admins to manage widget integrations for a customer channel.
목표: platform admin이 고객사 channel의 widget integration을 관리할 수 있게 한다.

Add a `ChannelIntegration` aggregate root under `core/conversation/domain`.
`core/conversation/domain` 아래에 `ChannelIntegration` aggregate root를 추가한다.

Add an `AllowedOrigins` value object for widget origin policy.
Widget origin 정책을 위해 `AllowedOrigins` value object를 추가한다.

Recommended files:
추천 파일:

```text
core/conversation/domain/ChannelIntegration.kt
core/conversation/domain/AllowedOrigins.kt
core/conversation/application/facade/AdminChannelIntegrationFacade.kt
core/conversation/application/query/AdminChannelIntegrationQueryService.kt
core/conversation/application/port/ChannelIntegrationRepository.kt
core/conversation/application/port/IntegrationKeyGenerator.kt
core/conversation/application/port/IntegrationSecretHasher.kt
core/conversation/infrastructure/persistence/integration/
core/conversation/infrastructure/security/
presentation/http/admin/AdminChannelIntegrationController.kt
```

Use `cv_channel_integrations` for persistence.
Persistence에는 `cv_channel_integrations`를 사용한다.

Keep `public_key` unique at the database level.
`public_key`는 database level에서 unique로 유지한다.

Do not add a unique constraint on `channel_id` and `type`.
`channel_id`와 `type`에는 unique constraint를 추가하지 않는다.

Enforce one active `WIDGET` integration per channel in application policy for now.
현재는 application policy에서 channel당 하나의 active `WIDGET` integration만 허용한다.

Return the raw secret only once when the widget integration is created.
Widget integration이 생성될 때 raw secret은 한 번만 반환한다.

Store only the hashed secret.
Secret은 hash만 저장한다.

Allow `*` in `AllowedOrigins`.
`AllowedOrigins`에서 `*`를 허용한다.

Treat an empty origin list as deny all.
빈 origin 목록은 deny all로 처리한다.

Expose admin integration endpoints.
Admin integration endpoint를 노출한다.

```text
POST /admin/channels/{channelId}/integrations/widget
GET /admin/channels/{channelId}/integrations
PATCH /admin/channels/{channelId}/integrations/{integrationId}/enable
PATCH /admin/channels/{channelId}/integrations/{integrationId}/disable
PATCH /admin/channels/{channelId}/integrations/{integrationId}/allowed-origins
```

Use facades for write use cases and query services for read use cases.
Write use case에는 facade를 사용하고 read use case에는 query service를 사용한다.

Reason: channel integration lifecycle and channel lifecycle are different enough to keep integration as its own aggregate root.
이유: channel integration lifecycle과 channel lifecycle은 충분히 달라서 integration을 별도 aggregate root로 두는 것이 적절하다.

Reason: avoiding a `(channel_id, type)` unique constraint keeps future multiple-widget support cheaper.
이유: `(channel_id, type)` unique constraint를 피하면 future multiple-widget 지원 비용이 낮아진다.

## 5B. Phase 1B: Public Widget Bootstrap / Phase 1B: Public Widget Bootstrap

Goal: allow widget clients to resolve bootstrap data from a public key.
목표: widget client가 public key로 bootstrap data를 조회할 수 있게 한다.

Add a public widget bootstrap query service.
Public widget bootstrap query service를 추가한다.

Recommended files:
추천 파일:

```text
core/conversation/domain/Origin.kt
core/conversation/application/query/WidgetBootstrapQueryService.kt
presentation/http/widget/WidgetBootstrapController.kt
```

Expose a public widget bootstrap endpoint.
Public widget bootstrap endpoint를 노출한다.

```text
POST /widget/bootstrap
```

Prefer the `Origin` header over request body origin.
Request body origin보다 `Origin` header를 우선 사용한다.

Use request body origin only as a fallback for development and test flows.
Request body origin은 development 및 test flow를 위한 fallback으로만 사용한다.

Normalize origin input to scheme, host, and optional port.
Origin input은 scheme, host, optional port로 normalize한다.

The bootstrap flow should require an active integration.
Bootstrap flow는 active integration을 요구해야 한다.

The bootstrap flow should require an active channel.
Bootstrap flow는 active channel을 요구해야 한다.

The bootstrap flow should validate the request origin through `AllowedOrigins`.
Bootstrap flow는 `AllowedOrigins`를 통해 request origin을 검증해야 한다.

Reason: public widget access should not bypass channel and integration lifecycle policy.
이유: public widget 접근이 channel 및 integration lifecycle policy를 우회하면 안 되기 때문이다.

## 6. Phase 2: ChannelMembership Model / Phase 2: ChannelMembership 모델

Goal: complete the core-owned channel-scoped membership model.
목표: core가 소유하는 channel-scoped membership 모델을 완성한다.

Do not extend the legacy `ChannelOperator` model for the new admin line.
새 admin 라인을 위해 legacy `ChannelOperator` 모델을 확장하지 않는다.

Use `ChannelMembership` in domain, application, infrastructure, and presentation naming.
Domain, application, infrastructure, presentation naming에서 `ChannelMembership`을 사용한다.

Use the `cv_channel_memberships` table.
`cv_channel_memberships` table을 사용한다.

Add channel role and agent status fields.
Channel role과 agent status field를 추가한다.

Recommended domain values:
추천 domain 값:

```text
ChannelMembershipRole: CHANNEL_ADMIN, AGENT
AgentStatus: ONLINE, AWAY, OFFLINE
```

Consider adding `invitedAt` and `joinedAt` when invitation flow is implemented.
Invitation flow를 구현할 때 `invitedAt`과 `joinedAt` 추가를 검토한다.

Move member management rules into `core/conversation/application/service` and expose them through facades.
Member management rule은 `core/conversation/application/service`로 이동하고 facade를 통해 노출한다.

Only `PLATFORM_ADMIN` and `CHANNEL_ADMIN` should manage channel members.
`PLATFORM_ADMIN`과 `CHANNEL_ADMIN`만 channel member를 관리할 수 있어야 한다.

Reason: global roles and channel roles must be separated before reliable admin permissions can exist.
이유: 신뢰할 수 있는 admin permission을 만들기 전에 global role과 channel role을 분리해야 하기 때문이다.

Reason: assignment, inbox access, and operator management all depend on membership semantics.
이유: assignee, inbox access, operator management가 모두 membership 의미론에 의존하기 때문이다.

## 7. Phase 3: Admin Channel Management / Phase 3: Admin Channel 관리

Goal: complete the backend API surface needed by the admin channel management page.
목표: admin channel management page에 필요한 backend API surface를 완성한다.

Harden and expand admin channel management APIs.
Admin channel management API를 강화하고 확장한다.

Baseline endpoints:
기준 endpoint:

```text
GET /admin/channels
GET /admin/channels/{id}
```

Move new admin channel use cases through `core/conversation/application/facade`.
새 admin channel use case는 `core/conversation/application/facade`를 통해 처리한다.

Use `presentation/http/admin` for new admin HTTP controllers when adding new endpoints.
새 endpoint를 추가할 때는 새 admin HTTP controller를 `presentation/http/admin`에 둔다.

Introduce channel lifecycle status on `cv_channels`.
`cv_channels`에 channel lifecycle status를 도입한다.

Recommended values:
추천 값:

```text
ChannelStatus: INACTIVE, ACTIVE, SUSPENDED, ARCHIVED
```

Separate platform-only settings from customer-configurable settings.
Platform-only 설정과 customer-configurable 설정을 분리한다.

Reason: admin shell, channel switcher, and channel management all need stable channel read APIs.
이유: admin shell, channel switcher, channel management는 모두 안정적인 channel read API가 필요하기 때문이다.

Reason: channel lifecycle state becomes the tenant-level control point for future operations.
이유: channel lifecycle state는 향후 운영을 위한 tenant-level control point가 되기 때문이다.

## 8. Phase 4: ChannelEndUser Separation / Phase 4: ChannelEndUser 분리

Goal: separate widget customers from backoffice users.
목표: widget customer를 backoffice user와 분리한다.

Introduce `ChannelEndUser` in `core/conversation/domain`.
`core/conversation/domain`에 `ChannelEndUser`를 도입한다.

Create a `cv_channel_end_users` table.
`cv_channel_end_users` table을 만든다.

Use a unique key on `channel_id + email`.
`channel_id + email`에 unique key를 사용한다.

Add a find-or-create use case by channel and email.
Channel과 email 기준 find-or-create use case를 추가한다.

Attach new conversations to `cv_channel_end_users`.
새 conversation은 `cv_channel_end_users`에 연결한다.

Leave legacy conversation tables in place during the transition.
전환 기간 동안 legacy conversation table은 그대로 둔다.

Do not migrate legacy data unless a separate migration decision is made.
별도 migration 결정을 내리기 전에는 legacy data를 migration하지 않는다.

Possible future migration:
가능한 향후 migration:

```text
1. Backfill cv_channel_end_users from legacy data.
2. Backfill cv_conversations from legacy data.
3. Switch selected reads to core tables.
4. Deprecate legacy reads.
5. Drop legacy tables only after explicit approval.
```

Require email before starting a real widget conversation.
실제 widget conversation을 시작하기 전에 email을 필수로 요구한다.

Reason: end users have a different lifecycle and authorization model from backoffice users.
이유: end user는 backoffice user와 다른 lifecycle 및 authorization model을 가지기 때문이다.

Reason: keeping end users in the generic user model will make conversation ownership and message sender semantics ambiguous.
이유: end user를 generic user model에 유지하면 conversation ownership과 message sender 의미가 모호해지기 때문이다.

## 9. Phase 5: Conversation Operations / Phase 5: Conversation 운영 기능

Goal: make conversations usable as an operational inbox.
목표: conversation을 운영 inbox로 사용할 수 있게 만든다.

Create `cv_conversations` and add conversation status.
`cv_conversations`를 만들고 conversation status를 추가한다.

Recommended values:
추천 값:

```text
ConversationStatus: OPEN, PENDING, CLOSED, DORMANT
```

Add assignee support.
Assignee 기능을 추가한다.

Prefer assigning to `ChannelMembership`, not directly to `User`.
직접 `User`에 할당하기보다 `ChannelMembership`에 할당하는 방식을 우선한다.

Add `lastMessageAt` for inbox sorting.
Inbox 정렬을 위해 `lastMessageAt`을 추가한다.

Add status and assignee mutation APIs.
Status 및 assignee 변경 API를 추가한다.

Required endpoints:
필수 endpoint:

```text
PATCH /channels/{channelId}/conversations/{conversationId}/status
PATCH /channels/{channelId}/conversations/{conversationId}/assignee
```

Add inbox list filters.
Inbox list filter를 추가한다.

Recommended filters:
추천 filter:

```text
status
assignee
cursor
limit
```

Reuse an ongoing conversation when the same end user returns to the same channel.
같은 end user가 같은 channel에 돌아오면 ongoing conversation을 재사용한다.

Reason: an inbox needs workflow state, assignment, and activity ordering.
이유: inbox에는 workflow state, assignment, activity ordering이 필요하기 때문이다.

Reason: status and assignee should be modeled before frontend workflows depend on temporary fields.
이유: frontend workflow가 임시 field에 의존하기 전에 status와 assignee를 모델링해야 하기 때문이다.

## 10. Phase 6: Message and Realtime Model / Phase 6: Message 및 Realtime 모델

Goal: align message read/write and realtime delivery with the core conversation model.
목표: message read/write와 realtime delivery를 core conversation 모델에 맞춘다.

Clarify message sender types.
Message sender type을 명확히 한다.

Recommended values:
추천 값:

```text
MessageSenderType: END_USER, CHANNEL_MEMBER, SYSTEM
```

Introduce `cv_conversation_messages` for the new admin and widget conversation line.
새 admin 및 widget conversation 라인을 위해 `cv_conversation_messages`를 도입한다.

Leave legacy `messages` and `chat_rooms` in place until the old line is deprecated.
Old line이 deprecate될 때까지 legacy `messages`와 `chat_rooms`는 그대로 둔다.

Add reply use cases for channel members.
Channel member의 reply use case를 추가한다.

Add message append use cases for end users.
End user의 message append use case를 추가한다.

Move WebSocket subscription checks through `ConversationAccessPolicy`.
WebSocket subscription check를 `ConversationAccessPolicy`를 통해 처리한다.

Consider conversation-scoped or channel-scoped topics for admin inbox realtime sync.
Admin inbox realtime sync를 위해 conversation 단위 또는 channel 단위 topic을 검토한다.

Reason: the existing user-scoped WebSocket topic is useful, but admin inbox needs multi-operator conversation visibility.
이유: 기존 user-scoped WebSocket topic은 유용하지만, admin inbox는 여러 operator가 같은 conversation을 볼 수 있어야 하기 때문이다.

Reason: sender semantics must be clear before replies from agents and messages from end users share one timeline.
이유: agent reply와 end user message가 하나의 timeline을 공유하기 전에 sender 의미가 명확해야 하기 때문이다.

## 11. Phase 7: Admin Frontend Integration / Phase 7: Admin Frontend 연동

Goal: connect the admin frontend to stable backend use cases.
목표: 안정된 backend use case에 admin frontend를 연결한다.

Connect login, refresh, and logout first.
Login, refresh, logout을 먼저 연결한다.

Connect channel list, detail, create, and update.
Channel list, detail, create, update를 연결한다.

Connect membership management.
Membership management를 연결한다.

Connect inbox list and conversation detail after conversation status and assignment exist.
Conversation status와 assignment가 존재한 뒤 inbox list와 conversation detail을 연결한다.

Connect reply flow after message sender semantics are settled.
Message sender 의미가 정리된 뒤 reply flow를 연결한다.

Connect realtime sync after subscription policy is enforced.
Subscription policy가 강제된 뒤 realtime sync를 연결한다.

Add role-based frontend guards, but do not rely on frontend guards for security.
Role 기반 frontend guard를 추가하되, 보안을 frontend guard에 의존하지 않는다.

Reason: frontend integration should not lock the backend into temporary API shapes.
이유: frontend 연동이 backend를 임시 API shape에 고정하면 안 되기 때문이다.

Reason: backend authorization must be the source of truth.
이유: backend authorization이 source of truth여야 하기 때문이다.

## 12. Phase 8: Audit Log / Phase 8: Audit Log

Goal: add traceability for admin and channel operations.
목표: admin 및 channel 운영 작업의 추적성을 추가한다.

Add audit logging after mutation use cases stabilize.
Mutation use case가 안정된 뒤 audit logging을 추가한다.

Recommended audit targets:
추천 audit 대상:

```text
CHANNEL
MEMBERSHIP
CONVERSATION
MESSAGE
```

Recommended audit actions:
추천 audit action:

```text
CREATED
UPDATED
DELETED
STATUS_CHANGED
ASSIGNED
MEMBER_ADDED
MEMBER_REMOVED
```

Store actor information.
Actor 정보를 저장한다.

Store target information.
Target 정보를 저장한다.

Store metadata or before-and-after snapshots when useful.
유용한 경우 metadata 또는 before-and-after snapshot을 저장한다.

Add read APIs only after the write format is stable.
Write format이 안정된 뒤 read API를 추가한다.

Reason: audit logs become expensive to change once data is written.
이유: audit log는 data가 쓰이기 시작하면 변경 비용이 커지기 때문이다.

Reason: adding audit after membership, end user, and conversation models stabilize avoids rewriting audit semantics.
이유: membership, end user, conversation 모델이 안정된 뒤 audit을 추가하면 audit 의미를 다시 작성하는 일을 피할 수 있기 때문이다.

## 13. Phase 9: Stabilization and Release Readiness / Phase 9: 안정화 및 출시 준비

Goal: make the admin system safe to deploy and operate.
목표: admin system을 배포 및 운영 가능한 상태로 만든다.

Define which legacy endpoints remain supported.
어떤 legacy endpoint를 계속 지원할지 정의한다.

Define which legacy endpoints are deprecated.
어떤 legacy endpoint를 deprecate할지 정의한다.

Add integration tests for authorization boundaries.
Authorization boundary에 대한 integration test를 추가한다.

Add smoke tests for admin auth, channel management, inbox, and reply.
Admin auth, channel management, inbox, reply에 대한 smoke test를 추가한다.

Add migration verification tests.
Migration 검증 test를 추가한다.

Add seed data for local admin testing.
Local admin test를 위한 seed data를 추가한다.

Write rollback notes for role, membership, end user, and conversation migrations.
Role, membership, end user, conversation migration에 대한 rollback note를 작성한다.

Document operational runbooks.
운영 runbook을 문서화한다.

Reason: role migration, end user separation, and conversation migration affect data shape and authorization.
이유: role migration, end user separation, conversation migration은 data shape과 authorization에 영향을 주기 때문이다.

Reason: release readiness requires tests and rollback paths, not only completed features.
이유: 출시 준비에는 기능 완성뿐 아니라 test와 rollback path가 필요하기 때문이다.

## 14. Recommended First Implementation Slice / 추천 첫 구현 Slice

Start with Phase 0A through Phase 0F as the first implementation slice.
첫 구현 slice는 Phase 0A부터 Phase 0F까지로 시작한다.

Recommended first slice:
추천 첫 slice:

```text
core/shared/error/*
core/iam/identity/application/facade/*
core/iam/identity/domain/*
core/iam/exception/*
core/iam/identity/infrastructure/persistence/user/*
core/iam/identity/infrastructure/security/*
core/iam/identity/infrastructure/bootstrap/*
core/iam/access/application/facade/*
core/iam/access/application/model/*
core/iam/access/domain/*
core/iam/access/infrastructure/persistence/refreshtoken/*
core/iam/access/infrastructure/persistence/loginfailure/*
core/iam/access/infrastructure/identity/*
core/iam/access/infrastructure/security/*
core/conversation/application/facade/*
core/conversation/application/service/*
core/conversation/domain/*
core/conversation/exception/*
core/conversation/infrastructure/persistence/channel/*
core/conversation/infrastructure/persistence/membership/*
core/conversation/infrastructure/iam/*
presentation/http/auth/*
presentation/http/admin/*
presentation/http/exception/*
presentation/http/shared/*
```

Create the first new tables before adding large admin features.
큰 admin feature를 추가하기 전에 첫 새 table을 만든다.

Recommended first tables:
추천 첫 table:

```text
1. iam_users
2. iam_refresh_tokens
3. iam_login_failures
4. cv_channels
5. cv_channel_memberships
```

Expose the first new endpoints through new controllers.
첫 새 endpoint는 새 controller를 통해 노출한다.

Recommended first endpoints:
추천 첫 endpoint:

```text
POST /admin/auth/login
POST /admin/auth/refresh
POST /admin/auth/logout
POST /admin/channels
GET /admin/channels
GET /admin/channels/{id}
```

Reason: the first slice should prove IAM identity, IAM access, conversation, presentation, transactions, and error handling together.
이유: 첫 slice는 IAM identity, IAM access, conversation, presentation, transaction, error handling이 함께 동작함을 증명해야 하기 때문이다.

## 15. Non-goals / 하지 않을 것

Do not rewrite the entire monolith before implementing admin features.
Admin 기능 구현 전에 monolith 전체를 재작성하지 않는다.

Do not keep two admin authentication lines as a long-term target.
두 admin authentication 라인을 장기 목표로 유지하지 않는다.

Do not create admin-only conversation tables outside the conversation bounded context.
Conversation bounded context 밖에 admin 전용 conversation table을 만들지 않는다.

Do not place business policy in controllers.
Business policy를 controller에 두지 않는다.

Do not promote channel-specific business code into `common`.
Channel-specific business code를 `common`으로 승격하지 않는다.

Do not rely on frontend guards as the primary authorization mechanism.
Frontend guard를 주 authorization mechanism으로 의존하지 않는다.
