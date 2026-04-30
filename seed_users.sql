CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
ALTER TABLE users ALTER COLUMN id SET DEFAULT uuid_generate_v4();
INSERT INTO users (email, password, role, full_name, entity_id, is_active) VALUES 
('admin@vigilai.health', '$2a$12$XrHoZRJVJvXbIZ1/kKg.Zug8S/Nx6WO40MfkLaIMtSnJzYMWlFBka', 'ADMIN', 'System Administrator', NULL, true),
('clinic@vigilai.health', '$2a$12$Q3ZO/62p8o966iGmwE6e8.nFfbzR0WryxXpTxzraSIZzLDjpAfHpW', 'CLINIC', 'Demo Clinic PHC', 'clinic-demo-001', true),
('hospital@vigilai.health', '$2a$12$b2iyPEQRapbuADGCzgdOIeWqjuyPg1tTq.4tOGzLBVwYI1IYRJJ9i', 'HOSPITAL', 'BMCRI Hospital', '1', true);
