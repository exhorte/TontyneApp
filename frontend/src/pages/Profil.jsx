import { useState } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { authApi } from '../api/auth.js'
import { normaliserErreur } from '../utils/errors.js'
import PageHeader, { InfoItem } from '../components/PageHeader.jsx'
import Alert from '../components/Alert.jsx'
import Badge from '../components/Badge.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Loader from '../components/Loader.jsx'
import Icone from '../components/Icone.jsx'

const LIBELLES_ROLE = {
  ADMINISTRATEUR: 'Administrateur',
  MEMBRE: 'Membre',
}

/**
 * Profil du compte authentifie : identite, numero de telephone (identifiant
 * principal), role global, et gestion de l'adresse electronique facultative
 * (GET /auth/me, POST/DELETE /auth/email, POST /auth/email/confirmer).
 */
export default function Profil() {
  const { profil, rafraichirProfil } = useAuth()

  const [etapeEmail, setEtapeEmail] = useState('repos') // 'repos' | 'saisie' | 'confirmation'
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [erreur, setErreur] = useState(null)
  const [succes, setSucces] = useState('')
  const [envoi, setEnvoi] = useState(false)

  if (!profil) return <Loader message="Chargement du profil..." />

  const reinitialiser = () => {
    setEtapeEmail('repos')
    setEmail('')
    setCode('')
    setErreur(null)
  }

  const soumettreEmail = async (evenement) => {
    evenement.preventDefault()
    setErreur(null)
    setSucces('')
    setEnvoi(true)
    try {
      const message = await authApi.ajouterEmail(email.trim())
      setSucces(typeof message === 'string' ? message : 'Un code de confirmation vous a été envoyé.')
      setEtapeEmail('confirmation')
      await rafraichirProfil()
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
      await authApi.confirmerEmail(code.trim())
      setSucces('Adresse électronique confirmée.')
      await rafraichirProfil()
      reinitialiser()
    } catch (e) {
      setErreur(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  const retirerEmail = async () => {
    setErreur(null)
    setSucces('')
    setEnvoi(true)
    try {
      await authApi.retirerEmail()
      await rafraichirProfil()
    } catch (e) {
      setErreur(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <>
      <PageHeader titre="Mon profil" sousTitre="Informations de votre compte Tontyn." />

      {erreur && (
        <Alert type="erreur" champs={erreur.champs}>
          {erreur.message}
        </Alert>
      )}
      {succes && !erreur && <Alert type="succes">{succes}</Alert>}

      <div className="carte">
        <div className="liste-infos">
          <InfoItem cle="Prénom">{profil.prenom}</InfoItem>
          <InfoItem cle="Nom">{profil.nom}</InfoItem>
          <InfoItem cle="Numéro de téléphone (identifiant)">{profil.telephone}</InfoItem>
          <InfoItem cle="Rôle">
            <Badge
              variante={profil.role === 'ADMINISTRATEUR' ? 'succes' : 'neutre'}
              libelle={LIBELLES_ROLE[profil.role] || profil.role}
            />
          </InfoItem>
        </div>
      </div>

      <div className="carte" style={{ marginTop: 16 }}>
        <h2 style={{ marginTop: 0 }}>Adresse électronique</h2>
        <p className="texte-discret">
          Facultative : un second canal de récupération, en plus de votre numéro de téléphone.
        </p>

        {profil.email && profil.emailVerifie ? (
          <>
            <div className="liste-infos" style={{ marginBottom: 16 }}>
              <InfoItem cle="Adresse confirmée">
                {profil.email} <Badge variante="succes" libelle="Vérifiée" />
              </InfoItem>
            </div>
            <Button variante="secondaire" onClick={retirerEmail} chargement={envoi}>
              Retirer cette adresse
            </Button>
          </>
        ) : etapeEmail === 'repos' ? (
          profil.emailEnAttente ? (
            <>
              <Alert type="info">
                Une confirmation est en attente pour {profil.emailEnAttente}.
              </Alert>
              <Button variante="principal" onClick={() => setEtapeEmail('confirmation')}>
                Saisir le code reçu
              </Button>
            </>
          ) : (
            <Button variante="principal" onClick={() => setEtapeEmail('saisie')}>
              {<><Icone nom="plus" taille={16} /> Associer une adresse</>}
            </Button>
          )
        ) : etapeEmail === 'saisie' ? (
          <form onSubmit={soumettreEmail} noValidate>
            <Field
              label="Adresse électronique"
              nom="email"
              type="email"
              valeur={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="prenom.nom@exemple.sn"
              requis
            />
            <Button type="submit" variante="principal" chargement={envoi}>
              Envoyer le code de confirmation
            </Button>{' '}
            <Button type="button" onClick={reinitialiser} disabled={envoi}>
              Annuler
            </Button>
          </form>
        ) : (
          <form onSubmit={soumettreCode} noValidate>
            <Field
              label="Code de confirmation (6 chiffres)"
              nom="code"
              type="text"
              inputMode="numeric"
              maxLength={6}
              valeur={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              aide={`Code envoyé à ${profil.emailEnAttente || 'votre adresse'}.`}
              requis
            />
            <Button type="submit" variante="principal" chargement={envoi} disabled={code.length !== 6}>
              Confirmer
            </Button>{' '}
            <Button type="button" onClick={reinitialiser} disabled={envoi}>
              Annuler
            </Button>
          </form>
        )}
      </div>
    </>
  )
}
