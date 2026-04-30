
-- ============================================================
-- Sync Seed Data for JPA-Generated Schema
-- ============================================================

-- 1. Insert Hospitals
INSERT INTO hospitals (name, code, latitude, longitude, total_icu_beds, occupied_beds, sepsis_mortality_rate, is_level1_trauma, dispatcher_phone, contact_email, api_endpoint, has_api_integration, is_active)
VALUES 
('Bangalore Medical College & RI', 'BMCRI', 12.9600, 77.5725, 40, 28, 12.5, TRUE, '+919000000001', 'dispatch@bmcri.org', 'https://api.bmcri.org/dispatch', TRUE, TRUE),
('St. Johns Medical College', 'SJMC', 12.9279, 77.6271, 25, 18, 15.2, FALSE, '+919000000002', 'icu@stjohns.org', NULL, FALSE, TRUE),
('Mysore Medical College', 'MMC', 12.3051, 76.6551, 20, 12, 18.0, FALSE, '+919000000003', 'dispatch@mmc.org', 'https://api.mmc.org/dispatch', TRUE, TRUE),
('Manipal Hospital Whitefield', 'MHW', 12.9698, 77.7500, 50, 35, 9.8, TRUE, '+919000000004', 'icu@manipal.org', 'https://api.manipal.org/dispatch', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- 2. Insert Hospital Specializations (Separate table created by @ElementCollection)
INSERT INTO hospital_specializations (hospital_id, specialization)
SELECT hospital_id, unnest(ARRAY['SEPSIS','CARDIAC','TRAUMA','STROKE']) FROM hospitals WHERE code = 'BMCRI'
UNION ALL
SELECT hospital_id, unnest(ARRAY['SEPSIS','OBSTETRIC','PEDIATRIC']) FROM hospitals WHERE code = 'SJMC'
UNION ALL
SELECT hospital_id, unnest(ARRAY['CARDIAC','RESPIRATORY']) FROM hospitals WHERE code = 'MMC'
UNION ALL
SELECT hospital_id, unnest(ARRAY['SEPSIS','CARDIAC','TRAUMA','STROKE','NEUROLOGY']) FROM hospitals WHERE code = 'MHW';

-- 3. Insert Doctors
INSERT INTO doctors (hospital_id, full_name, specialty, is_available)
SELECT hospital_id, 'Dr. Rajesh Kumar', 'Critical Care / Sepsis', TRUE FROM hospitals WHERE code = 'BMCRI'
UNION ALL
SELECT hospital_id, 'Dr. Priya Sharma', 'Emergency Medicine', TRUE FROM hospitals WHERE code = 'BMCRI'
UNION ALL
SELECT hospital_id, 'Dr. Ahmed Khan', 'Pediatric ICU', TRUE FROM hospitals WHERE code = 'SJMC'
UNION ALL
SELECT hospital_id, 'Dr. Kavita Nair', 'Pulmonology', FALSE FROM hospitals WHERE code = 'MMC'
UNION ALL
SELECT hospital_id, 'Dr. Srinivas Rao', 'Cardiology', TRUE FROM hospitals WHERE code = 'MHW';

-- 4. Insert Patients
INSERT INTO patients (clinic_id, phone_number, full_name, age, gender, medical_history)
VALUES 
('clinic-demo-001', '+919876543210', 'Ramesh Patil',  45, 'M', 'Type 2 Diabetes, Hypertension'),
('clinic-demo-001', '+919876543211', 'Anjali Devi',   28, 'F', 'No significant history'),
('clinic-demo-001', '+919876543212', 'Govind Rao',    67, 'M', 'COPD, Former smoker'),
('clinic-demo-002', '+919876543213', 'Baby Fatima',    3, 'F', 'Premature birth at 34 weeks')
ON CONFLICT (clinic_id, phone_number) DO NOTHING;
