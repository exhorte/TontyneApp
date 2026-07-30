import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { jwtDecode } from 'jwt-decode'
import client, { enregistrerGestionnaire401, stockageToken } from '../api/client.js'
import { authApi } from '../api/auth.js'
import { membresApi } from '../api/membres.js'

const AuthContext = createContext(null)

/**
 * Decode le JWT emis par le backend.
 * Claims : sub = e-mail, role = ADMINISTRATEUR | GESTIONNAIRE | MEMBRE, exp (secondes).
 * @returns {{ email: string, role: string, expiration: number } | null} null si absent/expire/illisible.
 */
function decoderToken(token) {
  if (!token) return null
  try {
    const charge = jwtDecode(token)
    const expirationMs = charge.exp * 1000
    if (Number.isFinite(expirationMs) && expirationMs <= Date.now()) return null
    return {
      email: charge.sub,
      role: charge.role,
      id: charge.id ?? null,
      expiration: expirationMs,
    }
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  // Rehydratation synchrone depuis le jeton persiste : evite un flash vers /login
  // au rechargement de la page.
  const [utilisateur, setUtilisateur] = useState(() => decoderToken(stockageToken.lire()))
  const [pret, setPret] = useState(false)
  const minuterie = useRef(null)

  const deconnexion = useCallback(() => {
    stockageToken.effacer()
    setUtilisateur(null)
  }, [])

  // Le client HTTP declenche la deconnexion des qu'une reponse 401 arrive.
  useEffect(() => {
    enregistrerGestionnaire401(deconnexion)
  }, [deconnexion])

  // Si le jeton persiste est expire ou illisible, on nettoie le stockage au demarrage.
  useEffect(() => {
    if (stockageToken.lire() && !decoderToken(stockageToken.lire())) {
      stockageToken.effacer()
    }
    setPret(true)
  }, [])

  // Deconnexion automatique a l'echeance exacte du jeton (1 h cote backend).
  useEffect(() => {
    if (minuterie.current) clearTimeout(minuterie.current)
    if (!utilisateur?.expiration) return undefined

    const delai = utilisateur.expiration - Date.now()
    if (delai <= 0) {
      deconnexion()
      return undefined
    }
    minuterie.current = setTimeout(deconnexion, delai)
    return () => clearTimeout(minuterie.current)
  }, [utilisateur?.expiration, deconnexion])

  /**
   * Identifiant numerique de l'utilisateur courant. Il est desormais porte
   * directement par le JWT (claim `id`). Pour un ancien jeton sans ce claim,
   * on retombe sur une resolution via les adhesions (GET /membres).
   */
  const [utilisateurId, setUtilisateurId] = useState(() => decoderToken(stockageToken.lire())?.id ?? null)

  const resoudreUtilisateurId = useCallback(async (email) => {
    if (!email) return null
    try {
      const membres = await membresApi.lister()
      const cible = membres.find(
        (m) => m.email?.toLowerCase() === email.toLowerCase(),
      )
      return cible?.utilisateurId ?? null
    } catch {
      return null
    }
  }, [])

  useEffect(() => {
    let annule = false
    if (!utilisateur?.email) {
      setUtilisateurId(null)
      return undefined
    }
    // L'identifiant est porte par le JWT : on l'utilise directement.
    if (utilisateur.id != null) {
      setUtilisateurId(utilisateur.id)
      return undefined
    }
    // Repli pour un ancien jeton sans claim `id`.
    resoudreUtilisateurId(utilisateur.email).then((id) => {
      if (!annule) setUtilisateurId(id)
    })
    return () => {
      annule = true
    }
  }, [utilisateur?.email, utilisateur?.id, resoudreUtilisateurId])

  /** Etape 1 du 2FA : verifie le mot de passe et declenche l'envoi du code OTP. */
  const demanderCode = useCallback(
    (identifiants) => authApi.connexion(identifiants),
    [],
  )

  /** Etape 2 du 2FA : echange le code contre un JWT et ouvre la session. */
  const validerCode = useCallback(async ({ email, code }) => {
    const reponse = await authApi.verifierOtp({ email, code })
    const token = reponse?.token
    const profil = decoderToken(token)
    if (!token || !profil) {
      throw new Error("Le jeton renvoye par le serveur est invalide ou deja expire.")
    }
    stockageToken.ecrire(token)
    setUtilisateur(profil)
    return profil
  }, [])

  const valeur = useMemo(() => {
    const role = utilisateur?.role ?? null
    return {
      utilisateur,
      utilisateurId,
      role,
      pret,
      estAuthentifie: Boolean(utilisateur),
      /** hasRole('ADMINISTRATEUR', 'GESTIONNAIRE') */
      aRole: (...roles) => (roles.flat().length === 0 ? Boolean(role) : roles.flat().includes(role)),
      demanderCode,
      validerCode,
      deconnexion,
      /** Force une nouvelle tentative de resolution (apres un ajout de membre). */
      rafraichirUtilisateurId: async () => {
        const id = await resoudreUtilisateurId(utilisateur?.email)
        setUtilisateurId(id)
        return id
      },
    }
  }, [utilisateur, utilisateurId, pret, demanderCode, validerCode, deconnexion, resoudreUtilisateurId])

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const contexte = useContext(AuthContext)
  if (!contexte) {
    throw new Error("useAuth doit etre utilise a l'interieur d'un <AuthProvider>.")
  }
  return contexte
}

// Reexport pour les tests manuels / debug depuis la console.
export { client }
