import client from './client.js'

/**
 * Ressource Utilisateur (comptes de la plateforme).
 * Reponse (UtilisateurResponse) : { id, nom, prenom, email, telephone, role }.
 * Accessible a tout compte authentifie.
 */
export const utilisateursApi = {
  lister: () => client.get('/utilisateurs').then((r) => r.data),
  obtenir: (id) => client.get(`/utilisateurs/${id}`).then((r) => r.data),
  /** Recherche un compte par numero de telephone exact. 404 si non inscrit. */
  rechercherParTelephone: (telephone) =>
    client.get('/utilisateurs/recherche', { params: { telephone } }).then((r) => r.data),
}
