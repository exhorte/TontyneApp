import { useCallback, useState } from 'react'
import { notificationsApi } from '../api/notifications.js'
import { useAuth } from '../auth/AuthContext.jsx'
import RoleGate from '../auth/RoleGate.jsx'
import useRequete from '../hooks/useRequete.js'
import useAnnuaire from '../hooks/useAnnuaire.js'
import { useToast } from '../components/Toast.jsx'
import { normaliserErreur } from '../utils/errors.js'
import PageHeader from '../components/PageHeader.jsx'
import Table from '../components/Table.jsx'
import Badge from '../components/Badge.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Alert from '../components/Alert.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Modal from '../components/Modal.jsx'
import { formaterDateHeure } from '../utils/format.js'
import { CANAUX_NOTIFICATION, ROLES_GESTION } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

/**
 * Notifications.
 * - « Mes notifications » : GET /notifications/utilisateur/{id} (nécessite l'id résolu).
 * - « Toutes » : GET /notifications, réservé ADMINISTRATEUR/GESTIONNAIRE.
 */
export default function Notifications() {
  const { utilisateurId, aRole } = useAuth()
  const toast = useToast()
  const peutGerer = aRole(ROLES_GESTION)

  const [vue, setVue] = useState('mes') // 'mes' | 'toutes'
  const [seulementNonLues, setSeulementNonLues] = useState(false)

  const charger = useCallback(() => {
    if (vue === 'toutes') return notificationsApi.lister()
    if (!utilisateurId) return []
    return notificationsApi.listerParUtilisateur(utilisateurId)
  }, [vue, utilisateurId])

  const { donnees, chargement, erreur, recharger } = useRequete(charger, [vue, utilisateurId], {
    valeurInitiale: [],
  })

  const notifications = (donnees ?? []).filter(
    (n) => !seulementNonLues || n.statut !== 'LUE',
  )
  const nonLues = (donnees ?? []).filter((n) => n.statut === 'ENVOYEE')

  const [enCours, setEnCours] = useState(null)

  const marquerLue = async (notification) => {
    setEnCours(notification.id)
    try {
      await notificationsApi.marquerLue(notification.id)
      recharger()
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
    } finally {
      setEnCours(null)
    }
  }

  const marquerToutesLues = async () => {
    const cibles = nonLues
    if (cibles.length === 0) return
    setEnCours('toutes')
    // Le backend ne propose pas d'endpoint groupé : on enchaîne les appels unitaires.
    const resultats = await Promise.allSettled(
      cibles.map((n) => notificationsApi.marquerLue(n.id)),
    )
    const echecs = resultats.filter((r) => r.status === 'rejected').length
    if (echecs === 0) toast.succes(`${cibles.length} notification(s) marquée(s) comme lues.`)
    else toast.erreur(`${echecs} notification(s) n'ont pas pu être marquées comme lues.`)
    setEnCours(null)
    recharger()
  }

  // --- Envoi d'une notification (ADMIN / GESTIONNAIRE) --------------------
  const [modale, setModale] = useState(false)
  const { utilisateurs } = useAnnuaire(modale)
  const [formulaire, setFormulaire] = useState({
    utilisateurId: '',
    type: 'INFORMATION',
    message: '',
    canal: 'EMAIL',
  })
  const [erreurFormulaire, setErreurFormulaire] = useState(null)
  const [envoi, setEnvoi] = useState(false)

  const envoyer = async (evenement) => {
    evenement.preventDefault()
    setErreurFormulaire(null)
    setEnvoi(true)
    try {
      const notification = await notificationsApi.envoyer({
        utilisateurId: Number(formulaire.utilisateurId),
        type: formulaire.type.trim(),
        message: formulaire.message.trim(),
        canal: formulaire.canal,
      })
      toast.succes(`Notification envoyée à ${notification.destinataire}.`)
      setModale(false)
      setFormulaire({ utilisateurId: '', type: 'INFORMATION', message: '', canal: 'EMAIL' })
      recharger()
    } catch (e) {
      setErreurFormulaire(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <>
      <PageHeader
        titre="Notifications"
        sousTitre={`${nonLues.length} notification(s) non lue(s) sur ${donnees?.length ?? 0}.`}
        actions={
          <>
            <Button onClick={marquerToutesLues} disabled={nonLues.length === 0} chargement={enCours === 'toutes'}>
              Tout marquer comme lu
            </Button>
            <RoleGate roles={ROLES_GESTION}>
              <Button variante="principal" onClick={() => setModale(true)}>
                {<><Icone nom="plus" taille={18} /> Envoyer une notification</>}
              </Button>
            </RoleGate>
          </>
        }
      />

      {peutGerer && (
        <div className="onglets" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={vue === 'mes'}
            className={`onglet${vue === 'mes' ? ' onglet--actif' : ''}`}
            onClick={() => setVue('mes')}
          >
            Mes notifications
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={vue === 'toutes'}
            className={`onglet${vue === 'toutes' ? ' onglet--actif' : ''}`}
            onClick={() => setVue('toutes')}
          >
            Toutes les notifications
          </button>
        </div>
      )}

      {vue === 'mes' && !utilisateurId && (
        <Alert type="info" titre="Notifications personnelles indisponibles">
          Votre identifiant utilisateur n'a pas pu être déterminé : l'API ne fournit cette liste
          que par identifiant numérique, reconstitué à partir de vos adhésions. Rejoignez une
          tontine{peutGerer ? ' ou consultez l\'onglet « Toutes les notifications »' : ''} pour
          voir vos notifications.
        </Alert>
      )}

      <div className="barre-filtres">
        <label className="rangee" style={{ gap: 6, fontSize: '0.85rem', fontWeight: 600 }}>
          <input
            type="checkbox"
            checked={seulementNonLues}
            onChange={(e) => setSeulementNonLues(e.target.checked)}
            style={{ width: 'auto' }}
          />
          Afficher uniquement les non lues
        </label>
        <Button onClick={recharger}>{<><Icone nom="actualiser" taille={18} /> Actualiser</>}</Button>
      </div>

      <div className="carte">
        <Table
          legende="Liste des notifications"
          chargement={chargement}
          erreur={erreur}
          donnees={notifications}
          classeLigne={(n) => (n.statut === 'ENVOYEE' ? 'ligne-non-lue' : undefined)}
          colonnes={[
            {
              cle: 'type',
              entete: 'Type',
              rendu: (n) => <span className="cellule-principale">{n.type}</span>,
            },
            {
              cle: 'message',
              entete: 'Message',
              rendu: (n) => n.message,
            },
            ...(vue === 'toutes'
              ? [{ cle: 'destinataire', entete: 'Destinataire' }]
              : []),
            { cle: 'canal', entete: 'Canal' },
            {
              cle: 'dateEnvoi',
              entete: 'Reçue le',
              rendu: (n) => formaterDateHeure(n.dateEnvoi),
            },
            { cle: 'statut', entete: 'Statut', rendu: (n) => <Badge statut={n.statut} /> },
            {
              cle: 'actions',
              entete: 'Actions',
              align: 'actions',
              rendu: (n) =>
                n.statut !== 'LUE' ? (
                  <Button
                    taille="petit"
                    onClick={() => marquerLue(n)}
                    chargement={enCours === n.id}
                  >
                    Marquer comme lue
                  </Button>
                ) : (
                  <span className="texte-discret">—</span>
                ),
            },
          ]}
          vide={
            <EmptyState
              icone="notifications"
              titre={seulementNonLues ? 'Aucune notification non lue' : 'Aucune notification'}
              texte="Les confirmations de paiement génèrent automatiquement une notification."
            />
          }
        />
      </div>

      <Modal
        ouverte={modale}
        titre="Envoyer une notification"
        onFermer={() => setModale(false)}
        pied={
          <>
            <Button onClick={() => setModale(false)} disabled={envoi}>Annuler</Button>
            <Button
              variante="principal"
              onClick={envoyer}
              chargement={envoi}
              disabled={!formulaire.utilisateurId || !formulaire.message.trim()}
            >
              Envoyer
            </Button>
          </>
        }
      >
        <form onSubmit={envoyer} noValidate>
          {erreurFormulaire && (
            <Alert type="erreur" champs={erreurFormulaire.champs}>
              {erreurFormulaire.message}
            </Alert>
          )}

          <Field
            label="Destinataire (identifiant utilisateur)"
            nom="utilisateurId"
            type="number"
            min="1"
            list="annuaire-notifications"
            valeur={formulaire.utilisateurId}
            onChange={(e) => setFormulaire((f) => ({ ...f, utilisateurId: e.target.value }))}
            erreur={erreurFormulaire?.champs?.utilisateurId}
            aide="Identifiant numérique du compte destinataire (l'API n'expose pas d'annuaire)."
            requis
          />
          <datalist id="annuaire-notifications">
            {utilisateurs.map((u) => (
              <option key={u.id} value={u.id}>{`${u.nomComplet} — ${u.email}`}</option>
            ))}
          </datalist>

          <div className="grille-champs">
            <Field
              label="Type"
              nom="type"
              valeur={formulaire.type}
              onChange={(e) => setFormulaire((f) => ({ ...f, type: e.target.value }))}
              erreur={erreurFormulaire?.champs?.type}
              aide="Ex. RAPPEL_COTISATION, INFORMATION."
              maxLength={50}
              requis
            />
            <Field
              label="Canal"
              nom="canal"
              type="select"
              valeur={formulaire.canal}
              onChange={(e) => setFormulaire((f) => ({ ...f, canal: e.target.value }))}
              erreur={erreurFormulaire?.champs?.canal}
              options={CANAUX_NOTIFICATION}
              aide="Seul l'e-mail est réellement transmis ; SMS et push sont historisés."
            />
          </div>

          <Field
            label="Message"
            nom="message"
            type="textarea"
            valeur={formulaire.message}
            onChange={(e) => setFormulaire((f) => ({ ...f, message: e.target.value }))}
            erreur={erreurFormulaire?.champs?.message}
            maxLength={1000}
            requis
          />
        </form>
      </Modal>
    </>
  )
}
