import client from './client.js'

/**
 * Ressource Membre (lien entre un utilisateur et une tontine).
 * Reponse (MembreResponse) : { id, dateAdhesion, roleGroupe, ordreTour, statut,
 *   utilisateurId, nomComplet, email, tontineId, tontineNom }
 */
export const membresApi = {
  /** Filtres acceptes : { tontineId, utilisateurId }. */
  lister: (params = {}) => client.get('/membres', { params }).then((r) => r.data),
  obtenir: (id) => client.get(`/membres/${id}`).then((r) => r.data),
  listerCotisations: (id) => client.get(`/membres/${id}/cotisations`).then((r) => r.data),

  creer: (donnees) => client.post('/membres', donnees).then((r) => r.data),
  modifier: (id, donnees) => client.put(`/membres/${id}`, donnees).then((r) => r.data),
  suspendre: (id) => client.patch(`/membres/${id}/suspendre`).then((r) => r.data),
  reactiver: (id) => client.patch(`/membres/${id}/reactiver`).then((r) => r.data),
  supprimer: (id) => client.delete(`/membres/${id}`).then((r) => r.data),
}
