import client from './client.js'

/**
 * Ressource Paiement (Orange Money / Wave).
 * Reponse (PaiementResponse) : { id, montant, date, methode, reference, statut,
 *   cotisationId, membreId, membreNom, recuId }
 * Statuts : INITIE -> CONFIRME (emet le recu + notifie) ou ANNULE.
 */
export const paiementsApi = {
  /** Filtre accepte : { statut }. */
  lister: (params = {}) => client.get('/paiements', { params }).then((r) => r.data),
  obtenir: (id) => client.get(`/paiements/${id}`).then((r) => r.data),
  obtenirRecu: (id) => client.get(`/paiements/${id}/recu`).then((r) => r.data),

  /** Ouvert a tout utilisateur authentifie. Corps : { cotisationId, methode, reference? }. */
  initier: (donnees) => client.post('/paiements', donnees).then((r) => r.data),
  /** Reserve ADMIN/GESTIONNAIRE : solde la cotisation, emet le recu, notifie le membre. */
  confirmer: (id) => client.patch(`/paiements/${id}/confirmer`).then((r) => r.data),
  annuler: (id) => client.patch(`/paiements/${id}/annuler`).then((r) => r.data),
  /** Reserve ADMINISTRATEUR. */
  supprimer: (id) => client.delete(`/paiements/${id}`).then((r) => r.data),
}
