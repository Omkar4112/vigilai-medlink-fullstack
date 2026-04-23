# VigilAI MedLink v2.0 — Production Setup Guide

## Architecture
```
Browser (login.html / clinic.html / hospital.html / admin.html)
    │  HTTP + WebSocket (JWT)
    ▼
Backend (Spring Boot :8080)  ←→  AI Service (FastAPI :8000)
    │  JDBC
    ▼
PostgreSQL (:5432)
```

## Quick Start (Docker — recommended)
```bash
cd vigilai-complete
docker compose up --build
```
- Frontend:  http://localhost:3000/login.html
- Backend:   http://localhost:8080/health
- AI:        http://localhost:8000/health

## Manual Start

### 1. PostgreSQL
```bash
psql -U postgres
CREATE DATABASE vigilai;
\c vigilai
\i database/schema/01_users.sql
\i database/schema/02_core.sql
```

### 2. AI Service
```bash
cd ai-service
pip install -r requirements.txt
python -c "from app.inference.xgboost_model import train; train()"
uvicorn app.main:app --reload --port 8000
```

### 3. Backend
```bash
cd backend
mvn spring-boot:run
```

### 4. Frontend
Open `frontend/login.html` in browser (or serve with nginx/live-server).

## Demo Credentials
| Role     | Email                     | Password     |
|----------|---------------------------|--------------|
| CLINIC   | clinic@vigilai.health     | Clinic@123   |
| HOSPITAL | hospital@vigilai.health   | Hospital@123 |
| ADMIN    | admin@vigilai.health      | Admin@123    |

## Bug Fixes Applied (from code review)
| # | File                  | Bug                                         | Fix                              |
|---|-----------------------|---------------------------------------------|----------------------------------|
| 1 | VitalController.java  | UUID set on Long alertId; .patientId() DNE  | Removed ID; used .patient(obj)   |
| 2 | application.yml       | jackson nested under `ai:` not `spring:`    | Moved under `spring.jackson`     |
| 3 | application.yml       | Dead `vigilai.ai-service.url` on port 8001  | Removed; single `ai.service.url` |
| 4 | HospitalRepository    | JpaRepository<Hospital, Integer>            | Changed to Long                  |
| 5 | MedLinkService        | .multiply() called on Double getRiskScore() | Used double arithmetic           |
| 6 | AlertService          | Long alertId passed as String entityId      | Wrapped with String.valueOf()    |

## API Reference

### Auth
- `POST /auth/login`     — { email, password } → { token, role, entityId }
- `POST /auth/register`  — { email, password, role, fullName, entityId }
- `GET  /auth/me`        — Returns current user from JWT

### Clinic (requires CLINIC/ADMIN role)
- `POST /api/clinic/vitals`                — Submit vitals → AI prediction → alert
- `GET  /api/clinic/vitals/patient/{id}`   — Patient vital history
- `GET  /api/clinic/patients?clinicId=`    — List clinic patients
- `GET  /api/clinic/alerts?clinicId=`      — Clinic alert history

### Hospital (requires HOSPITAL/ADMIN role)
- `GET  /api/hospital/alerts`              — Pending alerts (all clinics)
- `POST /api/hospital/alerts/{id}/approve` — Approve → trigger smart dispatch
- `POST /api/hospital/alerts/{id}/hold`    — Hold with reason
- `POST /api/hospital/alerts/{id}/dismiss` — Dismiss alert
- `GET  /api/hospital/dashboard?hospitalId=` — ICU + doctor + alert stats
- `PUT  /api/hospital/icu/beds?hospitalId=`  — Update bed counts
- `GET  /api/hospital/doctors?hospitalId=`   — Doctor list

### Admin (requires ADMIN role)
- `GET  /api/admin/dashboard`              — System-wide stats
- `GET  /api/admin/users`                  — All users
- `GET  /api/admin/hospitals`              — All hospitals
- `GET  /api/admin/audit`                  — Paginated WORM audit logs
- `POST /api/admin/audit/verify`           — SHA-256 chain verification

### AI Service (internal)
- `POST /predict`   — Vitals → { risk_score, risk_level, confidence, top_features }
- `POST /explain`   — Prediction → { explanation, treatment_recs, paramedic_guidance }

## Features Implemented
- ✅ JWT auth (role-based: CLINIC / HOSPITAL / ADMIN)
- ✅ BCrypt password hashing
- ✅ Age-adaptive triage (Neonatal / Pediatric / Adult)
- ✅ 10 emergency types (SEPSIS, CARDIAC, STROKE, RESPIRATORY…)
- ✅ XGBoost ML model + rule-engine fallback
- ✅ LLM explanation + treatment recs + paramedic guidance
- ✅ Smart hospital routing (mortality + ICU + distance + specialization)
- ✅ Real-time WebSocket alerts to hospital dashboard
- ✅ Offline-first clinic (queue → sync)
- ✅ WORM audit log with SHA-256 hash chain
- ✅ AES-256-GCM encryption service
- ✅ HIPAA / DPDP compliance markers
- ✅ Doctor availability management
- ✅ ICU bed tracking
- ✅ Admin panel with user management
- ✅ All 6 original bugs fixed
