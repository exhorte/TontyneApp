import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth.js'
import { normaliserErreur } from '../utils/errors.js'
import Alert from '../components/Alert.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Icone from '../components/Icone.jsx'

const FORMULAIRE_VIDE = {
  prenom: '',
  nom: '',
  email: '',
  telephone: '',
  motDePasse: '',
  confirmation: '',
}

/**
 * Inscription. Le backend attribue systematiquement le role MEMBRE ;
 * la montee en GESTIONNAIRE/ADMINISTRATEUR releve de l'administration.
 */
export default function Register() {
  const navigate = useNavigate()
  const [formulaire, setFormulaire] = useState(FORMULAIRE_VIDE)
  const [erreur, setErreur] = useState(null)
  const [erreursLocales, setErreursLocales] = useState({})
  const [envoi, setEnvoi] = useState(false)

  const majChamp = (nom) => (evenement) => {
    setFormulaire((f) => ({ ...f, [nom]: evenement.target.value }))
    setErreursLocales((e) => ({ ...e, [nom]: undefined }))
  }

  /** Controles effectues cote client avant l'appel (le backend revalide de toute facon). */
  const validerLocalement = () => {
    const erreurs = {}
    if (!formulaire.prenom.trim()) erreurs.prenom = 'Le prénom est obligatoire.'
    if (!formulaire.nom.trim()) erreurs.nom = 'Le nom est obligatoire.'
    if (!formulaire.email.trim()) erreurs.email = "L'e-mail est obligatoire."
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formulaire.email.trim())) {
      erreurs.email = "Format d'e-mail invalide."
    }
    if (formulaire.motDePasse.length < 8) {
      erreurs.motDePasse = 'Le mot de passe doit contenir au moins 8 caractères.'
    }
    if (formulaire.confirmation !== formulaire.motDePasse) {
      erreurs.confirmation = 'Les deux mots de passe ne correspondent pas.'
    }
    if (formulaire.telephone && !/^[+0-9][0-9 ]{7,19}$/.test(formulaire.telephone.trim())) {
      erreurs.telephone = 'Numéro de téléphone invalide (ex. +221 77 000 00 00).'
    }
    setErreursLocales(erreurs)
    return Object.keys(erreurs).length === 0
  }

  const soumettre = async (evenement) => {
    evenement.preventDefault()
    setErreur(null)
    if (!validerLocalement()) return

    setEnvoi(true)
    try {
      await authApi.inscrire({
        nom: formulaire.nom.trim(),
        prenom: formulaire.prenom.trim(),
        email: formulaire.email.trim(),
        motDePasse: formulaire.motDePasse,
        telephone: formulaire.telephone.trim(),
      })
      navigate('/login', {
        state: { messageInscription: 'Compte créé avec succès. Vous pouvez vous connecter.' },
      })
    } catch (e) {
      setErreur(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  const erreurChamp = (nom) => erreursLocales[nom] || erreur?.champs?.[nom]

  return (
    <div className="page-auth page-auth--inscription">
      <div className="carte-auth">
        <div className="carte-auth__marque">
          <span className="logo-pastille" aria-hidden="true">T</span>
          <span>Tontyn</span>
        </div>
        <h1 className="auth-titre">Rejoignez vos tontines en confiance.</h1>
        <p className="auth-sous-titre">
          Créez votre compte et suivez vos cotisations, vos cycles et vos reçus
          en toute sécurité.
        </p>

        {erreur && (
          <Alert type="erreur" champs={erreur.champs}>
            {erreur.message}
          </Alert>
        )}

        <form onSubmit={soumettre} noValidate>
          <div className="grille-champs">
            <Field
              label="Prénom"
              nom="prenom"
              valeur={formulaire.prenom}
              onChange={majChamp('prenom')}
              erreur={erreurChamp('prenom')}
              autoComplete="given-name"
              requis
            />
            <Field
              label="Nom"
              nom="nom"
              valeur={formulaire.nom}
              onChange={majChamp('nom')}
              erreur={erreurChamp('nom')}
              autoComplete="family-name"
              requis
            />
          </div>

          <Field
            label="Adresse e-mail"
            nom="email"
            type="email"
            valeur={formulaire.email}
            onChange={majChamp('email')}
            erreur={erreurChamp('email')}
            autoComplete="email"
            requis
          />

          <Field
            label="Téléphone"
            nom="telephone"
            type="tel"
            valeur={formulaire.telephone}
            onChange={majChamp('telephone')}
            erreur={erreurChamp('telephone')}
            aide="Facultatif. Format attendu : +221 77 000 00 00"
            autoComplete="tel"
          />

          <div className="grille-champs">
            <Field
              label="Mot de passe"
              nom="motDePasse"
              type="password"
              valeur={formulaire.motDePasse}
              onChange={majChamp('motDePasse')}
              erreur={erreurChamp('motDePasse')}
              aide="8 caractères minimum"
              autoComplete="new-password"
              requis
            />
            <Field
              label="Confirmation"
              nom="confirmation"
              type="password"
              valeur={formulaire.confirmation}
              onChange={majChamp('confirmation')}
              erreur={erreursLocales.confirmation}
              autoComplete="new-password"
              requis
            />
          </div>

          <Button type="submit" variante="principal" bloc chargement={envoi}>
            Créer mon compte
          </Button>
        </form>

        <div className="auth-atouts">
          <span className="auth-atout">
            <Icone nom="succes" taille={16} /> Traçabilité totale
          </span>
          <span className="auth-atout">
            <Icone nom="succes" taille={16} /> Double authentification
          </span>
          <span className="auth-atout">
            <Icone nom="succes" taille={16} /> Orange Money &amp; Wave
          </span>
        </div>

        <p className="auth-pied">
          Vous avez déjà un compte ? <Link to="/login">Se connecter</Link>
        </p>
      </div>
    </div>
  )
}
