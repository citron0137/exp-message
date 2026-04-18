# ChannelMembership Aggregate

## Boundary

`ChannelMembership` owns channel-scoped backoffice authorization inside the Conversation bounded context.

It links an IAM user to a Conversation channel.

## Tree

```text
ChannelMembership
├── id
├── channelId
├── userId
├── role
│   ├── CHANNEL_ADMIN
│   └── AGENT
├── agentStatus
│   ├── ONLINE
│   ├── AWAY
│   └── OFFLINE
├── createdAt
└── updatedAt
```

## Responsibility

- Represent channel membership.
- Store channel-scoped role.
- Store agent availability status.
- Provide the authorization base for future inbox, reply, and member management features.

## Behavior

```text
ChannelMembership.createChannelAdmin(channelId, userId)
```

## Facade Use Cases

```text
AdminChannelFacade.createChannel(command)
```

## Persistence

```text
cv_channel_memberships
```

## Non-Ownership

- Global role is owned by IAM IdentityUser.
- Platform admin authorization is resolved from IAM principal data.
- Conversation assignment will reference membership but is not owned by this aggregate.

## Update Rules

Update this document when membership state, role behavior, status behavior, table ownership, or member management use cases change.
