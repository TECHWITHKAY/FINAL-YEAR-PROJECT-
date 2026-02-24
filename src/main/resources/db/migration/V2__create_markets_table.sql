-- Create markets table linked to cities
CREATE TABLE markets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    city_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_markets_city FOREIGN KEY (city_id) REFERENCES cities(id)
);

-- Index for faster market lookups by city
CREATE INDEX idx_markets_city_id ON markets(city_id);
