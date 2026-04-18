# Channel Aggregate

## Boundary

`Channel` is the tenant root inside the Conversation bounded context.

It represents a customer-company channel.

## Tree

```text
Channel
├── id
├── name
├── status
│   ├── INACTIVE
│   ├── ACTIVE
│   ├── SUSPENDED
│   └── ARCHIVED
├── createdAt
└── updatedAt
```

## Responsibility

- Own the tenant boundary.
- Own the channel lifecycle status.
- Act as the parent boundary for channel membership and future conversation resources.

## Behavior

```text
Channel.create(name)
```

## Facade Use Cases

```text
AdminChannelFacade.createChannel(command)
AdminChannelFacade.listChannels(actor)
AdminChannelFacade.getChannel(actor, channelId)
```

## Persistence

```text
cv_channels
```

## Non-Ownership

- Widget integration credentials are not stored on Channel in Phase 0.
- Channel-scoped user role is owned by ChannelMembership.
- Backoffice identity is owned by IAM.

## Update Rules

Update this document when Channel state, lifecycle behavior, table ownership, or admin channel facade use cases change.
