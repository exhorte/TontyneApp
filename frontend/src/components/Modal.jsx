import { useEffect, useRef } from 'react'
import Button from './Button.jsx'

/**
 * Modale accessible : fermeture par Echap, focus place a l'ouverture,
 * focus rendu a l'element declencheur a la fermeture, defilement de fond bloque.
 */
export default function Modal({
  ouverte,
  titre,
  sousTitre,
  onFermer,
  children,
  pied,
  large = false,
}) {
  const boiteRef = useRef(null)
  const focusPrecedent = useRef(null)

  useEffect(() => {
    if (!ouverte) return undefined

    focusPrecedent.current = document.activeElement
    const surTouche = (evenement) => {
      if (evenement.key === 'Escape') onFermer?.()
    }
    document.addEventListener('keydown', surTouche)

    const debordementInitial = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    // Place le focus sur le premier element interactif de la modale.
    const premier = boiteRef.current?.querySelector(
      'input, select, textarea, button:not(.modale__fermer)',
    )
    premier?.focus()

    return () => {
      document.removeEventListener('keydown', surTouche)
      document.body.style.overflow = debordementInitial
      focusPrecedent.current?.focus?.()
    }
  }, [ouverte, onFermer])

  if (!ouverte) return null

  return (
    <div
      className="modale-voile"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onFermer?.()
      }}
    >
      <div
        className={`modale${large ? ' modale--large' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={titre}
        ref={boiteRef}
      >
        <div className="modale__entete">
          <div>
            <h2>{titre}</h2>
            {sousTitre && <p className="modale__sous-titre">{sousTitre}</p>}
          </div>
          <button
            type="button"
            className="modale__fermer"
            onClick={onFermer}
            aria-label="Fermer la fenetre"
          >
            ×
          </button>
        </div>

        <div className="modale__corps">{children}</div>

        {pied && <div className="modale__pied">{pied}</div>}
      </div>
    </div>
  )
}

/**
 * Modale de confirmation pour les actions destructives ou irreversibles
 * (suppression, cloture, suspension).
 */
export function ConfirmDialog({
  ouverte,
  titre = 'Confirmer l’action',
  message,
  detail,
  libelleConfirmation = 'Confirmer',
  dangereux = false,
  chargement = false,
  onConfirmer,
  onAnnuler,
}) {
  return (
    <Modal
      ouverte={ouverte}
      titre={titre}
      onFermer={chargement ? undefined : onAnnuler}
      pied={
        <>
          <Button onClick={onAnnuler} disabled={chargement}>
            Annuler
          </Button>
          <Button
            variante={dangereux ? 'danger-plein' : 'principal'}
            onClick={onConfirmer}
            chargement={chargement}
          >
            {libelleConfirmation}
          </Button>
        </>
      }
    >
      <p>{message}</p>
      {detail && <p className="texte-discret mt-16">{detail}</p>}
    </Modal>
  )
}
