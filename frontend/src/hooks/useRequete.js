import { useCallback, useEffect, useRef, useState } from 'react'
import { messageErreur } from '../utils/errors.js'

/**
 * Charge des donnees depuis l'API en gerant les etats chargement / erreur,
 * et en ignorant les reponses obsoletes (changement de filtre, demontage).
 *
 * @param appel fonction asynchrone retournant les donnees
 * @param dependances tableau de dependances (comme useEffect)
 * @param options.actif si false, la requete n'est pas lancee
 * @param options.valeurInitiale valeur avant le premier chargement
 */
export default function useRequete(appel, dependances = [], options = {}) {
  const { actif = true, valeurInitiale = null } = options

  const [donnees, setDonnees] = useState(valeurInitiale)
  const [chargement, setChargement] = useState(actif)
  const [erreur, setErreur] = useState(null)

  const appelRef = useRef(appel)
  appelRef.current = appel

  const compteur = useRef(0)
  const monte = useRef(true)
  useEffect(() => {
    monte.current = true
    return () => {
      monte.current = false
    }
  }, [])

  const executer = useCallback(async () => {
    if (!actif) {
      setChargement(false)
      return
    }
    const version = ++compteur.current
    setChargement(true)
    setErreur(null)
    try {
      const resultat = await appelRef.current()
      // Une requete plus recente a ete lancee entre-temps : on ignore ce resultat.
      if (version !== compteur.current || !monte.current) return
      setDonnees(resultat)
    } catch (e) {
      if (version !== compteur.current || !monte.current) return
      setErreur(messageErreur(e))
    } finally {
      if (version === compteur.current && monte.current) setChargement(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [actif, ...dependances])

  useEffect(() => {
    executer()
  }, [executer])

  return { donnees, chargement, erreur, recharger: executer, setDonnees }
}
