# LoginFailure Aggregate

## Boundary

`LoginFailure` records failed login attempts inside IAM access.

It is prepared in Phase 0 and will become active when login failure policy is connected.

## Tree

```text
LoginFailure
├── id
├── email
├── ipAddress
└── failedAt
```

## Responsibility

- Record failed login attempts.
- Provide the data source for future brute-force protection.
- Support future lockout and throttling policy.

## Behavior

```text
No domain behavior is connected yet.
```

## Planned Facade Use Cases

```text
AccessFacade.login(command)
```

## Persistence

```text
iam_login_failures
```

## Non-Ownership

- Password verification is owned by IAM access services.
- User identity is owned by IdentityUser.
- HTTP request parsing is owned by Presentation.

## Update Rules

Update this document when login failure policy, lockout rules, or persistence behavior are implemented.
