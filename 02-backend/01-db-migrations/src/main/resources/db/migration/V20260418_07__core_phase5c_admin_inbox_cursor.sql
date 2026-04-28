-- Core Phase 5C admin inbox cursor and filter support.

CREATE INDEX idx_cv_channel_conversations_inbox_cursor
    ON cv_channel_conversations (
        channel_id,
        status,
        assignee_membership_id,
        last_message_at,
        created_at,
        id
    );
