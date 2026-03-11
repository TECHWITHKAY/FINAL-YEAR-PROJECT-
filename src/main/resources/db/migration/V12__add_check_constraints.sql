-- V12__add_check_constraints.sql

-- 1.6  Users role CHECK
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
    CHECK (role IN ('ADMIN', 'FIELD_AGENT', 'ANALYST', 'VIEWER'));

-- 1.3  Price records status CHECK (backfill first, then constrain)
UPDATE price_records SET status = 'APPROVED' WHERE status IS NULL;

ALTER TABLE price_records
    ADD CONSTRAINT chk_price_records_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- 1.5  Market health scores range CHECKs
ALTER TABLE market_health_scores
    ADD CONSTRAINT chk_health_score    CHECK (score          BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_health_fresh    CHECK (data_freshness BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_health_stab     CHECK (price_stability BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_health_cov      CHECK (coverage       BETWEEN 0 AND 100);

-- 1.1  Missing index on reviewed_by FK in price_records
CREATE INDEX IF NOT EXISTS idx_price_records_reviewed_by ON price_records(reviewed_by);

-- 1.1  Missing index on performed_by FK in price_record_audits
CREATE INDEX IF NOT EXISTS idx_audit_performed_by ON price_record_audits(performed_by);
