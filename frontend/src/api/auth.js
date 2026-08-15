import client from './client.js'

/**
 * Authentification a double facteur.
 * /auth/register et /auth/login repondent en text/plain ; /auth/verify-otp
 * renvoie un JSON { token, message }.
 */
export const authApi = {
  /** Etape 0 : creation du compte (role MEMBRE attribue par le backend). */
  inscrire: (donnees) => client.post('/auth/register', donnees).then((r) => r.data),

  /** Etape 1 : verification du code PIN, envoi du code OTP par SMS. */
  connexion: ({ telephone, codePin }) =>
    client.post('/auth/login', { telephone, codePin }).then((r) => r.data),

  /** Etape 2 : verification du code OTP, delivrance du jeton JWT. */
  verifierOtp: ({ telephone, code }) =>
    client.post('/auth/verify-otp', { telephone, code }).then((r) => r.data),

  /** Profil de l'utilisateur authentifie. */
  moi: () => client.get('/auth/me').then((r) => r.data),

  /** Association d'une adresse electronique, depuis le profil. */
  ajouterEmail: (email) => client.post('/auth/email', { email }).then((r) => r.data),

  /** Confirmation de l'adresse au moyen du code recu. */
  confirmerEmail: (code) => client.post('/auth/email/confirmer', { code }).then((r) => r.data),
}
