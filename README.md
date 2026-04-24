# 🏥 VigilAI MedLink v2.0

> Real-time AI-driven triage system with microservices architecture and live hospital coordination.

![Java](https://img.shields.io/badge/Backend-SpringBoot-green)
![Python](https://img.shields.io/badge/AI-FastAPI-blue)
![ML](https://img.shields.io/badge/Model-XGBoost-orange)
![Database](https://img.shields.io/badge/DB-PostgreSQL-blue)

---

## 🚀 Overview

VigilAI MedLink is a real-time system that helps clinics detect critical patient conditions early and coordinate with hospitals efficiently.

It combines:

* AI-based risk prediction
* Real-time alerting
* Smart hospital routing

to improve emergency response and decision-making.

---

## 🎯 Key Features

* 🔐 JWT-based authentication (Clinic / Hospital / Admin roles)
* 🧠 AI-powered risk prediction (XGBoost + rule engine fallback)
* ⚡ Real-time alerts using WebSockets
* 🏥 Smart hospital routing (ICU availability, specialization, risk)
* 📊 Admin dashboard with system-wide monitoring
* 🔒 WORM audit logs with SHA-256 integrity verification
* 📡 Offline-first clinic support (queue → sync)
* 🛏️ ICU bed tracking & doctor availability management

---

## 🧠 Problem It Solves

Clinics often lack advanced tools to:

* Identify critical conditions early
* Communicate with hospitals in real-time
* Make data-driven emergency decisions

VigilAI bridges this gap using AI + real-time system design.

---

## 🏗️ System Architecture

Frontend (Browser) → Spring Boot Backend → FastAPI AI Service → PostgreSQL
Communication: REST APIs + WebSockets (JWT Secured)

---

## 📸 Screenshots

### 🔐 Secure Login

<img width="1041" height="887" alt="Login Page" src="https://github.com/user-attachments/assets/c478af2e-e31d-4772-bb55-7212b1892687" />

---

### 🏥 Clinic — Submit Vitals & AI Prediction

<img width="1913" height="923" alt="Clinic Vitals and AI Prediction" src="https://github.com/user-attachments/assets/eef23a30-a434-459b-a8fd-1c1064361133" />

---

### 👥 Clinic — Patient Management

<img width="1915" height="894" alt="Clinic Patient Management" src="https://github.com/user-attachments/assets/7d1fb032-aa08-4a85-a729-d04ce68974e2" />

---

### 🚨 Hospital — Incoming Alerts

<img width="1906" height="906" alt="Hospital Alerts Dashboard" src="https://github.com/user-attachments/assets/dde742fc-342f-49bb-b910-5b3422517d7d" />

---

### 🛏️ ICU Bed Management

<img width="1846" height="898" alt="ICU Bed Dashboard" src="https://github.com/user-attachments/assets/a3aa8344-f4db-4a8b-80f7-483432728354" />

---

### 🧑‍💼 Admin Panel — System Overview

<img width="1914" height="907" alt="Admin Dashboard Overview" src="https://github.com/user-attachments/assets/16b327cd-b1ba-46e2-961a-622cee6accf5" />

---

## ⚙️ Tech Stack

**Frontend:** HTML, CSS, JavaScript
**Backend:** Spring Boot
**AI Service:** FastAPI (Python)
**Database:** PostgreSQL
**ML Model:** XGBoost
**Auth:** JWT + BCrypt
**Realtime:** WebSockets
**Security:** AES-256-GCM, SHA-256 audit chain

---

## ⚡ Quick Start (Docker)

```bash
cd vigilai-complete
docker compose up --build
```

* Frontend: http://localhost:3000/login.html
* Backend: http://localhost:8080/health
* AI Service: http://localhost:8000/health

---

## 🛠️ Manual Setup

### 1. PostgreSQL

```sql
CREATE DATABASE vigilai;
```

Run schema files:

```bash
\i database/schema/01_users.sql
\i database/schema/02_core.sql
```

---

### 2. AI Service

```bash
cd ai-service
pip install -r requirements.txt
python -c "from app.inference.xgboost_model import train; train()"
uvicorn app.main:app --reload --port 8000
```

---

### 3. Backend

```bash
cd backend
mvn spring-boot:run
```

---

### 4. Frontend

```
frontend/login.html
```

---

## 🔐 Demo Credentials

| Role     | Email                                                     | Password     |
| -------- | --------------------------------------------------------- | ------------ |
| CLINIC   | [clinic@vigilai.health](mailto:clinic@vigilai.health)     | Clinic@123   |
| HOSPITAL | [hospital@vigilai.health](mailto:hospital@vigilai.health) | Hospital@123 |
| ADMIN    | [admin@vigilai.health](mailto:admin@vigilai.health)       | Admin@123    |

---

## 📡 API Highlights

### Auth

* POST `/auth/login`
* POST `/auth/register`
* GET `/auth/me`

### Clinic

* Submit vitals → AI prediction → alert
* View patients & alerts

### Hospital

* Approve / hold / dismiss alerts
* ICU & doctor management

### Admin

* System-wide dashboard
* Audit logs with integrity verification

---

## ⚙️ Engineering Highlights

* Implemented microservice architecture (Spring Boot + FastAPI)
* Integrated ML model (XGBoost) with explainability
* Designed real-time alert system using WebSockets
* Built secure audit logging with SHA-256 hash chaining
* Fixed multiple backend issues (type mismatches, config errors)

---

## 🚧 Future Improvements

* Cloud deployment (AWS / GCP)
* Real-time ambulance tracking
* Enhanced ML model with more clinical data
* Mobile application support

---

## 📫 Connect

* GitHub: https://github.com/Omkar4112
* LinkedIn: https://www.linkedin.com/in/-omkar/
