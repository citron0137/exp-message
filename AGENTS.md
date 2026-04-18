# Codex Rules for exp-message

## Project Context

This repository is a messaging platform prototype.

- Backend: Kotlin, Spring Boot, JPA, Redis, WebSocket.
- Main backend module: `02-backend/00-monolitic`.
- The service is currently a modular monolith.
- Future service extraction should be driven by real bounded contexts, operational pressure, and team ownership, not by package names alone.

## Change Strategy

Ideal rules describe the target architecture for new code.

- New code should follow the ideal rules in this document.
- Existing code may not fully follow these rules. Do not perform broad architectural migration unless the user explicitly asks for it.
- For small scoped changes, keep local consistency with the surrounding package while avoiding the spread of known legacy patterns.
- If an ideal rule conflicts with current implementation, prefer a minimal compatible change and mention the architectural gap.
- Do not introduce a one-off hybrid pattern in only one package when peer packages remain on the old pattern.
- When a larger refactor is needed, migrate the full use case or coherent package slice, not a single isolated class.

## Target Package Structure

Ideal top-level backend package structure:

```text
site.rahoon.message.monolithic
  presentation/
    http/
    websocket/
    config/
    exception/
    auth/
    shared/

  core/
    iam/
      application/
        facade/
        service/
        port/
      domain/
      infrastructure/
        persistence/
        security/
      exception/
      shared/
      docs/

    conversation/
      application/
        facade/
        service/
        port/
      domain/
      infrastructure/
        persistence/
        messaging/
        integration/
      exception/
      shared/
      docs/

    notification/
      application/
      domain/
      infrastructure/
      exception/
      shared/
      docs/

    shared/

  common/
```

Meaning:

- `presentation`: delivery adapters. HTTP, WebSocket, request/response DTOs, auth extraction, web/security/websocket config, and exception mapping.
- `core`: business bounded contexts. This is where DDD application, domain, and infrastructure code lives.
- `common`: global technical platform shared code. This is not a bounded context.

### Current Transitional Compromise

The current backend layout uses feature-level top packages such as `user`, `authtoken`, `chatroom`, `message`, and `notification`.

- Keep the current layout unless the task is explicitly about architecture migration.
- New architecture should target `presentation` plus `core/{boundedContext}`.
- New endpoints should prefer `presentation` when adding that structure is in scope.
- Existing controllers and feature packages should be migrated by coherent use case or bounded context slice, not file by file.
- When adding new behavior to existing packages, classify it mentally under the target bounded contexts below.

## Bounded Contexts

Ideal bounded contexts are:

- `iam`: user lifecycle, credential storage, login, token issuing, refresh token, session policy, login failure lockout, token revocation, account status, and global role.
- `conversation`: channel, channel operator, channel conversation, chat room, chat room member, message, real-time conversation delivery contracts.
- `notification`: notification, message notification, email notification job, notification templates, recipient routing.

Do not treat every top-level package as an independent bounded context. Most current packages under `conversation` are aggregates, feature modules, or subdomains within the same larger context.

### IAM Internal Modules

`iam` is one bounded context, but it may keep internal modules such as `identity` and `access` when that keeps roles clear.

`identity` owns user lifecycle and credential storage, including password hashes.

`access` owns authentication and authorization-session behavior such as login, token issuing, refresh token rotation, session expiry, and login failure policy.

- `access` should not depend on the full `identity` domain model.
- `access` should use an internal application port that returns an access-specific snapshot such as `LoginPrincipal`.
- The snapshot should speak the `access` language and contain only what access needs, such as user ID, email, password hash, role, and account status.
- Because `identity` and `access` are internal modules of `iam`, direct internal adapters are acceptable when they remain inside `core/iam`.

### Current Transitional Compromise

Current packages still group `user`, `authtoken`, and `loginfailure` separately.

- Do not move all identity/access code immediately.
- New login/token/session flows should be designed under `core/iam/access`.
- New user/group/profile/lifecycle flows should be designed under `core/iam/identity`.
- Existing direct service calls may remain for small edits, but new cross-BC flows should prefer ports or events.
- Legacy auth code that is actively refactored should move into `core/iam`; core code should depend on core-owned contracts, not legacy feature services.

