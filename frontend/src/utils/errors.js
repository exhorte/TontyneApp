/**
 * Normalisation des erreurs renvoyees par l'API.
 *
 * Le backend expose un format uniforme (record ApiError) :
 *   { horodatage, statut, erreur, message, chemin, champs? }
 * "champs" n'est present que pour les 400 de validation (@Valid sur les DTO) et
 * associe le nom du champ a son message : { "nom": "Le nom est obligatoire." }
 */

const MESSAGES_PAR_STATUT = {
  401: 'Session expiree ou non authentifiee. Merci de vous reconnecter.',
  403: "Acces refuse : vous n'avez pas les privileges necessaires pour cette action.",
  404: 'Ressource introuvable.',
  409: 'Operation impossible : conflit avec une regle de gestion.',
  500: 'Une erreur interne est survenue. Reessayez plus tard.',
}

/**
 * Transforme une erreur axios en objet exploitable par les formulaires.
 * @returns {{ statut: number|null, message: string, champs: Record<string,string> }}
 */
export function normaliserErreur(erreur) {
  // Pas de reponse : backend eteint, CORS, coupure reseau.
  if (!erreur?.response) {
    return {
      statut: null,
      message:
        "Impossible de joindre le serveur. Verifiez que l'API est demarree sur "
        + `${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}.`,
      champs: {},
    }
  }

  const { status, data } = erreur.response

  // Certains endpoints (auth) repondent en text/plain.
  if (typeof data === 'string' && data.trim()) {
    return { statut: status, message: data.trim(), champs: {} }
  }

  const champs = data && typeof data.champs === 'object' && data.champs ? data.champs : {}

  // Sur un 400 de validation, le message generique "Donnees invalides." est peu
  // parlant seul : on met en avant les messages par champ.
  let message = data?.message || MESSAGES_PAR_STATUT[status] || 'Une erreur est survenue.'
  if (status === 400 && Object.keys(champs).length > 0) {
    message = 'Certains champs sont invalides : corrigez-les puis reessayez.'
  }

  return { statut: status, message, champs }
}

/** Raccourci lorsqu'on ne souhaite afficher qu'un message (toast, banniere). */
export function messageErreur(erreur) {
  return normaliserErreur(erreur).message
}
