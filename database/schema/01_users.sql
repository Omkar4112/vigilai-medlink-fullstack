-- ============================================================
-- VigilAI MedLink — Users & Auth Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL CHECK (role IN ('CLINIC', 'HOSPITAL', 'ADMIN')),
    entity_id     VARCHAR(100),  -- clinic_id or hospital_id based on role
    full_name     VARCHAR(200),
    phone         VARCHAR(20),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    last_login    TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Seed admin user (password: Admin@123)
INSERT INTO users (email, password_hash, role, full_name) VALUES
('admin@vigilai.health', '$2a$12$XrHoZRJVJvXbIZ1/kKg.Zug8S/Nx6WO40MfkLaIMtSnJzYMWlFBka', 'ADMIN', 'System Administrator')
ON CONFLICT (email) DO NOTHING;

-- Seed clinic user (password: Clinic@123)
INSERT INTO users (email, password_hash, role, full_name, entity_id) VALUES
('clinic@vigilai.health', '$2a$12$Q3ZO/62p8o966iGmwE6e8.nFfbzR0WryxXpTxzraSIZzLDjpAfHpW', 'CLINIC', 'Demo Clinic PHC', 'a1b2c3d4-e5f6-7890-abcd-000000000001')
ON CONFLICT (email) DO NOTHING;

-- Seed hospital user (password: Hospital@123)
INSERT INTO users (email, password_hash, role, full_name, entity_id) VALUES
('hospital@vigilai.health', '$2a$12$b2iyPEQRapbuADGCzgdOIeWqjuyPg1tTq.4tOGzLBVwYI1IYRJJ9i', 'HOSPITAL', 'BMCRI Hospital', '1')
ON CONFLICT (email) DO NOTHING;
