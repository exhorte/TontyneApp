// Valeurs metier acceptees par le backend (voir les DTO Spring : TontineRequest,
// CycleRequest, MembreRequest, PaiementRequest, NotificationRequest).

export const ROLES = {
  ADMINISTRATEUR: 'ADMINISTRATEUR',
  GESTIONNAIRE: 'GESTIONNAIRE',
  MEMBRE: 'MEMBRE',
}

/** Roles autorises a ecrire (creation / modification) sur la plupart des ressources. */
export const ROLES_GESTION = [ROLES.ADMINISTRATEUR, ROLES.GESTIONNAIRE]

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
  { valeur: 'GESTIONNAIRE', libelle: 'Gestionnaire du groupe' },
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
