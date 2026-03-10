CREATE TABLE price_record_audits (
    id BIGSERIAL PRIMARY KEY,
    price_record_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT,
    old_price DECIMAL(12, 2),
    new_price DECIMAL(12, 2),
    note VARCHAR(500),
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_price_record FOREIGN KEY (price_record_id) REFERENCES price_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_performed_by FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE INDEX idx_audit_price_record_id ON price_record_audits(price_record_id);
CREATE INDEX idx_audit_performed_at ON price_record_audits(performed_at DESC);
