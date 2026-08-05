# Tontyn — application mobile (Flutter)

Client mobile de la plateforme Tontyn, reprenant le design system du frontend web :
bleu `#0052FF` en accent unique, blanc dominant, neutres chauds, titres en graisse 400,
angles doux et aucune ombre.

## Démarrage

```bash
flutter pub get
flutter run
```

### Adresse de l'API

L'application vise le backend Spring Boot :

| Contexte | Adresse utilisée |
|---|---|
| Émulateur Android | `http://10.0.2.2:8080/api` (le « localhost » de la machine hôte) |
| Autres plateformes | `http://localhost:8080/api` |

Pour un téléphone physique sur le même réseau, indiquez l'adresse IP de votre poste :

```bash
flutter run --dart-define=API_URL=http://192.168.1.10:8080/api
```

## Comptes de démonstration (profil `h2`)

| Compte | Identifiants |
|---|---|
| Administrateur | `admin@tontyn.sn` / `Admin@1234` |
| Gestionnaire | `gestionnaire@tontyn.sn` / `Gestion@1234` |
| Membre | `membre1@tontyn.sn` / `Membre@1234` |

Sans serveur SMTP configuré, le code de vérification à six chiffres est écrit
dans les journaux du backend, sur la ligne `[MAIL DESACTIVE]`.

## Organisation du code

```
lib/
├─ main.dart              point d'entrée et aiguillage de session
├─ theme/app_theme.dart   jetons de conception repris du web
├─ services/
│  ├─ api_service.dart    client HTTP Dio, jeton JWT, erreurs normalisées
│  ├─ auth_service.dart   double authentification et session persistante
│  └─ ressources.dart     accès aux ressources métier de l'API
├─ models/modeles.dart    modèles alignés sur les DTO du backend
├─ widgets/communs.dart   badges, cartes, lignes de liste, états vides
└─ screens/               écrans de l'application
```

## Fonctionnalités

- Connexion à double facteur (mot de passe puis code à usage unique) et inscription.
- Session persistante : le jeton JWT est conservé et vérifié au démarrage.
- Tableau de bord : indicateurs, cotisations à régler, notifications, mes tontines.
- Tontines : liste, création, fiche détaillée avec onglets Membres et Cycles,
  ajout de membre depuis l'annuaire, génération des cycles.
- Cotisations : consultation et enregistrement.
- Paiements : initiation par Orange Money ou Wave, confirmation par un
  gestionnaire, consultation du reçu numérique.
- Membres, cycles, reçus et notifications, avec marquage comme lue.
- Affichage adapté au rôle : les actions de gestion sont réservées aux profils
  administrateur et gestionnaire.
