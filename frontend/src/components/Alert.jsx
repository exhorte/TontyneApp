import Icone from './Icone.jsx'

const ICONES = {
  erreur: 'erreur',
  succes: 'succes',
  info: 'info',
  attention: 'alerte',
}

/**
 * Banniere de message. `champs` affiche le detail des erreurs de validation
 * renvoyees par le backend (ApiError.champs) sous forme de liste.
 */
export default function Alert({ type = 'info', titre, children, champs }) {
  const details = champs ? Object.entries(champs) : []
  return (
    <div className={`alerte alerte--${type}`} role={type === 'erreur' ? 'alert' : 'status'}>
      <span className="alerte__icone" aria-hidden="true"><Icone nom={ICONES[type] || 'info'} taille={20} /></span>
      <div className="alerte__contenu">
        {titre && <strong>{titre}</strong>}
        {children && <div>{children}</div>}
        {details.length > 0 && (
          <ul>
            {details.map(([champ, message]) => (
              <li key={champ}>{message}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
