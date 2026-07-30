/** Indicateur de chargement (role=status pour les lecteurs d'ecran). */
export default function Loader({ message = 'Chargement en cours...' }) {
  return (
    <div className="chargement" role="status" aria-live="polite">
      <div className="spinner" aria-hidden="true" />
      <span>{message}</span>
    </div>
  )
}