## Layering

Ideal layer responsibilities inside `core/{boundedContext}`:

- `application`: use-case orchestration, transaction boundaries, facades, query services, ports, application events.
- `domain`: aggregate roots, entities, value objects, domain services, domain events, commands, errors, domain repository contracts when locally consistent.
- `infrastructure`: technical adapters grouped by adapter type. Use `infrastructure/persistence/{aggregate}` for JPA entities, Spring Data repositories, and persistence mappers; use sibling adapter groups such as `messaging`, `security`, `integration`, or `token` when needed.
- `exception`: BC-specific error enums and exception classes.
- `shared`: code shared only inside this bounded context.
- `docs`: concise BC-local documentation for significant aggregate roots and domain concepts.

Ideal dependency direction:

```text
presentation -> core/{bc}/application
core/{bc}/application -> core/{bc}/domain
core/{bc}/infrastructure -> core/{bc}/application ports
core/{bc}/infrastructure -> core/{bc}/domain
core/{bc}/domain -> core/{bc}/shared, core/shared
common -> no presentation or core/{bc} dependency
```

Domain code must not depend on presentation, infrastructure, Spring, JPA, Redis, Web, WebSocket, Security, or serialization frameworks.

### Database Naming

New tables should use a short bounded-context prefix so future service extraction is easier to identify and detach at the database level.

- `iam_`: IAM tables.
- `cv_`: Conversation tables.
- Add new BC prefixes only when introducing a real bounded context.
- Do not rename or drop legacy tables unless the user explicitly asks for a data migration.
- New core flows may create and use new tables instead of reusing legacy tables when the implementation intentionally leaves legacy behavior in place.

## Presentation Layer

Ideal:

- Presentation code lives under `presentation`.
- HTTP controllers and WebSocket handlers are thin delivery adapters.
- Request and response DTOs belong to presentation.
- Presentation extracts auth/session information, parses request data, calls application facades or query services, and maps responses.
- Presentation does not contain business branching or persistence logic.
- Presentation does not depend on infrastructure types.
- Presentation should avoid depending directly on domain entities. Prefer application result DTOs or presentation response DTO mapping from application outputs.
- Presentation should rely on centralized exception handling where possible.

### Current Transitional Compromise

Existing controllers currently live inside feature packages.

- Keep existing controllers in place for small edits.
- New endpoint code should be thin and depend on application services, not domain or infrastructure.
- If a presentation migration starts, move controllers, request DTOs, response DTOs, and related web config together by coherent slice.

## Application Layer

Ideal:

- Application services own use-case orchestration.
- Application services own transaction boundaries for write flows.
- Application services coordinate repositories, domain services, ports, events, and aggregates.
- Application facades are the public mutation entry points for presentation and other delivery adapters.
- Public write services callable from presentation or other delivery adapters should be named and packaged as facades, typically under `application/facade`.
- Public read services callable from presentation or other delivery adapters may be named and packaged as query services under `application/query`.
- `@Transactional` belongs on facade-level application entry points. Lower-level application/domain services should participate in the facade transaction rather than opening their own transaction boundary.
- Query service public methods should usually use `@Transactional(readOnly = true)` when they read through repositories.
- Query services handle read-only query models and may bypass domain models when performance or shape requires it.
- Ports define dependencies on repositories, external systems, and other bounded contexts in the caller's language.
- Application services may parse transport-level formats into domain-friendly values.
- Application services must not own business invariants that belong in domain objects.
- Application services should not be only pass-through wrappers unless local consistency requires it.

### Current Transitional Compromise

Some current application services simply translate criteria to commands and delegate to domain services.

- New use cases should put orchestration in application services.
- When touching a complete write flow, prefer moving transaction ownership to application if the change remains scoped.
- Avoid large responsibility reshuffling during unrelated bug fixes.

## Domain Layer

Ideal:

