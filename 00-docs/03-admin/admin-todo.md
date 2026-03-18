# Admin App TODO (ChannelTalk-like)

Goal: complete items in order and ship a usable admin app.
Rule: always reuse existing backend/frontend modules first, then add only missing pieces.

## 1) Bootstrap Admin Frontend
- [ ] **Do**: Create a dedicated admin frontend app.
  - **What**: New app scaffold (`03-frontend/03-admin`) with routing.
  - **Already available**: Widget app stack (`Vite + React + TS`) and shared HTTP client pattern.
  - **Reuse strategy**: Copy project skeleton and shared API client conventions from `03-frontend/01-widget`.
  - **How**: Add base layout and route guards only; do not rebuild common request utilities.
  - **Output**: Admin app runs locally with empty pages (`/login`, `/channels`, `/inbox`).

## 2) Admin Authentication
- [ ] **Do**: Connect admin auth APIs.
  - **What**: Login, refresh, logout flow.
  - **Already available**: `/admin/auth/login`, `/admin/auth/refresh`, `/admin/auth/logout`.
  - **Reuse strategy**: Reuse token-store/interceptor approach from widget auth flow; switch endpoints to admin auth.
  - **How**: Store access/refresh tokens and add auto-refresh interceptor.
  - **Output**: Admin can sign in/out and keep session alive.

## 3) Global Admin Shell
- [ ] **Do**: Build admin shell UI.
  - **What**: Sidebar, header, channel switcher, protected routes.
  - **Already available**: Existing auth status/error handling style in widget state management.
  - **Reuse strategy**: Reuse store/action pattern; avoid creating a second auth architecture.
  - **How**: Add app layout + auth guard + loading/unauthorized states.
  - **Output**: Consistent shell for all admin pages.

## 4) Customer Company (Channel) Registration
- [ ] **Do**: Implement channel create flow.
  - **What**: "Register customer company" form.
  - **Already available**: `POST /admin/channels`.
  - **Reuse strategy**: Use existing backend validation and response shape; frontend only submits valid payload.
  - **How**: Call create API, validate required fields, show success/failure feedback.
  - **Output**: New customer channel can be created from admin UI.

## 5) Channel Management Page
- [ ] **Do**: Implement channel management.
  - **What**: Update/delete channel and basic details view.
  - **Already available**: `PUT /admin/channels/{id}`, `DELETE /admin/channels/{id}`.
  - **Reuse strategy**: Use existing admin channel APIs first; add only one missing read endpoint if required.
  - **How**: Build UI with current APIs; if list endpoint is missing, add `GET /admin/channels` only (no duplicate read paths).
  - **Output**: Admin can manage customer channels end-to-end.

## 6) Operator Management by Channel
- [ ] **Do**: Implement operator CRUD page.
  - **What**: List/add/update/remove operators for selected channel.
  - **Already available**: `/channels/{channelId}/operators` CRUD.
  - **Reuse strategy**: Keep operator lifecycle fully on this API; do not create separate admin-only operator APIs unless needed.
  - **How**: Add form validation and optimistic UI updates.
  - **Output**: Channel operators are manageable in one screen.

## 7) Inbox (Conversations List)
- [ ] **Do**: Build channel inbox list.
  - **What**: Conversation list for selected channel.
  - **Already available**: `GET /channels/{channelId}/conversations`.
  - **Reuse strategy**: Start with current list payload; only add fields missing for inbox UX.
  - **How**: Show paging and empty/loading/error states.
  - **Output**: Admin can browse channel conversations.

## 8) Conversation Detail + Reply
- [ ] **Do**: Build conversation detail panel.
  - **What**: Message history + reply box.
  - **Already available**: `GET /messages?chatRoomId=...`, `POST /messages`.
  - **Reuse strategy**: Reuse message DTOs and send/load flow from widget chat module.
  - **How**: Support send error retries and standard message rendering.
  - **Output**: Admin can read and reply in a conversation.

## 9) Realtime Message Sync
- [ ] **Do**: Wire realtime updates.
  - **What**: New messages appear without refresh.
  - **Already available**: WebSocket message relay and topic subscription flow in backend/widget.
  - **Reuse strategy**: Reuse same websocket client contract and subscription model; do not introduce a second realtime protocol.
  - **How**: Subscribe per admin/operator session and merge events into active thread/inbox.
  - **Output**: Live conversation experience in admin.

## 10) Conversation Operations (New Backend)
- [ ] **Do**: Add essential conversation operations.
  - **What**: Status (`open/pending/closed`) and assignee.
  - **Already available**: Base conversation domain/table and CRUD.
  - **Reuse strategy**: Extend existing `channel_conversations` model and service; avoid creating parallel "admin_conversation" tables.
  - **How**: Add DB columns + migration + APIs (`PATCH status`, `PATCH assignee`) + frontend controls.
  - **Output**: Basic support workflow management is available.

## 11) Access Control Hardening
- [ ] **Do**: Enforce admin roles.
  - **What**: Owner/admin/agent permissions.
  - **Already available**: Admin auth boundary (`/admin/**`), role checks via auth resolver patterns.
  - **Reuse strategy**: Build on existing authorization mechanism; add granular policy checks, not a separate auth system.
  - **How**: Apply backend authorization checks + frontend feature guards by role.
  - **Output**: Sensitive actions are restricted to proper roles.

## 12) Audit Log (New Backend)
- [ ] **Do**: Add audit trail for admin actions.
  - **What**: Track who changed status/assignee/operators/channels.
  - **Already available**: Service-layer action points where mutations already happen.
  - **Reuse strategy**: Hook into existing mutation services; avoid duplicate write paths.
  - **How**: Create audit table + write logs in service layer + simple read API/page.
  - **Output**: Action traceability for operations and debugging.

## 13) Release Readiness
- [ ] **Do**: Stabilize and document.
  - **What**: Basic tests, seed data, runbook.
  - **Already available**: Existing test conventions and script structure in repository.
  - **Reuse strategy**: Follow current test/script patterns; do not introduce a separate tooling stack for admin only.
  - **How**: Add smoke tests for auth/inbox/reply, write setup and rollback notes.
  - **Output**: Team can deploy and operate admin safely.

