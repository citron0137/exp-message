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
├── lastMessageSequence
├── createdAt
├── updatedAt
└── closedAt
```

## Responsibility

- Own the visitor conversation lifecycle.
- Represent the reusable open conversation for a visitor in a channel.
- Own the conversation-scoped message sequence counter.
- Provide the future parent boundary for assignment.

## Behavior

```text
ChannelConversation.start(channelId, visitorId)
ChannelConversation.canReuseForVisitorEntry()
ChannelConversation.canAcceptVisitorMessage()
ChannelConversation.canBeViewedByVisitor()
ChannelConversation.issueNextMessageSequence(now)
ChannelConversation.markOpen(now)
ChannelConversation.markDormant(now)
ChannelConversation.markClosed(now)
ChannelConversation.reactivateAsPending(now)
```

## Facade Use Cases

```text
WidgetEntryFacade.enterConversation(command)
WidgetMessageFacade.sendVisitorMessage(command)
WidgetMessageQueryService.listMessages(query)
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
- `lastMessageSequence` starts from `0`.
- The first stored message sequence is `1`.
- Visitor messages can be appended only when status is `PENDING` or `OPEN`.
- Visitor message reads are allowed when status is `PENDING`, `OPEN`, or `DORMANT`.
- Agent assignment is not part of Phase 2.

## Non-Ownership

- Visitor identity is owned by Visitor.
- Visitor session authentication is owned by VisitorSession.
- Message content and idempotency are owned by ConversationMessage.
- Realtime delivery is a future presentation delivery concern.

## Update Rules

Update this document when conversation status, assignment behavior, message ownership, table ownership, or widget entry use cases change.
