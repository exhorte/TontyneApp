import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import Icone from './Icone.jsx'

const ToastContext = createContext(null)

const DUREE_PAR_DEFAUT = 4500

/** Fournit les notifications ephemeres (succes / erreur / info) a toute l'application. */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const retirer = useCallback((id) => {
    setToasts((liste) => liste.filter((t) => t.id !== id))
  }, [])

  const ajouter = useCallback(
    (message, type = 'info', duree = DUREE_PAR_DEFAUT) => {
      const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`
      setToasts((liste) => [...liste, { id, message, type }])
      if (duree > 0) setTimeout(() => retirer(id), duree)
      return id
    },
    [retirer],
  )

  const valeur = useMemo(
    () => ({
      toast: ajouter,
      succes: (message, duree) => ajouter(message, 'succes', duree),
      erreur: (message, duree) => ajouter(message, 'erreur', duree ?? 7000),
      info: (message, duree) => ajouter(message, 'info', duree),
    }),
    [ajouter],
  )

  return (
    <ToastContext.Provider value={valeur}>
      {children}
      <div className="zone-toasts" aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast--${t.type}`} role="status">
            <span className="toast__icone" aria-hidden="true">
              <Icone nom={t.type === 'succes' ? 'succes' : t.type === 'erreur' ? 'erreur' : 'info'} taille={20} />
            </span>
            <div className="toast__contenu">{t.message}</div>
            <button
              type="button"
              className="toast__fermer"
              onClick={() => retirer(t.id)}
              aria-label="Fermer la notification"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const contexte = useContext(ToastContext)
  if (!contexte) {
    throw new Error("useToast doit etre utilise a l'interieur d'un <ToastProvider>.")
  }
  return contexte
}
