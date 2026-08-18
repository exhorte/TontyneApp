import { useState } from 'react'
import { utilisateursApi } from '../api/utilisateurs.js'
import { normaliserErreur } from '../utils/errors.js'

/**
 * Verifie un numero de telephone au moment de l'ajout d'un membre (pas de
 * bouton de verification separe) : rechercher() est appele depuis le
 * gestionnaire de soumission du formulaire et renvoie le compte trouve, ou
 * leve une erreur normalisee (meme forme que normaliserErreur) si le numero
 * n'est inscrit sur Tontyn avec aucun compte.
 */
export default function useRechercheTelephone() {
  const [telephone, setTelephone] = useState('')

  const rechercher = async () => {
    const saisie = telephone.trim()
    if (!saisie) {
      throw { message: 'Saisissez le numéro de téléphone du membre à ajouter.', champs: {} }
    }
    try {
      return await utilisateursApi.rechercherParTelephone(saisie)
    } catch (e) {
      const { statut, message } = normaliserErreur(e)
      throw {
        message: statut === 404
          ? "Ce numéro n'est inscrit sur Tontyn avec aucun compte."
          : message,
        champs: {},
      }
    }
  }

  const reinitialiser = () => setTelephone('')

  return { telephone, setTelephone, rechercher, reinitialiser }
}
