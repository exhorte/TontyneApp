import client from './client.js'

/**
 * Ressource Recu (justificatif d'un paiement confirme).
 * Reponse (RecuResponse) : { id, numero, dateEmission, montant, paiementId,
 *   referencePaiement, methode, membreId, membreNom, tontineId, tontineNom, cycleNumero }
 */
export const recusApi = {
  /** Filtre accepte : { membreId }. */
  lister: (params = {}) => client.get('/recus', { params }).then((r) => r.data),
  obtenir: (id) => client.get(`/recus/${id}`).then((r) => r.data),
  obtenirParPaiement: (paiementId) =>
    client.get(`/recus/paiement/${paiementId}`).then((r) => r.data),

  /**
   * Emission manuelle : la confirmation d'un paiement genere deja le recu,
   * cet endpoint sert de rattrapage (409 si un recu existe deja).
   */
  genererPourPaiement: (paiementId) =>
    client.post(`/recus/paiement/${paiementId}`).then((r) => r.data),
  /** Reserve ADMINISTRATEUR. */
  supprimer: (id) => client.delete(`/recus/${id}`).then((r) => r.data),
}
