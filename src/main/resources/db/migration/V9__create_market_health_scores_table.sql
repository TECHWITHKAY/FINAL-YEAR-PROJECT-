CREATE TABLE market_health_scores (
    id BIGSERIAL PRIMARY KEY,
    market_id BIGINT NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    data_freshness DECIMAL(5, 2) NOT NULL,
    price_stability DECIMAL(5, 2) NOT NULL,
    coverage DECIMAL(5, 2) NOT NULL,
    grade VARCHAR(1) NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_health_market FOREIGN KEY (market_id) REFERENCES markets(id) ON DELETE CASCADE
);

CREATE INDEX idx_health_market_id ON market_health_scores(market_id);
CREATE INDEX idx_health_computed_at ON market_health_scores(computed_at DESC);
CREATE INDEX idx_health_score ON market_health_scores(score DESC);
CREATE INDEX idx_health_grade ON market_health_scores(grade);
