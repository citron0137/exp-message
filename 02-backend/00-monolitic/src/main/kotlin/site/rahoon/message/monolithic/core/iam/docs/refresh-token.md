# RefreshToken Aggregate

## Boundary

`RefreshToken` owns stateful session renewal inside the IAM access area.

It is part of the IAM bounded context.

## Tree

```text
RefreshToken
├── token
├── userId
├── sessionId
├── expiresAt
└── createdAt
```

## Responsibility

- Store refresh token state.
- Bind a refresh token to a user and session.
- Decide whether the refresh token is expired.
- Support refresh token rotation during session refresh.

## Behavior

```text
CoreRefreshToken.isExpired(now)
```

## Facade Use Cases

```text
AccessFacade.login(command)
AccessFacade.refresh(command)
AccessFacade.logout(principal)
```

## Persistence

```text
iam_refresh_tokens
```

## Non-Ownership

- Access token JWT serialization is not owned by this aggregate.
- HTTP cookie transport is owned by Presentation.
- User profile data is owned by IdentityUser.

## Update Rules

Update this document when refresh token state, rotation behavior, persistence ownership, or access facade session use cases change.