- Domain code is pure Kotlin.
- Domain classes must not depend on Spring, JPA, Hibernate, Redis, Web, WebSocket, Security, or serialization frameworks.
- Do not add `@Service`, `@Component`, `@Repository`, or `@Transactional` to new domain classes.
- Business invariants belong in entities, value objects, or domain services.
- Domain services are stateless business services for behavior that does not naturally fit inside one entity or value object.
- Domain events represent business facts and should not depend on Spring event classes.
- Prefer value objects for values that carry domain rules or domain meaning, such as email, API key, message content, user ID, chat room ID, channel ID, and token identifiers.
- Avoid putting orchestration, persistence flow, transaction scripts, or response DTO construction in domain services.

### Current Transitional Compromise

Some existing `*DomainService` classes are Spring beans and currently own transaction boundaries.

- Do not spread this pattern to new domain classes.
- For small edits to existing domain services, preserve local consistency unless the user asks for refactoring.
- For new use cases, put orchestration and `@Transactional` on application services.
- For larger refactors, move transaction ownership and repository orchestration from `domain` to `application`, leaving pure domain rules behind.

## Repository and Persistence

Ideal:

- JPA entities, Spring Data repositories, Redis implementations, and persistence mappers live in `infrastructure`.
- New persistence adapters should live under `infrastructure/persistence/{aggregate}`.
- The `{aggregate}` folder should be named after the aggregate root or persistence owner, such as `channel`, `membership`, `user`, or `refreshtoken`.
- Domain models must not reference JPA entities or Redis data structures.
- Repository implementations do not own transaction boundaries.
- Repositories should not hide unrelated commits or external side effects.
- Avoid upsert-style repository methods when the use case clearly knows whether it is creating or updating.
- Keep JPA-specific APIs out of domain-facing repository contracts.

### Current Transitional Compromise

Repository interfaces currently live in `domain` in this project. This is acceptable for now.

- Do not move all repository interfaces just for architectural purity.
- New external service ports or cross-context ports should prefer `application/port` when adding that structure is scoped and consistent.

## Shared Code

Shared code must have an explicit scope. Prefer the narrowest possible scope.

Use this order before promoting code:

```text
same class -> same package -> core/{bc}/shared -> core/shared -> common
```

### `core/{bc}/shared`

BC-local shared code.

- Used only inside one bounded context.
- May contain BC constants, local helper types, local snapshots, and reusable internal policies.
- Must not be imported by other bounded contexts.

### `core/shared`

DDD shared kernel. Keep it very small.

Allowed:

- base domain error and exception contracts
- domain event base contracts
- result and pagination primitives used by multiple bounded contexts
- time or ID abstractions if domain-facing and truly shared

Rules:

- No Spring, JPA, Redis, Web, or WebSocket dependency.
- No BC-specific business policy.
- Add code here only when two or more bounded contexts truly share the same concept and no single BC owns it.

### `presentation/shared`

Presentation-only shared code.

Allowed:

- API response wrappers
- request/response support types
- auth principal argument resolvers and presentation auth helpers
- presentation exception response shapes
- WebSocket reply and error body types

Rules:

- `core` must not import `presentation/shared`.
- Do not put business policy here.

### `common`

Global technical platform shared code. `common` is not a bounded context.

Allowed:

- persistence base classes and repository support
- JPA, Redis, transaction, lock, async, tracing, observability, encoding, and technical configuration helpers
- framework adapters that are not owned by one bounded context

Rules:

- No BC-specific business policy.
- No cross-BC orchestration logic.
- `common` must not depend on `presentation` or a concrete `core/{bc}` package.
- Before adding code to `common`, ask whether the code is used by at least two bounded contexts, has no clear BC owner, and is stable enough to share globally.

## Cross-Context Communication

Ideal:

- A bounded context should not directly depend on another bounded context's concrete application service or domain model.
- Cross-context communication should use ports/adapters, domain events, or application events.
- A port should speak the caller context's language.
- Cross-context writes must keep transaction ownership explicit in the top-level application use case.
- Business notifications and token/session flows are not best-effort by default. Make success/failure semantics explicit.

### Current Transitional Compromise

This is currently a monolith prototype, and some package services call each other directly.

