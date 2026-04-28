-- Core Phase 1A channel integration table.

CREATE TABLE cv_channel_integrations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    type VARCHAR(40) NOT NULL,
    public_key VARCHAR(100) NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    allowed_origins JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_cv_channel_integrations_public_key UNIQUE (public_key)
);

CREATE INDEX idx_cv_channel_integrations_channel_id ON cv_channel_integrations (channel_id);
CREATE INDEX idx_cv_channel_integrations_channel_type_status ON cv_channel_integrations (channel_id, type, status);
CREATE INDEX idx_cv_channel_integrations_status ON cv_channel_integrations (status);
