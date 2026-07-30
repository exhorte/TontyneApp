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
 *   1. e-mail + mot de passe  -> le backend envoie un code OTP a 6 chiffres
 *   2. saisie du code         -> delivrance du jeton JWT
 */
export default function Login() {
  const { estAuthentifie, demanderCode, validerCode } = useAuth()
  const navigate = useNavigate()
  const emplacement = useLocation()

  const [etape, setEtape] = useState('identifiants') // 'identifiants' | 'otp'
  const [email, setEmail] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
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
      const message = await demanderCode({ email: email.trim(), motDePasse })
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
      await validerCode({ email: email.trim(), code: code.trim() })
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
    <div className="page-auth">
      <div className="carte-auth">
        <div className="carte-auth__marque">
          <span className="logo-pastille" aria-hidden="true">TS</span>
          <span className="carte-auth__titre">TontineSafe</span>
        </div>
        <p className="carte-auth__accroche">
          Gestion sécurisée de vos tontines communautaires
        </p>

        {/* Indicateur d'etape du 2FA */}
        <div className="etapes" aria-hidden="true">
          <div className={`etape ${etape === 'identifiants' ? 'etape--active' : 'etape--faite'}`}>
            <span className="etape__puce">{etape === 'otp' ? <Icone nom="succes" taille={16} /> : '1'}</span>
            <span>Identifiants</span>
          </div>
          <span className="etapes__trait" />
          <div className={`etape ${etape === 'otp' ? 'etape--active' : ''}`}>
            <span className="etape__puce">2</span>
            <span>Code de vérification</span>
          </div>
        </div>

        {erreur && (
          <Alert type="erreur" champs={erreur.champs}>
            {erreur.message}
          </Alert>
        )}

        {etape === 'identifiants' ? (
          <form onSubmit={soumettreIdentifiants} noValidate>
            <Field
              label="Adresse e-mail"
              nom="email"
              type="email"
              valeur={email}
              onChange={(e) => setEmail(e.target.value)}
              erreur={erreur?.champs?.email}
              autoComplete="username"
              placeholder="prenom.nom@exemple.sn"
              requis
            />
            <Field
              label="Mot de passe"
              nom="motDePasse"
              type="password"
              valeur={motDePasse}
              onChange={(e) => setMotDePasse(e.target.value)}
              erreur={erreur?.champs?.motDePasse}
              autoComplete="current-password"
              requis
            />
            <Button
              type="submit"
              variante="principal"
              bloc
              chargement={envoi}
              disabled={!email || !motDePasse}
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
                aide={`Code envoyé à ${email}. Il expire au bout de 5 minutes.`}
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

        <p className="carte-auth__pied">
          Pas encore de compte ? <Link to="/register">Créer un compte</Link>
        </p>

        <div className="encart-demo">
          <strong>Démonstration</strong> — compte administrateur&nbsp;:{' '}
          <code>admin@tontinesafe.sn</code> / <code>Admin@1234</code>.
          <br />
          Sans serveur SMTP configuré, le code à 6 chiffres est écrit dans les journaux du
          backend (ligne <code>[MAIL DESACTIVE]</code>).
        </div>
      </div>
    </div>
  )
}
