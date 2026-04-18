-- Core Phase 0 foundation tables.
-- Prefixes: iam_ = IAM, cv_ = conversation.

CREATE TABLE iam_users (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    global_role VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_iam_users_email UNIQUE (email)
);

CREATE INDEX idx_iam_users_global_role ON iam_users (global_role);

CREATE TABLE iam_refresh_tokens (
    token VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_iam_refresh_tokens_user_id ON iam_refresh_tokens (user_id);
CREATE INDEX idx_iam_refresh_tokens_session_id ON iam_refresh_tokens (session_id);

CREATE TABLE iam_login_failures (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    failed_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_iam_login_failures_email ON iam_login_failures (email);
CREATE INDEX idx_iam_login_failures_ip_address ON iam_login_failures (ip_address);

CREATE TABLE cv_channels (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_cv_channels_status ON cv_channels (status);

CREATE TABLE cv_channel_memberships (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(40) NOT NULL,
    agent_status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_cv_channel_memberships_channel_user UNIQUE (channel_id, user_id)
);

CREATE INDEX idx_cv_channel_memberships_channel_id ON cv_channel_memberships (channel_id);
CREATE INDEX idx_cv_channel_memberships_user_id ON cv_channel_memberships (user_id);
