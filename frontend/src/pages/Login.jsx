import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { normaliserErreur } from '../utils/errors.js'
import Alert from '../components/Alert.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Icone from '../components/Icone.jsx'

/**
 * Connexion en deux etapes (double facteur) :
 *   1. numero de telephone + code PIN -> le backend envoie un code OTP a 6 chiffres par SMS
 *   2. saisie du code         -> delivrance du jeton JWT
 */
export default function Login() {
  const { estAuthentifie, demanderCode, validerCode } = useAuth()
  const navigate = useNavigate()
  const emplacement = useLocation()

  const [etape, setEtape] = useState('identifiants') // 'identifiants' | 'otp'
  const [telephone, setTelephone] = useState('')
  const [codePin, setCodePin] = useState('')
  const [code, setCode] = useState('')
  const [erreur, setErreur] = useState(null)
  const [succes, setSucces] = useState('')
  const [envoi, setEnvoi] = useState(false)

  // Deja connecte : on ne repasse pas par le formulaire.
  if (estAuthentifie) {
    return <Navigate to={emplacement.state?.depuis || '/tableau-de-bord'} replace />
  }

  const soumettreIdentifiants = async (evenement) => {
    evenement.preventDefault()
    setErreur(null)
    setEnvoi(true)
    try {
      const message = await demanderCode({ telephone: telephone.trim(), codePin })
      setSucces(typeof message === 'string' ? message : 'Un code de vérification vous a été envoyé.')
      setEtape('otp')
    } catch (e) {
      setErreur(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  const soumettreCode = async (evenement) => {
    evenement.preventDefault()
    setErreur(null)
    setEnvoi(true)
    try {
      await validerCode({ telephone: telephone.trim(), code: code.trim() })
      navigate(emplacement.state?.depuis || '/tableau-de-bord', { replace: true })
    } catch (e) {
      setErreur(
        e?.response ? normaliserErreur(e) : { message: e.message, champs: {} },
      )
    } finally {
      setEnvoi(false)
    }
  }

  const revenirEnArriere = () => {
    setEtape('identifiants')
    setCode('')
    setErreur(null)
    setSucces('')
  }

  return (
    <div className="page-auth page-auth--connexion">
      <div className="auth-marque">
        <span className="logo-pastille" aria-hidden="true">T</span>
        <span>Tontyn</span>
      </div>

      <div className="carte-auth">
        <h1 className="auth-titre">
          {etape === 'identifiants' ? 'Bienvenue sur Tontyn' : 'Vérification'}
        </h1>
        <p className="auth-sous-titre">
          {etape === 'identifiants'
            ? 'Connectez-vous pour gérer vos tontines en toute sécurité.'
            : 'Saisissez le code à six chiffres reçu par SMS.'}
        </p>

        {erreur && (
          <Alert type="erreur" champs={erreur.champs}>
            {erreur.message}
          </Alert>
        )}

        {etape === 'identifiants' ? (
          <form onSubmit={soumettreIdentifiants} noValidate>
            <Field
              label="Numéro de téléphone"
              nom="telephone"
              type="tel"
              valeur={telephone}
              onChange={(e) => setTelephone(e.target.value)}
              erreur={erreur?.champs?.telephone}
              autoComplete="username"
              placeholder="77 123 45 67"
              requis
            />
            <Field
              label="Code PIN (4 chiffres)"
              nom="codePin"
              type="password"
              valeur={codePin}
              onChange={(e) => setCodePin(e.target.value)}
              erreur={erreur?.champs?.codePin}
              autoComplete="current-password"
              requis
            />
            <Button
              type="submit"
              variante="principal"
              bloc
              chargement={envoi}
              disabled={!telephone || !codePin}
            >
              Se connecter
            </Button>
          </form>
        ) : (
          <form onSubmit={soumettreCode} noValidate>
            {succes && <Alert type="info">{succes}</Alert>}

            <div className="champ-otp">
              <Field
                label="Code de vérification (6 chiffres)"
                nom="code"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-password"
                maxLength={6}
                valeur={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                erreur={erreur?.champs?.code}
                aide={`Code envoyé par SMS au ${telephone}. Il expire au bout de 5 minutes.`}
                autoFocus
                requis
              />
            </div>

            <Button
              type="submit"
              variante="principal"
              bloc
              chargement={envoi}
              disabled={code.length !== 6}
            >
              Valider et accéder
            </Button>
            <Button bloc className="mt-16" onClick={revenirEnArriere} disabled={envoi}>
              {<><Icone nom="chevronGauche" taille={16} /> Modifier mes identifiants</>}
            </Button>
          </form>
        )}

        <p className="auth-pied">
          Pas encore de compte ? <Link to="/register">Créer un compte</Link>
        </p>

      </div>
    </div>
  )
}
