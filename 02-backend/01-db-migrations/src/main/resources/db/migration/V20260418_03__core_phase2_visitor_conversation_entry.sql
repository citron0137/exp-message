-- Core Phase 2 visitor session and conversation entry tables.

CREATE TABLE cv_visitors (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    external_id VARCHAR(100) NULL,
    display_name VARCHAR(100) NULL,
    email VARCHAR(255) NULL,
    metadata JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_cv_visitors_channel_id ON cv_visitors (channel_id);
CREATE INDEX idx_cv_visitors_external_id ON cv_visitors (external_id);

CREATE TABLE cv_visitor_sessions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    visitor_id VARCHAR(36) NOT NULL,
    channel_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_seen_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_cv_visitor_sessions_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_cv_visitor_sessions_visitor_id ON cv_visitor_sessions (visitor_id);
CREATE INDEX idx_cv_visitor_sessions_channel_id ON cv_visitor_sessions (channel_id);
CREATE INDEX idx_cv_visitor_sessions_expires_at ON cv_visitor_sessions (expires_at);

CREATE TABLE cv_channel_conversations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    visitor_id VARCHAR(36) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    closed_at DATETIME(6) NULL
);

CREATE INDEX idx_cv_channel_conversations_channel_id ON cv_channel_conversations (channel_id);
CREATE INDEX idx_cv_channel_conversations_visitor_id ON cv_channel_conversations (visitor_id);
CREATE INDEX idx_cv_channel_conversations_status ON cv_channel_conversations (status);
CREATE INDEX idx_cv_channel_conversations_channel_status ON cv_channel_conversations (channel_id, status);
