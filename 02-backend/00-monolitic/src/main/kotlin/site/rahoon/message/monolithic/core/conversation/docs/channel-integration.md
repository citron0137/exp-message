# Channel Integration Aggregate

## Boundary

`ChannelIntegration` is the external-entry integration root inside the Conversation bounded context.

It represents one way a customer channel connects to the conversation system.

## Tree

```text
ChannelIntegration
├── id
├── channelId
├── type
│   └── WIDGET
├── publicKey
├── secretHash
├── status
│   ├── ACTIVE
│   └── DISABLED
├── allowedOrigins
│   └── values
├── createdAt
└── updatedAt
```

## Responsibility

- Own widget integration identity for a channel.
- Own public key and hashed secret storage.
- Own integration lifecycle status.
- Own allowed origin policy for widget bootstrap.

## Behavior

```text
ChannelIntegration.createWidget(channelId, publicKey, secretHash, allowedOrigins)
ChannelIntegration.enable()
ChannelIntegration.disable()
ChannelIntegration.updateAllowedOrigins(allowedOrigins)
ChannelIntegration.isActive()
AllowedOrigins.of(values)
AllowedOrigins.allows(origin)
Origin.parse(rawOrigin)
```

## Facade And Query Use Cases

```text
AdminChannelIntegrationFacade.createWidgetIntegration(command)
AdminChannelIntegrationFacade.enableIntegration(command)
AdminChannelIntegrationFacade.disableIntegration(command)
AdminChannelIntegrationFacade.updateAllowedOrigins(command)
AdminChannelIntegrationQueryService.listByChannel(actor, channelId)
WidgetBootstrapQueryService.bootstrap(query)
```

## Persistence

```text
cv_channel_integrations
```

## Invariants

- `publicKey` is unique at the database level.
- Phase 1A allows only one active `WIDGET` integration per channel through application policy.
- Empty `allowedOrigins` means deny all.
- `*` allows every non-blank origin.
- Origin input is normalized to scheme, host, and optional port before comparison.
- Secret values are returned only when created and are stored only as hashes.

## Non-Ownership

- Channel lifecycle is owned by `Channel`.
- Visitor identity and conversation sessions are not owned by this aggregate in Phase 1A.

## Update Rules

Update this document when integration type, lifecycle behavior, origin policy, key policy, table ownership, or admin integration use cases change.
