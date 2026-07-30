import client from './client.js'

/**
 * Authentification a double facteur.
 * /auth/register et /auth/login repondent en text/plain ; /auth/verify-otp
 * renvoie un JSON { token, message }.
 */
export const authApi = {
  /** Etape 0 : creation du compte (role MEMBRE attribue par le backend). */
  inscrire: (donnees) => client.post('/auth/register', donnees).then((r) => r.data),

  /** Etape 1 : verification du mot de passe, envoi du code OTP. */
  connexion: ({ email, motDePasse }) =>
    client.post('/auth/login', { email, motDePasse }).then((r) => r.data),

  /** Etape 2 : verification du code OTP, delivrance du jeton JWT. */
  verifierOtp: ({ email, code }) =>
    client.post('/auth/verify-otp', { email, code }).then((r) => r.data),
}