- Direct calls within the larger `conversation` context are acceptable for now.
- New flows crossing `iam`, `conversation`, and `notification` should prefer events or ports.
- Domain code must not import another bounded context's concrete infrastructure, presentation, or controller types.
- If a direct cross-context call is retained for local consistency, keep it in application code and mention the boundary tradeoff when relevant.

## Events and Side Effects

Ideal:

- Domain events describe business facts.
- Application events may be used for framework delivery and integration.
- Side effects such as notification dispatch, email job creation, WebSocket fanout, token revocation propagation, and external calls should be triggered from application-level orchestration or event handlers.
- Do not perform external side effects inside entities or value objects.

### Current Transitional Compromise

Existing Spring application events may live in application packages.

- Keep existing event style for small changes.
- For new domain-significant events, prefer a domain event concept and adapt it to Spring at the application boundary.

## Exceptions and Errors

Ideal:

- Domain/application errors should use project-specific domain error types and exceptions.
- BC-specific errors and exceptions live under `core/{bc}/exception`.
- Shared error contracts live in `core/shared/error` only when multiple bounded contexts need the same shape.
- BC exceptions should expose a consistent shape: bounded context, application error code, user-facing message, developer-facing message, HTTP status, and optional structured detail map.
- Error codes should be stable and specific enough for clients to react to.
- User-facing messages should avoid leaking technical internals.
- Developer details should include enough context for logs and debugging.
- Presentation should rely on centralized exception handling where possible.

### Current Transitional Compromise

The project currently uses `DomainException` plus context-specific error enums.

- Continue using the existing pattern unless the task is explicitly about exception architecture.
- Do not throw generic `IllegalArgumentException` or `IllegalStateException` for expected domain/application failures when a domain error exists.

## Time and ID Generation

Ideal:

- Time and ID generation should be explicit and testable.
- Prefer injecting `Clock`, ID generators, or passing generated values from application services when practical.
- Domain behavior should be deterministic in unit tests.

### Current Transitional Compromise

Some existing domain objects call `LocalDateTime.now()` and `UUID.randomUUID()` directly.

- Do not expand this pattern in new domain-heavy code.
- For small edits, keep existing constructors/factories unless changing them is part of the task.
- For new aggregates or larger refactors, introduce testable time and ID policies.

## Testing

Ideal:

- Domain entity, value object, and domain service tests are pure unit tests with no Spring context.
- Application service tests cover success and failure paths with mocked repositories, ports, and external dependencies.
- Repository and persistence behavior should be covered with focused integration tests when query behavior is non-trivial.
- Presentation tests are reserved for critical request/response, auth, and WebSocket flows.
- Use Arrange-Act-Assert structure.
- Do not delete or weaken existing tests unless explicitly requested.

## Security and Configuration

- Never hardcode secrets, connection strings, API keys, tokens, or passwords.
- Use Spring configuration, environment variables, secret managers, or local-only ignored files for environment-specific values.
- Keep authentication and authorization checks explicit at presentation/application boundaries.
- Password hashes are owned by `core/iam/identity`; token and session policies are owned by `core/iam/access`.
- Cookie, header, and request body extraction are presentation concerns. Core IAM facades should receive normalized command values instead of transport-specific objects.
- Invitation workflows should be modeled explicitly. Until then, server-generated temporary passwords are acceptable for channel admin bootstrap flows.
- Do not expose internal exception details to clients.

## Documentation

- When adding or reshaping a bounded context, update architecture documentation if present.
- Each bounded context may have `core/{bc}/docs`.
- For each significant aggregate root in a bounded context, add one short Markdown file under `core/{bc}/docs`.
- Aggregate docs should stay simple but useful: include a package tree, aggregate responsibility, owned state, key methods/factories, invariants, repository/persistence mapping, and important workflows.
- Do not document every entity or value object separately unless it has enough independent domain weight.
- Update the matching aggregate doc when a change modifies aggregate responsibility, invariants, public methods/factories, or persistence ownership.
- Keep comments focused on non-obvious business rules or technical constraints.
- New comments should be written in English.
- Avoid comment noise. Function-level comments are useful when introducing new public facades, ports, domain factories, or non-obvious workflows, but trivial private helpers do not need comments.
