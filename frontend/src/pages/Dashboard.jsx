import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { tontinesApi } from '../api/tontines.js'
import { membresApi } from '../api/membres.js'
import { cotisationsApi } from '../api/cotisations.js'
import { notificationsApi } from '../api/notifications.js'
import useRequete from '../hooks/useRequete.js'
import PageHeader, { StatCard } from '../components/PageHeader.jsx'
import Table from '../components/Table.jsx'
import Badge from '../components/Badge.jsx'
import Alert from '../components/Alert.jsx'
import Loader from '../components/Loader.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Button from '../components/Button.jsx'
import { formaterDateHeure, formaterMontant } from '../utils/format.js'
import { ROLES_ACTION } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

/**
 * Synthese de la session : tontines, cotisations restant a regler, montant deja
 * verse et dernieres notifications.
 *
 * L'API ne fournit pas d'endpoint "/me" : quand l'identifiant numerique de
 * l'utilisateur n'a pas pu etre resolu (compte membre d'aucune tontine), on bascule
 * sur une vue globale pour les profils de gestion.
 */
export default function Dashboard() {
  const { utilisateur, utilisateurId, role } = useAuth()
  const estGestion = ROLES_ACTION.includes(role)

  const chargerSynthese = useCallback(async () => {
    // --- Cas 1 : l'utilisateur est rattache a au moins une tontine ---
    if (utilisateurId) {
      const [tontines, adhesions, notifications] = await Promise.all([
        tontinesApi.listerParUtilisateur(utilisateurId),
        membresApi.lister({ utilisateurId }),
        notificationsApi.listerParUtilisateur(utilisateurId),
      ])

      const cotisationsParAdhesion = await Promise.all(
        adhesions.map((membre) => membresApi.listerCotisations(membre.id)),
      )
      const cotisations = cotisationsParAdhesion.flat()

      return { tontines, adhesions, cotisations, notifications, personnel: true }
    }

    // --- Cas 2 : profil de gestion sans adhesion : vue globale ---
    if (estGestion) {
      const [tontines, cotisations, notifications] = await Promise.all([
        tontinesApi.lister(),
        cotisationsApi.lister(),
        notificationsApi.lister().catch(() => []),
      ])
      return { tontines, adhesions: [], cotisations, notifications, personnel: false }
    }

    // --- Cas 3 : simple membre pas encore inscrit a une tontine ---
    return { tontines: [], adhesions: [], cotisations: [], notifications: [], personnel: true }
  }, [utilisateurId, estGestion])

  const { donnees, chargement, erreur, recharger } = useRequete(chargerSynthese, [
    utilisateurId,
    estGestion,
  ])

  if (chargement) return <Loader message="Chargement du tableau de bord..." />

  const tontines = donnees?.tontines ?? []
  const cotisations = donnees?.cotisations ?? []
  const notifications = donnees?.notifications ?? []
  const personnel = donnees?.personnel ?? true

  const enAttente = cotisations.filter((c) => c.statut !== 'PAYEE')
  const payees = cotisations.filter((c) => c.statut === 'PAYEE')
  const totalVerse = payees.reduce((somme, c) => somme + (c.montant || 0), 0)
  const totalDu = enAttente.reduce((somme, c) => somme + (c.montant || 0), 0)
  const nonLues = notifications.filter((n) => n.statut === 'ENVOYEE')

  const prenom = utilisateur?.telephone ?? ''

  return (
    <>
      <PageHeader
        titre={`Bonjour ${prenom}`}
        sousTitre={
          personnel
            ? 'Voici la situation de vos tontines et de vos cotisations.'
            : "Vue d'ensemble de la plateforme (vous n'êtes membre d'aucune tontine)."
        }
        actions={
          <Button onClick={recharger} variante="secondaire">
            {<><Icone nom="actualiser" taille={18} /> Actualiser</>}
          </Button>
        }
      />

      {erreur && <Alert type="erreur">{erreur}</Alert>}

      {!utilisateurId && !estGestion && (
        <Alert type="info" titre="Aucune adhésion pour le moment">
          Vous n'êtes membre d'aucune tontine. Créez-en une ou demandez à en rejoindre une
          pour que vous puissiez cotiser.
        </Alert>
      )}

      <div className="grille grille--stats">
        <StatCard
          libelle={personnel ? 'Mes tontines' : 'Tontines'}
          valeur={tontines.length}
          detail={`${tontines.filter((t) => t.statut === 'ACTIVE').length} active(s)`}
        />
        <StatCard
          libelle="Cotisations à régler"
          valeur={enAttente.length}
          detail={formaterMontant(totalDu)}
        />
        <StatCard
          libelle="Total déjà versé"
          valeur={formaterMontant(totalVerse)}
          detail={`${payees.length} cotisation(s) payée(s)`}
        />
        <StatCard
          libelle="Notifications non lues"
          valeur={nonLues.length}
          detail={`${notifications.length} au total`}
        />
      </div>

      <div className="grille grille--2 mt-16">
        {/* Cotisations restant a payer */}
        <section className="carte">
          <div className="carte__entete">
            <h2>Cotisations à régler</h2>
            <Link to="/cotisations">Tout voir</Link>
          </div>
          <Table
            legende="Cotisations en attente de paiement"
            colonnes={[
              {
                cle: 'tontine',
                entete: 'Tontine',
                rendu: (c) => (
                  <>
                    <div className="cellule-principale">{c.tontineNom}</div>
                    <div className="cellule-secondaire">Cycle n°{c.cycleNumero}</div>
                  </>
                ),
              },
              {
                cle: 'montant',
                entete: 'Montant',
                align: 'num',
                rendu: (c) => formaterMontant(c.montant),
              },
              {
                cle: 'action',
                entete: '',
                align: 'actions',
                rendu: (c) => (
                  <Link
                    className="btn btn--principal btn--petit"
                    to={`/paiements?cotisationId=${c.id}`}
                  >
                    Payer
                  </Link>
                ),
              },
            ]}
            donnees={enAttente.slice(0, 6)}
            vide={
              <EmptyState
                icone="succes"
                titre="Aucune cotisation en attente"
                texte="Toutes vos cotisations enregistrées sont réglées."
              />
            }
          />
        </section>

        {/* Dernieres notifications */}
        <section className="carte">
          <div className="carte__entete">
            <h2>Dernières notifications</h2>
            <Link to="/notifications">Tout voir</Link>
          </div>
          <Table
            legende="Dernières notifications reçues"
            classeLigne={(n) => (n.statut === 'ENVOYEE' ? 'ligne-non-lue' : undefined)}
            colonnes={[
              {
                cle: 'message',
                entete: 'Message',
                rendu: (n) => (
                  <>
                    <div className="cellule-principale">{n.type}</div>
                    <div className="cellule-secondaire">{n.message}</div>
                  </>
                ),
              },
              {
                cle: 'dateEnvoi',
                entete: 'Reçue le',
                rendu: (n) => (
                  <>
                    <div>{formaterDateHeure(n.dateEnvoi)}</div>
                    <Badge statut={n.statut} />
                  </>
                ),
              },
            ]}
            donnees={notifications.slice(0, 5)}
            vide={
              <EmptyState
                icone="notifications"
                titre="Aucune notification"
                texte="Les confirmations de paiement apparaîtront ici."
              />
            }
          />
        </section>
      </div>

      {/* Mes tontines */}
      <section className="carte mt-16">
        <div className="carte__entete">
          <h2>{personnel ? 'Mes tontines' : 'Tontines de la plateforme'}</h2>
          <Link to="/tontines">Tout voir</Link>
        </div>
        <Table
          legende="Liste des tontines"
          colonnes={[
            {
              cle: 'nom',
              entete: 'Tontine',
              rendu: (t) => (
                <>
                  <Link className="cellule-principale" to={`/tontines/${t.id}`}>
                    {t.nom}
                  </Link>
                  <div className="cellule-secondaire">{t.description || '—'}</div>
                </>
              ),
            },
            {
              cle: 'montantCotisation',
              entete: 'Cotisation',
              align: 'num',
              rendu: (t) => formaterMontant(t.montantCotisation),
            },
            {
              cle: 'membres',
              entete: 'Membres',
              align: 'num',
              rendu: (t) => `${t.nombreMembresInscrits} / ${t.nombreMembres}`,
            },
            {
              cle: 'nombreCycles',
              entete: 'Cycles',
              align: 'num',
            },
            {
              cle: 'statut',
              entete: 'Statut',
              rendu: (t) => <Badge statut={t.statut} />,
            },
          ]}
          donnees={tontines.slice(0, 5)}
          vide={
            <EmptyState
              icone="tontines"
              titre="Aucune tontine"
              texte={
                estGestion
                  ? 'Créez votre première tontine pour démarrer.'
                  : "Vous n'êtes membre d'aucune tontine pour le moment."
              }
              action={
                estGestion ? (
                  <Link className="btn btn--principal" to="/tontines">
                    Créer une tontine
                  </Link>
                ) : null
              }
            />
          }
        />
      </section>
    </>
  )
}
