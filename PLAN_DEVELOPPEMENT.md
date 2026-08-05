# Tontyn — Plan de développement

Plateforme web et mobile sécurisée de gestion des tontines communautaires.
Architecture **3-tiers** : présentation (React + Flutter) · application (Spring Boot) · données (PostgreSQL).

## 1. Découpage en modules

| Module | Responsabilité |
|---|---|
| Authentification | Inscription, connexion, JWT, 2FA par code OTP e-mail, BCrypt |
| Utilisateurs & rôles | Comptes, rôles (ADMINISTRATEUR, GESTIONNAIRE, MEMBRE) |
| Tontines | Création et paramétrage des groupes (montant, périodicité, ordre) |
| Membres | Adhésion, invitation, retrait, attribution de rôle dans le groupe |
| Cycles | Cycles de cotisation, tours de distribution, bénéficiaire |
| Cotisations | Enregistrement et suivi des versements |
| Paiements | Intégration Orange Money / Wave, statut de transaction |
| Reçus | Génération de reçus numériques |
| Notifications | Rappels et alertes par e-mail (SMTP) |
| Tableau de bord | Suivi temps réel : cotisations, soldes, historiques |

## 2. Backlog SCRUM (épics → user stories)

### Sprint 0 — Mise en place (infrastructure)
- Initialiser les 3 projets (backend, frontend, mobile)
- Docker Compose PostgreSQL + profils dev/prod
- Configuration Git, conventions de code, CI de base

### Sprint 1 — Authentification & sécurité
- En tant qu'utilisateur, je peux créer un compte (mot de passe haché BCrypt)
- En tant qu'utilisateur, je me connecte et reçois un code OTP par e-mail (2FA)
- En tant qu'utilisateur authentifié, je reçois un jeton JWT et j'accède aux ressources protégées
- Gestion des rôles et contrôle d'accès

### Sprint 2 — Tontines & membres
- Créer / paramétrer une tontine (gestionnaire)
- Inviter, ajouter, retirer des membres ; attribuer des rôles
- Consulter la liste de ses tontines (membre)

### Sprint 3 — Cycles & cotisations
- Générer et suivre les cycles de cotisation et les tours
- Désigner le bénéficiaire d'un cycle
- Enregistrer une cotisation et suivre son statut

### Sprint 4 — Paiements & reçus
- Initier un paiement via Orange Money / Wave (simulateur en dev)
- Confirmer le paiement, enregistrer la cotisation
- Générer un reçu numérique (PDF)

### Sprint 5 — Notifications & tableau de bord
- Envoyer des rappels de cotisation par e-mail
- Tableau de bord : cotisations, soldes, historiques en temps réel

### Sprint 6 — Tests, validation & finalisation
- Tests unitaires (JUnit / Mockito) et d'intégration (Postman)
- Tests des interfaces, validation groupe pilote
- Documentation et préparation de la soutenance

## 3. Roadmap de mise en place (ordre conseillé)

1. **Environnement** : Docker + PostgreSQL, profils Spring `dev` / `prod`.
2. **Backend** : entités JPA → repositories → sécurité (JWT + 2FA) → contrôleurs REST.
3. **Frontend React** : client Axios, page de connexion, tableau de bord.
4. **Mobile Flutter** : service HTTP, écrans de connexion et de cotisation.
5. **Intégrations** : Orange Money / Wave, envoi d'e-mails SMTP.
6. **Tests & validation**.

## 4. Structure des dossiers

```
SOURCES/
├─ PLAN_DEVELOPPEMENT.md      ← ce document
├─ README.md                  ← comment démarrer
├─ docker-compose.yml         ← PostgreSQL (+ pgAdmin)
├─ backend/                   ← Spring Boot (API REST, JPA, sécurité)
├─ frontend/                  ← React (Vite)
└─ mobile/                    ← Flutter
```

## 5. Pile technique

- **Backend** : Java 17, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security, JWT (jjwt), Spring Mail, PostgreSQL.
- **Frontend** : React 18 (Vite), React Router, Axios.
- **Mobile** : Flutter (Dio pour l'API).
- **Base de données** : PostgreSQL 16 (Docker en local).
- **Outils** : Git/GitHub, Maven, Docker, Postman, IntelliJ IDEA / VS Code.
