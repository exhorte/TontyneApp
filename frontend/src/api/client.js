import axios from 'axios'

export const CLE_TOKEN = 'tontinesafe.token'

/** Seule donnee persistee cote navigateur : le jeton JWT. */
export const stockageToken = {
  lire: () => localStorage.getItem(CLE_TOKEN),
  ecrire: (token) => localStorage.setItem(CLE_TOKEN, token),
  effacer: () => localStorage.removeItem(CLE_TOKEN),
}

// Client HTTP centralise vers l'API Spring Boot.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

// Ajoute automatiquement le jeton JWT si present.
client.interceptors.request.use((config) => {
  const token = stockageToken.lire()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/**
 * L'AuthContext enregistre ici sa fonction de deconnexion : cela evite un import
 * circulaire entre le client HTTP et le contexte React.
 */
let surSessionExpiree = null
export function enregistrerGestionnaire401(callback) {
  surSessionExpiree = callback
}

// Reponse 401 -> le jeton est absent, invalide ou expire : on deconnecte.
client.interceptors.response.use(
  (reponse) => reponse,
  (erreur) => {
    const estAppelAuth = erreur?.config?.url?.includes('/auth/')
    if (erreur?.response?.status === 401 && !estAppelAuth && surSessionExpiree) {
      surSessionExpiree()
    }
    return Promise.reject(erreur)
  },
)

export default client
