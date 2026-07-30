import client from './client.js'

/**
 * Ressource Utilisateur (comptes de la plateforme).
 * Reponse (UtilisateurResponse) : { id, nom, prenom, email, telephone, role }.
 * Acces reserve aux roles ADMINISTRATEUR / GESTIONNAIRE.
 */
export const utilisateursApi = {
  lister: () => client.get('/utilisateurs').then((r) => r.data),
  obtenir: (id) => client.get(`/utilisateurs/${id}`).then((r) => r.data),
}
