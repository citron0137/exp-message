# Channel Conversation Aggregate

## Boundary

`ChannelConversation` is the visitor conversation root inside the Conversation bounded context.

It represents one conversation between a visitor and a customer channel.

## Tree

```text
ChannelConversation
├── id
├── channelId
├── visitorId
├── status
│   ├── PENDING
│   ├── OPEN
│   ├── DORMANT
│   └── CLOSED
├── createdAt
├── updatedAt
└── closedAt
```

## Responsibility

- Own the visitor conversation lifecycle.
- Represent the reusable open conversation for a visitor in a channel.
- Provide the future parent boundary for messages and assignment.

## Behavior

```text
ChannelConversation.start(channelId, visitorId)
ChannelConversation.canReuseForVisitorEntry()
ChannelConversation.canAcceptVisitorMessage()
ChannelConversation.canBeViewedByVisitor()
ChannelConversation.markOpen(now)
ChannelConversation.markDormant(now)
ChannelConversation.markClosed(now)
ChannelConversation.reactivateAsPending(now)
```

## Facade Use Cases

```text
WidgetEntryFacade.enterConversation(command)
```

## Persistence

```text
cv_channel_conversations
```

## Invariants

- New widget conversations start as `PENDING`.
- Visitor entry reuses `PENDING`, `OPEN`, and `DORMANT` conversations.
- Visitor entry does not reuse `CLOSED` conversations.
- `DORMANT` conversations reactivate as `PENDING` on visitor entry.
- `CLOSED` conversations are not visible to visitors.
- Message storage is not part of Phase 2.
- Agent assignment is not part of Phase 2.

## Non-Ownership

- Visitor identity is owned by Visitor.
- Visitor session authentication is owned by VisitorSession.
- Message persistence and realtime delivery are future aggregates or services.

## Update Rules

Update this document when conversation status, assignment behavior, message ownership, table ownership, or widget entry use cases change.
