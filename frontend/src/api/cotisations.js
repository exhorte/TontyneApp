import client from './client.js'

/**
 * Ressource Cotisation (versement attendu d'un membre pour un cycle).
 * Reponse (CotisationResponse) : { id, montant, date, statut, cycleId, cycleNumero,
 *   membreId, membreNom, tontineId, tontineNom }
 * Statuts : EN_ATTENTE -> PAYEE (bascule a la confirmation du paiement).
 */
export const cotisationsApi = {
  /** Filtres acceptes : { cycleId, membreId, tontineId } (priorite dans cet ordre). */
  lister: (params = {}) => client.get('/cotisations', { params }).then((r) => r.data),
  obtenir: (id) => client.get(`/cotisations/${id}`).then((r) => r.data),
  /** 404 si aucun paiement n'est encore rattache a la cotisation. */
  obtenirPaiement: (id) => client.get(`/cotisations/${id}/paiement`).then((r) => r.data),

  /** Ouvert a tout utilisateur authentifie. Corps : { cycleId, membreId, montant? }. */
  creer: (donnees) => client.post('/cotisations', donnees).then((r) => r.data),
  modifier: (id, donnees) => client.put(`/cotisations/${id}`, donnees).then((r) => r.data),
  supprimer: (id) => client.delete(`/cotisations/${id}`).then((r) => r.data),
}
