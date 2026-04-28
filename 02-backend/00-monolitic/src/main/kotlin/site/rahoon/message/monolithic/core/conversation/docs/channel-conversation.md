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
├── assigneeMembershipId
├── lastMessageSequence
├── lastMessageAt
├── createdAt
├── updatedAt
└── closedAt
```

## Responsibility

- Own the visitor conversation lifecycle.
- Represent the reusable open conversation for a visitor in a channel.
- Own the conversation-scoped message sequence counter.
- Store the latest message timestamp for admin inbox ordering.
- Store the assigned channel membership for admin operations.

## Behavior

```text
ChannelConversation.start(channelId, visitorId)
ChannelConversation.canReuseForVisitorEntry()
ChannelConversation.canAcceptVisitorMessage()
ChannelConversation.canBeViewedByVisitor()
ChannelConversation.issueNextMessageSequence(now)
ChannelConversation.recordMessage(sequence, messageCreatedAt)
ChannelConversation.changeStatus(nextStatus, now)
ChannelConversation.assignTo(membershipId, now)
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
AdminConversationQueryService.listConversations(query)
AdminConversationQueryService.getConversation(query)
AdminConversationQueryService.listMessages(query)
AdminConversationFacade.changeStatus(command)
AdminConversationFacade.changeAssignee(command)
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
- `lastMessageAt` is updated when a stored message becomes the latest message.
- Admin inbox reads sort conversations by `COALESCE(lastMessageAt, createdAt)` and `id`.
- Admin status operations do not reopen `CLOSED` conversations.
- Admin assignment stores a `ChannelMembership` identifier, not a user identifier.
- Admin assignment is allowed only for memberships in the same channel.
- Admin inbox cursors use the activity timestamp and conversation identifier.

## Non-Ownership

- Visitor identity is owned by Visitor.
- Visitor session authentication is owned by VisitorSession.
- Message content and idempotency are owned by ConversationMessage.
- Membership role and agent availability are owned by ChannelMembership.
- Realtime delivery is a presentation delivery concern.

## Update Rules

Update this document when conversation status, assignment behavior, message ownership, table ownership, or widget entry use cases change.
