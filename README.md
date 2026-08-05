# Tontyn

Plateforme web et mobile sécurisée de gestion des tontines communautaires.
Architecture 3-tiers : **React / Flutter** (présentation) · **Spring Boot** (application) · **PostgreSQL** (données).

Voir `PLAN_DEVELOPPEMENT.md` pour le découpage en modules et le backlog SCRUM.

## Démarrage rapide

### 1. Base de données (Docker)
```bash
docker compose up -d db      # PostgreSQL sur localhost:5432
# pgAdmin optionnel : http://localhost:5050
```

### 2. Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run       # profil 'dev' par défaut, API sur http://localhost:8080
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

## Arborescence
- `backend/`  — API REST, entités JPA, sécurité JWT + 2FA
- `frontend/` — application web React (Vite)
- `mobile/`   — application mobile Flutter
