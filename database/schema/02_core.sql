-- ============================================================
-- VigilAI MedLink — Patients & Vitals
-- ============================================================

CREATE TABLE IF NOT EXISTS patients (
    patient_id      SERIAL PRIMARY KEY,
    clinic_id       VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    full_name       VARCHAR(200),
    age             INT NOT NULL CHECK (age >= 0 AND age <= 150),
    gender          VARCHAR(1) CHECK (gender IN ('M', 'F', 'O')),
    medical_history TEXT,
    age_group       VARCHAR(20) GENERATED ALWAYS AS (
                        CASE
                            WHEN age <= 0    THEN 'NEONATAL'
                            WHEN age <= 18   THEN 'PEDIATRIC'
                            ELSE 'ADULT'
                        END
                    ) STORED,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(clinic_id, phone_number)
);

CREATE INDEX idx_patients_clinic ON patients(clinic_id);
CREATE INDEX idx_patients_phone ON patients(phone_number);
CREATE INDEX idx_patients_age_group ON patients(age_group);

CREATE TABLE IF NOT EXISTS vitals (
    vital_id                 SERIAL PRIMARY KEY,
    patient_id               INT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    clinic_id                VARCHAR(100) NOT NULL,
    heart_rate               INT CHECK (heart_rate >= 0 AND heart_rate <= 300),
    temperature              DECIMAL(5,2) CHECK (temperature >= 25.0 AND temperature <= 45.0),
    respiratory_rate         INT CHECK (respiratory_rate >= 0 AND respiratory_rate <= 100),
    blood_pressure_systolic  INT CHECK (blood_pressure_systolic >= 0 AND blood_pressure_systolic <= 300),
    blood_pressure_diastolic INT CHECK (blood_pressure_diastolic >= 0 AND blood_pressure_diastolic <= 200),
    spo2                     INT CHECK (spo2 >= 0 AND spo2 <= 100),
    clinical_notes           TEXT,
    emergency_type           VARCHAR(30) CHECK (emergency_type IN (
                                 'CARDIAC','STROKE','RESPIRATORY','TRAUMA',
                                 'SEPSIS','POISONING','OBSTETRIC','DIABETIC',
                                 'SEIZURE','HEAT_STROKE','UNKNOWN')),
    vital_timestamp          TIMESTAMP NOT NULL,
    sync_status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (sync_status IN ('PENDING', 'SYNCED', 'FAILED')),
    is_encrypted             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vitals_patient ON vitals(patient_id);
CREATE INDEX idx_vitals_clinic ON vitals(clinic_id);
CREATE INDEX idx_vitals_sync ON vitals(sync_status);
CREATE INDEX idx_vitals_timestamp ON vitals(vital_timestamp DESC);

CREATE TABLE IF NOT EXISTS triage_flags (
    triage_id      SERIAL PRIMARY KEY,
    patient_id     INT NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    vital_id       INT NOT NULL REFERENCES vitals(vital_id) ON DELETE CASCADE,
    rule_severity  VARCHAR(20) NOT NULL CHECK (rule_severity IN ('PRIORITY', 'NORMAL')),
    hr_flag        BOOLEAN NOT NULL DEFAULT FALSE,
    temp_flag      BOOLEAN NOT NULL DEFAULT FALSE,
    rr_flag        BOOLEAN NOT NULL DEFAULT FALSE,
    bp_flag        BOOLEAN NOT NULL DEFAULT FALSE,
    spo2_flag      BOOLEAN NOT NULL DEFAULT FALSE,
    flag_count     INT NOT NULL DEFAULT 0,
    flagged_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_triage_patient ON triage_flags(patient_id);
CREATE INDEX idx_triage_severity ON triage_flags(rule_severity);

-- ============================================================
-- Hospitals & Doctors
-- ============================================================

CREATE TABLE IF NOT EXISTS hospitals (
    hospital_id           SERIAL PRIMARY KEY,
    name                  VARCHAR(200) NOT NULL,
    code                  VARCHAR(20) UNIQUE NOT NULL,
    latitude              DECIMAL(10,7) NOT NULL,
    longitude             DECIMAL(10,7) NOT NULL,
    total_icu_beds        INT NOT NULL DEFAULT 0,
    occupied_beds         INT NOT NULL DEFAULT 0,
    specializations       TEXT[] DEFAULT '{}',
    sepsis_mortality_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    is_level1_trauma      BOOLEAN NOT NULL DEFAULT FALSE,
    dispatcher_phone      VARCHAR(20),
    contact_email         VARCHAR(100),
    api_endpoint          VARCHAR(500),
    has_api_integration   BOOLEAN NOT NULL DEFAULT FALSE,
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hospitals_active ON hospitals(is_active) WHERE is_active = TRUE;

CREATE TABLE IF NOT EXISTS doctors (
    doctor_id    SERIAL PRIMARY KEY,
    hospital_id  INT NOT NULL REFERENCES hospitals(hospital_id),
    full_name    VARCHAR(200) NOT NULL,
    specialty    VARCHAR(100),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    shift_start  TIME,
    shift_end    TIME,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doctors_hospital ON doctors(hospital_id);

-- ============================================================
-- Alerts
-- ============================================================

CREATE TABLE IF NOT EXISTS alerts (
    alert_id             BIGSERIAL PRIMARY KEY,
    patient_id           INT REFERENCES patients(patient_id),
    clinic_id            VARCHAR(100) NOT NULL,
    risk_score           DECIMAL(5,4) NOT NULL CHECK (risk_score >= 0 AND risk_score <= 1),
    severity             VARCHAR(30) NOT NULL,
    risk_level           VARCHAR(20),
    emergency_type       VARCHAR(30),
    top_features         TEXT[] DEFAULT '{}',
    confidence           DECIMAL(5,4),
    model_version        VARCHAR(50),
    llm_explanation      TEXT,
    treatment_recs       TEXT,
    paramedic_guidance   TEXT,
    heart_rate           INT,
    temperature          DECIMAL(5,2),
    respiratory_rate     INT,
    bp_systolic          INT,
    bp_diastolic         INT,
    spo2                 INT,
    patient_age          INT,
    status               VARCHAR(20) DEFAULT 'NEW',
    clinician_decision   VARCHAR(20) DEFAULT 'PENDING'
                         CHECK (clinician_decision IN ('APPROVED','HOLD','PENDING','DISMISSED')),
    clinician_id         VARCHAR(100),
    hold_reason          TEXT,
    notes                TEXT,
    decision_at          TIMESTAMP,
    dispatch_status      VARCHAR(30) DEFAULT 'PENDING',
    hospital_id          INT REFERENCES hospitals(hospital_id),
    dispatched_at        TIMESTAMP,
    clinic_latitude      DECIMAL(10,7),
    clinic_longitude     DECIMAL(10,7),
    alert_timestamp      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_patient ON alerts(patient_id);
CREATE INDEX idx_alerts_clinic ON alerts(clinic_id);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_dispatch ON alerts(dispatch_status);
CREATE INDEX idx_alerts_time ON alerts(alert_timestamp DESC);

-- ============================================================
-- Documents
-- ============================================================

CREATE TABLE IF NOT EXISTS documents (
    doc_id         SERIAL PRIMARY KEY,
    patient_id     INT REFERENCES patients(patient_id),
    uploaded_by    UUID REFERENCES users(id),
    doc_type       VARCHAR(50),
    file_name      VARCHAR(255),
    encrypted_data TEXT,
    encryption_key VARCHAR(500),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Outcomes (for model retraining)
-- ============================================================

CREATE TABLE IF NOT EXISTS outcomes (
    outcome_id     SERIAL PRIMARY KEY,
    alert_id       BIGINT REFERENCES alerts(alert_id),
    was_sepsis     BOOLEAN,
    final_diagnosis VARCHAR(200),
    patient_survived BOOLEAN,
    data_source    VARCHAR(100),
    confirmed_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Audit Log (WORM)
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_log_worm (
    log_id        BIGSERIAL PRIMARY KEY,
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(50) NOT NULL,
    entity_id     VARCHAR(100) NOT NULL,
    user_id       VARCHAR(100),
    old_value     TEXT,
    new_value     TEXT,
    timestamp     TIMESTAMP NOT NULL DEFAULT NOW(),
    hash_previous VARCHAR(256),
    hash_current  VARCHAR(256) NOT NULL,
    signature     VARCHAR(512),
    immutable     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_log_worm(entity_type, entity_id);
CREATE INDEX idx_audit_time ON audit_log_worm(timestamp DESC);

-- ============================================================
-- Seed Data
-- ============================================================

INSERT INTO hospitals (name, code, latitude, longitude, total_icu_beds, occupied_beds,
                       sepsis_mortality_rate, is_level1_trauma, dispatcher_phone,
                       contact_email, api_endpoint, has_api_integration, specializations) VALUES
    ('Bangalore Medical College & RI', 'BMCRI', 12.9600, 77.5725, 40, 28,
     12.5, TRUE, '+919000000001', 'dispatch@bmcri.org', 'https://api.bmcri.org/dispatch', TRUE,
     ARRAY['SEPSIS','CARDIAC','TRAUMA','STROKE']),
    ('St. John''s Medical College', 'SJMC', 12.9279, 77.6271, 25, 18,
     15.2, FALSE, '+919000000002', 'icu@stjohns.org', NULL, FALSE,
     ARRAY['SEPSIS','OBSTETRIC','PEDIATRIC']),
    ('Mysore Medical College', 'MMC', 12.3051, 76.6551, 20, 12,
     18.0, FALSE, '+919000000003', 'dispatch@mmc.org', 'https://api.mmc.org/dispatch', TRUE,
     ARRAY['CARDIAC','RESPIRATORY']),
    ('Manipal Hospital Whitefield', 'MHW', 12.9698, 77.7500, 50, 35,
     9.8, TRUE, '+919000000004', 'icu@manipal.org', 'https://api.manipal.org/dispatch', TRUE,
     ARRAY['SEPSIS','CARDIAC','TRAUMA','STROKE','NEUROLOGY'])
ON CONFLICT (code) DO NOTHING;

INSERT INTO doctors (hospital_id, full_name, specialty, is_available) VALUES
    (1, 'Dr. Rajesh Kumar', 'Critical Care / Sepsis', TRUE),
    (1, 'Dr. Priya Sharma', 'Emergency Medicine', TRUE),
    (2, 'Dr. Ahmed Khan', 'Pediatric ICU', TRUE),
    (3, 'Dr. Kavita Nair', 'Pulmonology', FALSE),
    (4, 'Dr. Srinivas Rao', 'Cardiology', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO patients (clinic_id, phone_number, full_name, age, gender, medical_history) VALUES
    ('clinic-demo-001', '+919876543210', 'Ramesh Patil',  45, 'M', 'Type 2 Diabetes, Hypertension'),
    ('clinic-demo-001', '+919876543211', 'Anjali Devi',   28, 'F', 'No significant history'),
    ('clinic-demo-001', '+919876543212', 'Govind Rao',    67, 'M', 'COPD, Former smoker'),
    ('clinic-demo-002', '+919876543213', 'Baby Fatima',    3, 'F', 'Premature birth at 34 weeks'),
    ('clinic-demo-002', '+919876543214', 'Suresh Mehta',  55, 'M', 'Post-cardiac bypass (2024)'),
    ('clinic-demo-003', '+919876543215', 'Lakshmi Bai',   72, 'F', 'Chronic kidney disease stage 3')
ON CONFLICT (clinic_id, phone_number) DO NOTHING;
