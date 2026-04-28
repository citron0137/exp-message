-- Core Phase 6A channel membership management support.

ALTER TABLE cv_channel_memberships
    ADD COLUMN status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_cv_channel_memberships_channel_status
    ON cv_channel_memberships (channel_id, status);
