-- Create price_records table for historical pricing data
CREATE TABLE price_records (
    id BIGSERIAL PRIMARY KEY,
    commodity_id BIGINT NOT NULL,
    market_id BIGINT NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK(price > 0),
    recorded_date DATE NOT NULL,
    source VARCHAR(200),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_price_records_commodity FOREIGN KEY (commodity_id) REFERENCES commodities(id),
    CONSTRAINT fk_price_records_market FOREIGN KEY (market_id) REFERENCES markets(id)
);

-- Composite index for performance on commodity/date queries
CREATE INDEX idx_price_records_commodity_date ON price_records(commodity_id, recorded_date);

-- Index for performance on market/date queries
CREATE INDEX idx_price_records_market_date ON price_records(market_id, recorded_date);
