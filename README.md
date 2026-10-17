# Tontyn

Secure web and mobile platform for managing community savings circles (tontines).
3-tier architecture: **React / Flutter** (presentation) · **Spring Boot** (application) · **PostgreSQL** (data).

See `PLAN_DEVELOPPEMENT.md` (in French) for the module breakdown and the SCRUM backlog.

## Quick start

### 1. Database (Docker)
```bash
docker compose up -d db      # PostgreSQL on localhost:5432
# optional pgAdmin: http://localhost:5050
```

### 2. Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run       # 'dev' profile by default, API on http://localhost:8080
```

### 3. Frontend (React)
```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173
```

### 4. Mobile (Flutter)
```bash
cd mobile
flutter pub get
flutter run
```

## Project layout
- `backend/`  — REST API, JPA entities, JWT + 2FA security
- `frontend/` — React web application (Vite)
- `mobile/`   — Flutter mobile application
