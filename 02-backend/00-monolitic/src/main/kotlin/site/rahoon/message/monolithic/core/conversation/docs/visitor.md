# Visitor Aggregate

## Boundary

`Visitor` is the public widget participant root inside the Conversation bounded context.

It represents an anonymous or externally identified visitor for one customer channel.

## Tree

```text
Visitor
├── id
├── channelId
├── externalId
├── displayName
├── email
├── metadata
├── createdAt
└── updatedAt
```

## Responsibility

- Own visitor identity inside a channel.
- Store optional external visitor information supplied by the widget client.
- Store small string metadata for future routing and CRM-style enrichment.

## Behavior

```text
Visitor.create(channelId, externalId, displayName, email, metadata)
```

## Facade Use Cases

```text
WidgetEntryFacade.createVisitorSession(command)
WidgetEntryFacade.enterConversation(command)
```

## Persistence

```text
cv_visitors
```

## Invariants

- Visitors always belong to one channel.
- Anonymous visitors are persisted.
- Metadata starts as `Map<String, String>`.
- Blank metadata keys are ignored.

## Non-Ownership

- Visitor session token lifecycle is owned by VisitorSession.
- Conversation lifecycle is owned by ChannelConversation.
- IAM users are not visitor identities.

## Update Rules

Update this document when visitor identity fields, metadata rules, table ownership, or widget visitor use cases change.
