# IdentityUser Aggregate

## Boundary

`IdentityUser` owns backoffice user identity inside the IAM bounded context.

It represents platform admins and customer-company backoffice users.

It does not represent widget end users.

## Tree

```text
IdentityUser
├── id
├── email
├── passwordHash
├── nickname
├── globalRole
│   ├── PLATFORM_ADMIN
│   └── CHANNEL_USER
├── createdAt
└── updatedAt
```

## Responsibility

- Store the login identity for backoffice users.
- Store the global platform role.
- Store the password hash for Phase 0.
- Provide the identity source used by IAM access login.

## Behavior

```text
IdentityUser.create(email, passwordHash, nickname, globalRole)
```

## Facade Use Cases

```text
IdentityFacade.createPlatformAdminIfAbsent(command)
IdentityFacade.createOrLoadCustomerAdmin(command)
```

## Persistence

```text
iam_users
```

## Non-Ownership

- Channel-scoped role is not stored here.
- Channel membership is owned by Conversation.
- Widget end user identity is owned by Conversation.

## Update Rules

Update this document when `IdentityUser` state, behavior, table ownership, or public identity facade use cases change.
