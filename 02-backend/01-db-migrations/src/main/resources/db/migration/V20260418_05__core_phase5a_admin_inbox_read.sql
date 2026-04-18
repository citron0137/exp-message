-- Core Phase 5A admin inbox read support.

ALTER TABLE cv_channel_conversations
    ADD COLUMN last_message_at DATETIME(6) NULL;

CREATE INDEX idx_cv_channel_conversations_channel_status_last_message_at
    ON cv_channel_conversations (channel_id, status, last_message_at);
