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
├── status
│   ├── ACTIVE
│   └── DISABLED
├── createdAt
└── updatedAt
```

## Responsibility

- Represent channel membership.
- Store channel-scoped role.
- Store agent availability status.
- Store membership management status.
- Provide the authorization base for future inbox, reply, and member management features.

## Behavior

```text
ChannelMembership.createChannelAdmin(channelId, userId)
ChannelMembership.create(channelId, userId, role)
ChannelMembership.canBeAssigned()
ChannelMembership.changeRole(role, now)
ChannelMembership.enable(now)
ChannelMembership.disable(now)
```

## Facade Use Cases

```text
AdminChannelFacade.createChannel(command)
AdminChannelMembershipFacade.createMembership(command)
AdminChannelMembershipFacade.changeRole(command)
AdminChannelMembershipFacade.enable(command)
AdminChannelMembershipFacade.disable(command)
AdminChannelMembershipQueryService.listByChannel(actor, channelId)
```

## Persistence

```text
cv_channel_memberships
```

## Non-Ownership

- Global role is owned by IAM IdentityUser.
- Platform admin authorization is resolved from IAM principal data.
- Conversation assignment references membership but is not owned by this aggregate.
- Temporary password generation is owned by IAM.

## Invariants

- A channel user can have only one membership per channel.
- New memberships start as `ACTIVE`.
- New memberships start with `AgentStatus.OFFLINE`.
- `DISABLED` memberships cannot be assigned to conversations.
- `CHANNEL_ADMIN` can create `AGENT` memberships in the same channel.
- Only `PLATFORM_ADMIN` can create `CHANNEL_ADMIN` memberships.
- Only `PLATFORM_ADMIN` can change membership roles.
- `CHANNEL_ADMIN` can enable or disable `AGENT` memberships in the same channel.
- `CHANNEL_ADMIN` cannot enable or disable `CHANNEL_ADMIN` memberships.
- The last active `CHANNEL_ADMIN` in a channel cannot be disabled or demoted.
- `CHANNEL_ADMIN` cannot disable their own membership.

## Update Rules

Update this document when membership state, role behavior, status behavior, table ownership, or member management use cases change.
