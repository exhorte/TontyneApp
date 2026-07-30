import { useEffect, useState } from 'react'
import { utilisateursApi } from '../api/utilisateurs.js'

/**
 * Annuaire des comptes de la plateforme, alimente par GET /api/utilisateurs
 * (reserve aux roles ADMINISTRATEUR / GESTIONNAIRE). Fournit pour chaque compte
 * { id, nomComplet, email } afin d'alimenter le selecteur d'ajout de membre.
 */
export default function useAnnuaire(actif = true) {
  const [utilisateurs, setUtilisateurs] = useState([])
  const [chargement, setChargement] = useState(actif)

  useEffect(() => {
    if (!actif) return undefined
    let annule = false
    setChargement(true)

    utilisateursApi
      .lister()
      .then((comptes) => {
        if (annule) return
        const liste = comptes
          .map((u) => ({
            id: u.id,
            nomComplet: `${u.prenom ?? ''} ${u.nom ?? ''}`.trim() || u.email,
            email: u.email,
          }))
          .sort((a, b) => a.id - b.id)
        setUtilisateurs(liste)
      })
      .catch(() => {
        if (!annule) setUtilisateurs([])
      })
      .finally(() => {
        if (!annule) setChargement(false)
      })

    return () => {
      annule = true
    }
  }, [actif])

  return { utilisateurs, chargement }
}
