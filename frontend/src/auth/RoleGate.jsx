import { useAuth } from './AuthContext.jsx'

/**
 * Masque ses enfants si l'utilisateur n'a pas l'un des roles attendus.
 * Sert a n'afficher les actions d'ecriture qu'aux profils autorises — le backend
 * reste la source de verite (@PreAuthorize), ceci evite les 403 inutiles.
 */
export default function RoleGate({ roles, children, remplacement = null }) {
  const { aRole } = useAuth()
  if (!aRole(roles)) return remplacement
  return children
}
