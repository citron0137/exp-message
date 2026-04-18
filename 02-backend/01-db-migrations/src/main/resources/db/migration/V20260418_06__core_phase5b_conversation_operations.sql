-- Core Phase 5B conversation operation support.

ALTER TABLE cv_channel_conversations
    ADD COLUMN assignee_membership_id VARCHAR(36) NULL;

CREATE INDEX idx_cv_channel_conversations_channel_assignee
    ON cv_channel_conversations (channel_id, assignee_membership_id);
