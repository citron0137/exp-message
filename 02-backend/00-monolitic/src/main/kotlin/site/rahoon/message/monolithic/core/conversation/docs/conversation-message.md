# Conversation Message Aggregate

## Boundary

`ConversationMessage` is the canonical message log root inside the Conversation bounded context.

It represents one persisted message in a channel conversation.

## Tree

```text
ConversationMessage
├── id
├── conversationId
├── channelId
├── sequence
├── senderType
│   ├── VISITOR
│   ├── AGENT
│   └── SYSTEM
├── senderId
├── clientMessageId
├── type
│   └── TEXT
├── content
│   └── MessageContent
├── status
│   └── VISIBLE
└── createdAt
```

## Responsibility

- Store the canonical conversation message history.
- Preserve conversation-scoped message ordering with `sequence`.
- Provide idempotent visitor message append with `clientMessageId`.
- Provide idempotent agent reply append with `clientMessageId`.
- Keep message content validation in `MessageContent`.

## Behavior

```text
ConversationMessage.visitorText(conversationId, channelId, visitorId, sequence, clientMessageId, content)
ConversationMessage.agentText(conversationId, channelId, membershipId, sequence, clientMessageId, content)
MessageContent.text(rawValue)
```

## Facade Use Cases

```text
WidgetMessageFacade.sendVisitorMessage(command)
AdminConversationFacade.sendReply(command)
WidgetMessageQueryService.listMessages(query)
```

## Persistence

```text
cv_conversation_messages
```

Required uniqueness:

```text
conversation_id + sequence
conversation_id + sender_type + sender_id + client_message_id
```

Required read index:

```text
conversation_id + sequence
```

## Invariants

- Message ids are UUID strings.
- Visitor message content is trimmed before persistence.
- Blank message content is rejected.
- Message content length must not exceed 4000 characters.
- Phase 3 stores only `TEXT` messages.
- Phase 3 stores only `VISIBLE` messages.
- Duplicate visitor append returns the existing message for the idempotency key.
- Duplicate agent reply append returns the existing message for the idempotency key.
- Agent reply sender id is the channel membership id, not the IAM user id.
- Platform admin authority alone cannot create an agent reply.
- A platform admin must have an active channel membership to reply as a channel operator.
- Disabled memberships cannot create new agent replies.
- Replies to `PENDING`, `OPEN`, and `DORMANT` conversations are allowed.
- Sending an agent reply moves the conversation to `OPEN`.
- Replies to `CLOSED` conversations are rejected.
- HTTP and later WebSocket delivery should reuse the same write facade.

## Non-Ownership

- Conversation lifecycle is owned by ChannelConversation.
- Visitor session authentication is owned by VisitorSession.
- Realtime delivery is not owned by ConversationMessage.
- Read receipts and delivery receipts are future concerns.

## Update Rules

Update this document when message type, sender type, message status, idempotency, ordering, or table ownership changes.
