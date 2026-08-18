import { useState } from 'react'
import { utilisateursApi } from '../api/utilisateurs.js'
import { normaliserErreur } from '../utils/errors.js'

/**
 * Recherche d'un compte de la plateforme par numero de telephone, pour
 * designer la personne a ajouter a une tontine sans exposer l'annuaire
 * complet des utilisateurs.
 */
export default function useRechercheTelephone() {
  const [telephone, setTelephone] = useState('')
  const [utilisateur, setUtilisateur] = useState(null)
  const [recherche, setRecherche] = useState(false)
  const [erreur, setErreur] = useState(null)

  const rechercher = async () => {
    setErreur(null)
    setUtilisateur(null)
    const saisie = telephone.trim()
    if (!saisie) {
      setErreur('Saisissez un numero de telephone.')
      return
    }
    setRecherche(true)
    try {
      const trouve = await utilisateursApi.rechercherParTelephone(saisie)
      setUtilisateur(trouve)
    } catch (e) {
      const { statut, message } = normaliserErreur(e)
      setErreur(
        statut === 404
          ? "Ce numero n'est inscrit sur Tontyn avec aucun compte."
          : message,
      )
    } finally {
      setRecherche(false)
    }
  }

  const reinitialiser = () => {
    setTelephone('')
    setUtilisateur(null)
    setErreur(null)
    setRecherche(false)
  }

  return { telephone, setTelephone, utilisateur, recherche, erreur, rechercher, reinitialiser }
}
