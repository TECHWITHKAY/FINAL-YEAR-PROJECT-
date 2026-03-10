-- Seed Initial Cities (Major regions of Ghana)
INSERT INTO cities (name, region) VALUES 
('Accra', 'Greater Accra'),
('Kumasi', 'Ashanti'),
('Tamale', 'Northern'),
('Takoradi', 'Western'),
('Cape Coast', 'Central');

-- Seed Initial Markets (2 per city)
INSERT INTO markets (name, city_id) VALUES 
('Makola Market', (SELECT id FROM cities WHERE name = 'Accra')),
('Madina Market', (SELECT id FROM cities WHERE name = 'Accra')),
('Kejetia Market', (SELECT id FROM cities WHERE name = 'Kumasi')),
('Bantama Market', (SELECT id FROM cities WHERE name = 'Kumasi')),
('Tamale Central Market', (SELECT id FROM cities WHERE name = 'Tamale')),
('Aboabo Market', (SELECT id FROM cities WHERE name = 'Tamale')),
('Market Circle', (SELECT id FROM cities WHERE name = 'Takoradi')),
('Kojo-Krom Market', (SELECT id FROM cities WHERE name = 'Takoradi')),
('Kotokuraba Market', (SELECT id FROM cities WHERE name = 'Cape Coast')),
('Abura Market', (SELECT id FROM cities WHERE name = 'Cape Coast'));

-- Seed Core Commodities
INSERT INTO commodities (name, category, unit) VALUES 
('Maize', 'Grains', 'kg'),
('Rice', 'Grains', 'bag'),
('Tomato', 'Vegetables', 'crate'),
('Yam', 'Tubers', 'bag'),
('Plantain', 'Fruits', 'bunch'),
('Groundnut', 'Legumes', 'kg');

-- Default admin is now handled by com.ghana.commoditymonitor.util.DataSeeder 
-- using properties from application.yaml
