import client from './client.js'

/**
 * Ressource Notification.
 * Reponse (NotificationResponse) : { id, type, message, dateEnvoi, canal, statut,
 *   utilisateurId, destinataire }
 * Statuts : ENVOYEE (non lue) / EN_ATTENTE (canal SMS ou PUSH) / LUE.
 */
export const notificationsApi = {
  /** Liste globale : reservee ADMIN/GESTIONNAIRE (403 sinon). */
  lister: () => client.get('/notifications').then((r) => r.data),
  obtenir: (id) => client.get(`/notifications/${id}`).then((r) => r.data),
  listerParUtilisateur: (utilisateurId) =>
    client.get(`/notifications/utilisateur/${utilisateurId}`).then((r) => r.data),

  /** Renvoie un compteur { nonLues: number } et non une liste. */
  compterNonLues: (utilisateurId) =>
    client
      .get(`/notifications/utilisateur/${utilisateurId}/non-lues`)
      .then((r) => r.data?.nonLues ?? 0),

  /** Reserve ADMIN/GESTIONNAIRE. Corps : { utilisateurId, type, message, canal? }. */
  envoyer: (donnees) => client.post('/notifications', donnees).then((r) => r.data),
  marquerLue: (id) => client.patch(`/notifications/${id}/lue`).then((r) => r.data),
  supprimer: (id) => client.delete(`/notifications/${id}`).then((r) => r.data),
}
