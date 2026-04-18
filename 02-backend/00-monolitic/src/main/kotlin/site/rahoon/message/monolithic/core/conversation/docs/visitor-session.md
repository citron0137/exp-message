# Visitor Session Aggregate

## Boundary

`VisitorSession` is the public widget session root inside the Conversation bounded context.

It represents the authenticated browser-side visitor session for one channel.

## Tree

```text
VisitorSession
├── id
├── visitorId
├── channelId
├── tokenHash
├── expiresAt
├── createdAt
└── lastSeenAt
```

## Responsibility

- Own visitor session token hash storage.
- Own visitor session expiry.
- Track last seen time for public widget activity.

## Behavior

```text
VisitorSession.create(visitorId, channelId, tokenHash, expiresAt)
VisitorSession.isExpired(now)
VisitorSession.touch(now)
```

## Facade Use Cases

```text
WidgetEntryFacade.createVisitorSession(command)
WidgetEntryFacade.enterConversation(command)
```

## Persistence

```text
cv_visitor_sessions
```

## Invariants

- Raw session tokens are returned only when created.
- Only deterministic token hashes are stored.
- Expired sessions cannot enter conversations.
- A session is scoped to one channel.

## Non-Ownership

- Visitor profile fields are owned by Visitor.
- Conversation lifecycle is owned by ChannelConversation.
- IAM access and refresh token policy does not apply to visitor sessions.

## Update Rules

Update this document when session token policy, expiry policy, table ownership, or widget session use cases change.
