CREATE TABLE seasonal_patterns (
    id BIGSERIAL PRIMARY KEY,
    commodity_id BIGINT NOT NULL,
    month_of_year SMALLINT NOT NULL CHECK (month_of_year BETWEEN 1 AND 12),
    seasonal_index DECIMAL(6, 4) NOT NULL,
    avg_price DECIMAL(12, 2) NOT NULL,
    data_year_from SMALLINT NOT NULL,
    data_year_to SMALLINT NOT NULL,
    sample_size INTEGER NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_seasonal_commodity FOREIGN KEY (commodity_id) REFERENCES commodities(id) ON DELETE CASCADE,
    CONSTRAINT uq_seasonal_commodity_month UNIQUE (commodity_id, month_of_year)
);

CREATE INDEX idx_seasonal_commodity_id ON seasonal_patterns(commodity_id);
CREATE INDEX idx_seasonal_month ON seasonal_patterns(month_of_year);
CREATE INDEX idx_seasonal_index ON seasonal_patterns(seasonal_index);
CREATE INDEX idx_seasonal_computed_at ON seasonal_patterns(computed_at DESC);
