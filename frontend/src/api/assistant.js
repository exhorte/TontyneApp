import client from './client.js'

/**
 * Assistant conversationnel (API DeepSeek cote serveur).
 * Reponse (AssistantResponse) : { reponse, disponible }.
 * `disponible` vaut false quand aucune vraie reponse du modele n'a pu etre
 * produite (assistant desactive, question invalide, limite atteinte, echec
 * DeepSeek) : `reponse` porte alors un message explicatif a afficher tel quel.
 */
export const assistantApi = {
  /** Aucun identifiant a fournir : la portee vient du jeton JWT, cote serveur. */
  demander: (question) =>
    client.post('/assistant/question', { question }).then((r) => r.data),
}
