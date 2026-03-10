ALTER TABLE price_records
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
ADD COLUMN submitted_by BIGINT,
ADD COLUMN reviewed_by BIGINT,
ADD COLUMN reviewed_at TIMESTAMPTZ,
ADD COLUMN rejection_reason VARCHAR(500);

ALTER TABLE price_records
ADD CONSTRAINT fk_price_records_submitted_by FOREIGN KEY (submitted_by) REFERENCES users(id),
ADD CONSTRAINT fk_price_records_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id);

CREATE INDEX idx_price_records_status ON price_records(status);
CREATE INDEX idx_price_records_submitted_by ON price_records(submitted_by);
