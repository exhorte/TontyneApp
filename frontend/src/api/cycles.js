import client from './client.js'

/**
 * Ressource Cycle (un tour de la tontine, avec son beneficiaire).
 * Reponse (CycleResponse) : { id, numero, dateDebut, dateFin, statut, tontineId,
 *   tontineNom, beneficiaireId, beneficiaireNom, montantCollecte }
 */
export const cyclesApi = {
  /** Filtres acceptes : { tontineId, statut }. `statut` n'a d'effet qu'avec `tontineId`. */
  lister: (params = {}) => client.get('/cycles', { params }).then((r) => r.data),
  obtenir: (id) => client.get(`/cycles/${id}`).then((r) => r.data),
  listerCotisations: (id) => client.get(`/cycles/${id}/cotisations`).then((r) => r.data),

  creer: (donnees) => client.post('/cycles', donnees).then((r) => r.data),
  modifier: (id, donnees) => client.put(`/cycles/${id}`, donnees).then((r) => r.data),
  cloturer: (id) => client.patch(`/cycles/${id}/cloturer`).then((r) => r.data),
  supprimer: (id) => client.delete(`/cycles/${id}`).then((r) => r.data),
}
