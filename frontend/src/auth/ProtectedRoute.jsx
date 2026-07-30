import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'
import Loader from '../components/Loader.jsx'

/**
 * Barriere d'acces : redirige vers /login si aucune session valide.
 * `roles` restreint en plus l'acces a certains roles (page 403 sinon).
 */
export default function ProtectedRoute({ roles }) {
  const { estAuthentifie, pret, aRole } = useAuth()
  const emplacement = useLocation()

  if (!pret) return <Loader message="Verification de la session..." />

  if (!estAuthentifie) {
    // On memorise la page demandee pour y revenir apres connexion.
    return <Navigate to="/login" replace state={{ depuis: emplacement.pathname }} />
  }

  if (roles?.length && !aRole(roles)) {
    return <Navigate to="/acces-refuse" replace />
  }

  return <Outlet />
}
