-- Phase 3 schema reference for message persistence.
-- Apply this manually until the project adopts a migration runner.

ALTER TABLE cv_channel_conversations
    ADD COLUMN last_message_sequence BIGINT NOT NULL DEFAULT 0;

CREATE TABLE cv_conversation_messages (
    id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    channel_id VARCHAR(36) NOT NULL,
    sequence BIGINT NOT NULL,
    sender_type VARCHAR(40) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    client_message_id VARCHAR(100) NOT NULL,
    type VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cv_conversation_messages_sequence UNIQUE (conversation_id, sequence),
    CONSTRAINT uk_cv_conversation_messages_idempotency UNIQUE (
        conversation_id,
        sender_type,
        sender_id,
        client_message_id
    ),
    INDEX idx_cv_conversation_messages_channel_id (channel_id),
    INDEX idx_cv_conversation_messages_conversation_sequence (conversation_id, sequence)
);
