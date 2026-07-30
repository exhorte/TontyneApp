import client from './client.js'

/**
 * Ressource Tontine.
 * Reponse (TontineResponse) : { id, nom, description, montantCotisation, periodicite,
 *   nombreMembres, nombreMembresInscrits, nombreCycles, dateCreation, statut }
 */
export const tontinesApi = {
  lister: (params = {}) => client.get('/tontines', { params }).then((r) => r.data),
  obtenir: (id) => client.get(`/tontines/${id}`).then((r) => r.data),
  listerParUtilisateur: (utilisateurId) =>
    client.get(`/tontines/utilisateur/${utilisateurId}`).then((r) => r.data),

  creer: (donnees) => client.post('/tontines', donnees).then((r) => r.data),
  modifier: (id, donnees) => client.put(`/tontines/${id}`, donnees).then((r) => r.data),
  cloturer: (id) => client.patch(`/tontines/${id}/cloturer`).then((r) => r.data),
  supprimer: (id) => client.delete(`/tontines/${id}`).then((r) => r.data),

  // Sous-ressources
  listerMembres: (id) => client.get(`/tontines/${id}/membres`).then((r) => r.data),
  ajouterMembre: (id, donnees) =>
    client.post(`/tontines/${id}/membres`, donnees).then((r) => r.data),
  listerCycles: (id) => client.get(`/tontines/${id}/cycles`).then((r) => r.data),
  /** Genere un cycle par membre actif, a partir de { dateDebut }. */
  genererCycles: (id, donnees) =>
    client.post(`/tontines/${id}/cycles/generer`, donnees).then((r) => r.data),
}
