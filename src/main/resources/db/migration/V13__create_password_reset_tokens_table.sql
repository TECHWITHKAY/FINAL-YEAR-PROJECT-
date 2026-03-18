CREATE TABLE password_reset_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  DEFAULT NOW()
);

CREATE INDEX idx_prt_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_prt_user_id    ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_expires_at ON password_reset_tokens(expires_at);
