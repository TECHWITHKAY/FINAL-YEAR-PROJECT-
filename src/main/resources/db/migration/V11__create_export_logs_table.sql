CREATE TABLE export_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    export_type VARCHAR(20) NOT NULL,
    filters TEXT,
    row_count INTEGER NOT NULL,
    file_size BIGINT NOT NULL,
    ip_address VARCHAR(45),
    exported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_export_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_export_user_id ON export_logs(user_id);
CREATE INDEX idx_export_exported_at ON export_logs(exported_at DESC);
CREATE INDEX idx_export_type ON export_logs(export_type);
