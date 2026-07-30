import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Icone from '../components/Icone.jsx'

/** Page 403 : l'utilisateur est connecte mais son role ne suffit pas. */
export default function AccesRefuse() {
  const { role } = useAuth()
  return (
    <div className="carte">
      <EmptyState
        icone="cadenas"
        titre="Accès refusé"
        texte={`Cette section est réservée à d'autres profils. Votre rôle actuel est « ${role || 'inconnu'} ».`}
        action={
          <Link className="btn btn--principal" to="/tableau-de-bord">
            Retour au tableau de bord
          </Link>
        }
      />
    </div>
  )
}
