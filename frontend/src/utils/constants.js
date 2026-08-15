// Valeurs metier acceptees par le backend (voir les DTO Spring : TontineRequest,
// CycleRequest, MembreRequest, PaiementRequest, NotificationRequest).

export const ROLES = {
  ADMINISTRATEUR: 'ADMINISTRATEUR',
  MEMBRE: 'MEMBRE',
}

/**
 * Roles autorises a TENTER une action de gestion.
 *
 * Depuis la refonte, les droits ne decoulent plus du role global : ils dependent
 * de la tontine concernee. Tout compte peut donc tenter une action, et c'est le
 * serveur qui tranche (403 sinon). Lorsque l'ecran connait la tontine, il affine
 * l'affichage grace au drapeau "administrateur" renvoye par l'API.
 */
export const ROLES_ACTION = [ROLES.ADMINISTRATEUR, ROLES.MEMBRE]

/** Reserve a l'exploitant de la plateforme (supervision globale). */
export const ROLES_PLATEFORME = [ROLES.ADMINISTRATEUR]

export const PERIODICITES = [
  { valeur: 'QUOTIDIENNE', libelle: 'Quotidienne' },
  { valeur: 'HEBDOMADAIRE', libelle: 'Hebdomadaire' },
  { valeur: 'BIMENSUELLE', libelle: 'Bimensuelle (2 semaines)' },
  { valeur: 'MENSUELLE', libelle: 'Mensuelle' },
  { valeur: 'TRIMESTRIELLE', libelle: 'Trimestrielle' },
]

export const STATUTS_TONTINE = [
  { valeur: 'ACTIVE', libelle: 'Active' },
  { valeur: 'SUSPENDUE', libelle: 'Suspendue' },
  { valeur: 'CLOTUREE', libelle: 'Cloturee' },
]

export const STATUTS_CYCLE = [
  { valeur: 'PLANIFIE', libelle: 'Planifie' },
  { valeur: 'EN_COURS', libelle: 'En cours' },
  { valeur: 'CLOTURE', libelle: 'Cloture' },
]

export const ROLES_GROUPE = [
  { valeur: 'MEMBRE', libelle: 'Membre' },
  { valeur: 'GESTIONNAIRE', libelle: 'Gestionnaire de la tontine' },
]

export const METHODES_PAIEMENT = [
  { valeur: 'ORANGE_MONEY', libelle: 'Orange Money' },
  { valeur: 'WAVE', libelle: 'Wave' },
]

export const CANAUX_NOTIFICATION = [
  { valeur: 'EMAIL', libelle: 'E-mail' },
  { valeur: 'SMS', libelle: 'SMS' },
  { valeur: 'PUSH', libelle: 'Notification push' },
]

/** Libelles lisibles pour les statuts techniques renvoyes par l'API. */
export const LIBELLES_STATUT = {
  ACTIVE: 'Active',
  ACTIF: 'Actif',
  SUSPENDUE: 'Suspendue',
  SUSPENDU: 'Suspendu',
  CLOTUREE: 'Cloturee',
  CLOTURE: 'Cloture',
  PLANIFIE: 'Planifie',
  EN_COURS: 'En cours',
  EN_ATTENTE: 'En attente',
  PAYEE: 'Payee',
  INITIE: 'Initie',
  CONFIRME: 'Confirme',
  ANNULE: 'Annule',
  ENVOYEE: 'Non lue',
  LUE: 'Lue',
}

/**
 * Correspondance statut -> variante visuelle du composant Badge.
 * succes (vert), attente (ambre), neutre (gris), danger (rouge), info (bleu).
 */
export const VARIANTES_STATUT = {
  ACTIVE: 'succes',
  ACTIF: 'succes',
  PAYEE: 'succes',
  CONFIRME: 'succes',
  LUE: 'neutre',
  EN_COURS: 'info',
  PLANIFIE: 'info',
  ENVOYEE: 'attente',
  EN_ATTENTE: 'attente',
  INITIE: 'attente',
  SUSPENDUE: 'danger',
  SUSPENDU: 'danger',
  ANNULE: 'danger',
  CLOTUREE: 'neutre',
  CLOTURE: 'neutre',
}

export const LIBELLES_METHODE = {
  ORANGE_MONEY: 'Orange Money',
  WAVE: 'Wave',
}
