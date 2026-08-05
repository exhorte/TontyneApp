import { Link } from 'react-router-dom'
import EmptyState from '../components/EmptyState.jsx'
import Icone from '../components/Icone.jsx'

/** Page 404 de l'application. */
export default function NonTrouve() {
  return (
    <div className="carte">
      <EmptyState
        icone="boussole"
        titre="Page introuvable"
        texte="L'adresse demandée ne correspond à aucune page de Tontyn."
        action={
          <Link className="btn btn--principal" to="/tableau-de-bord">
            Retour au tableau de bord
          </Link>
        }
      />
    </div>
  )
}
