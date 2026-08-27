import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cyclesApi } from '../api/cycles.js'
import { membresApi } from '../api/membres.js'
import { cotisationsApi } from '../api/cotisations.js'
import { tontinesApi } from '../api/tontines.js'
import { useAuth } from '../auth/AuthContext.jsx'
import useRequete from '../hooks/useRequete.js'
import { useToast } from '../components/Toast.jsx'
import { normaliserErreur } from '../utils/errors.js'
import PageHeader, { InfoItem, StatCard } from '../components/PageHeader.jsx'
import Table from '../components/Table.jsx'
import Badge from '../components/Badge.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Alert from '../components/Alert.jsx'
import Loader from '../components/Loader.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Modal, { ConfirmDialog } from '../components/Modal.jsx'
import { formaterDate, formaterDateHeure, formaterMontant } from '../utils/format.js'
import Icone from '../components/Icone.jsx'

/** Fiche d'un cycle : periode, beneficiaire et cotisations rattachees. */
export default function CycleDetail() {
  const { id } = useParams()
  const toast = useToast()
  const { utilisateurId } = useAuth()

  const chargerTout = useCallback(async () => {
    const cycle = await cyclesApi.obtenir(id)
    const [cotisations, membres, tontine] = await Promise.all([
      cyclesApi.listerCotisations(id),
      membresApi.lister({ tontineId: cycle.tontineId }),
      tontinesApi.obtenir(cycle.tontineId),
    ])
    return { cycle, cotisations, membres, tontine }
  }, [id])

  const { donnees, chargement, erreur, recharger } = useRequete(chargerTout, [id])

  const [modaleCotisation, setModaleCotisation] = useState(false)
  const [formulaire, setFormulaire] = useState({ montant: '' })
  const [erreurFormulaire, setErreurFormulaire] = useState(null)
  const [envoi, setEnvoi] = useState(false)
  const [confirmation, setConfirmation] = useState(false)
  const [traitement, setTraitement] = useState(false)

  if (chargement) return <Loader message="Chargement du cycle..." />
  if (erreur) {
    return (
      <>
        <Alert type="erreur">{erreur}</Alert>
        <Link className="btn btn--secondaire" to="/cycles">{<><Icone nom="chevronGauche" taille={16} /> Retour aux cycles</>}</Link>
      </>
    )
  }

  const { cycle, cotisations, membres, tontine } = donnees
  // Droits de gestion propres a la tontine de ce cycle (voir TontineResponse.administrateur).
  const peutGerer = tontine.administrateur

  const membresActifs = membres.filter((m) => m.statut === 'ACTIF')
  const idsAyantCotise = new Set(cotisations.map((c) => c.membreId))
  const membresSansCotisation = membresActifs.filter((m) => !idsAyantCotise.has(m.id))
  const payees = cotisations.filter((c) => c.statut === 'PAYEE')

  // Le membre ne se choisit plus dans une liste : c'est la fiche membre de
  // l'utilisateur connecte pour la tontine de ce cycle, identifiee automatiquement.
  const monAdhesion = membres.find((m) => m.utilisateurId === utilisateurId) ?? null
  const dejaCotise = monAdhesion ? idsAyantCotise.has(monAdhesion.id) : false
  const peutCotiser = Boolean(monAdhesion) && monAdhesion.statut === 'ACTIF' && !dejaCotise

  const enregistrerCotisation = async (evenement) => {
    evenement.preventDefault()
    if (!peutCotiser) return
    setErreurFormulaire(null)
    setEnvoi(true)
    try {
      await cotisationsApi.creer({
        cycleId: Number(id),
        membreId: monAdhesion.id,
        // Vide = montant de cotisation de la tontine (valeur par defaut du backend).
        montant: formulaire.montant ? Number(formulaire.montant) : null,
      })
      toast.succes('Cotisation enregistrée. Elle peut désormais être réglée.')
      setModaleCotisation(false)
      setFormulaire({ montant: '' })
      recharger()
    } catch (e) {
      setErreurFormulaire(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  const cloturer = async () => {
    setTraitement(true)
    try {
      await cyclesApi.cloturer(id)
      toast.succes(`Cycle n°${cycle.numero} clôturé.`)
      setConfirmation(false)
      recharger()
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
      setConfirmation(false)
    } finally {
      setTraitement(false)
    }
  }

  return (
    <>
      <PageHeader
        filAriane={
          <>
            <Link to="/cycles">{<><Icone nom="chevronGauche" taille={14} /> Cycles</>}</Link>
            {' · '}
            <Link to={`/tontines/${cycle.tontineId}`}>{cycle.tontineNom}</Link>
          </>
        }
        titre={`Cycle n°${cycle.numero}`}
        sousTitre={`${formaterDate(cycle.dateDebut)} → ${formaterDate(cycle.dateFin)}`}
        actions={
          <>
            <Button
              variante="principal"
              onClick={() => setModaleCotisation(true)}
              disabled={cycle.statut === 'CLOTURE' || !peutCotiser}
              title={cycle.statut === 'CLOTURE' ? 'Le cycle est clôturé.' : undefined}
            >
              {<><Icone nom="plus" taille={18} /> Enregistrer une cotisation</>}
            </Button>
            {peutGerer && cycle.statut !== 'CLOTURE' && (
              <Button onClick={() => setConfirmation(true)}>Clôturer le cycle</Button>
            )}
          </>
        }
      />

      <div className="grille grille--stats">
        <StatCard
          libelle="Montant collecté"
          valeur={formaterMontant(cycle.montantCollecte)}
          detail={`${payees.length} cotisation(s) payée(s)`}
        />
        <StatCard
          libelle="Cotisations enregistrées"
          valeur={`${cotisations.length} / ${membresActifs.length}`}
          detail={`${membresSansCotisation.length} membre(s) sans cotisation`}
        />
        <StatCard libelle="Bénéficiaire" valeur={cycle.beneficiaireNom || 'Non désigné'} />
      </div>

      <div className="carte mt-16">
        <div className="carte__corps">
          <div className="liste-infos">
            <InfoItem cle="Statut"><Badge statut={cycle.statut} /></InfoItem>
            <InfoItem cle="Tontine">
              <Link to={`/tontines/${cycle.tontineId}`}>{cycle.tontineNom}</Link>
            </InfoItem>
            <InfoItem cle="Date de début">{formaterDate(cycle.dateDebut)}</InfoItem>
            <InfoItem cle="Date de fin">{formaterDate(cycle.dateFin)}</InfoItem>
          </div>
        </div>
      </div>

      {cycle.statut !== 'CLOTURE' && membresSansCotisation.length > 0 && (
        <Alert type="attention" titre="Cotisations manquantes">
          {membresSansCotisation.length} membre(s) actif(s) n'ont pas encore de cotisation sur ce
          cycle : {membresSansCotisation.map((m) => m.nomComplet).join(', ')}. La clôture restera
          refusée tant que toutes les cotisations ne sont pas réglées.
        </Alert>
      )}

      <section className="carte mt-16">
        <div className="carte__entete">
          <h2>Cotisations du cycle</h2>
        </div>
        <Table
          legende="Cotisations rattachées au cycle"
          donnees={cotisations}
          colonnes={[
            {
              cle: 'membreNom',
              entete: 'Membre',
              rendu: (c) => <span className="cellule-principale">{c.membreNom}</span>,
            },
            {
              cle: 'montant',
              entete: 'Montant',
              align: 'num',
              rendu: (c) => formaterMontant(c.montant),
            },
            {
              cle: 'date',
              entete: 'Enregistrée le',
              rendu: (c) => formaterDateHeure(c.date),
            },
            { cle: 'statut', entete: 'Statut', rendu: (c) => <Badge statut={c.statut} /> },
            {
              cle: 'actions',
              entete: 'Actions',
              align: 'actions',
              rendu: (c) =>
                c.statut === 'PAYEE' ? (
                  <Link className="btn btn--secondaire btn--petit" to="/recus">
                    Voir le reçu
                  </Link>
                ) : (
                  <Link
                    className="btn btn--principal btn--petit"
                    to={`/paiements?cotisationId=${c.id}`}
                  >
                    Payer
                  </Link>
                ),
            },
          ]}
          vide={
            <EmptyState
              icone="cotisations"
              titre="Aucune cotisation"
              texte="Enregistrez les cotisations des membres pour ce cycle."
              action={
                cycle.statut !== 'CLOTURE' && peutCotiser ? (
                  <Button variante="principal" onClick={() => setModaleCotisation(true)}>
                    {<><Icone nom="plus" taille={18} /> Enregistrer une cotisation</>}
                  </Button>
                ) : null
              }
            />
          }
        />
      </section>

      <Modal
        ouverte={modaleCotisation}
        titre="Enregistrer une cotisation"
        sousTitre={`Cycle n°${cycle.numero} — ${cycle.tontineNom}`}
        onFermer={() => setModaleCotisation(false)}
        pied={
          <>
            <Button onClick={() => setModaleCotisation(false)} disabled={envoi}>Annuler</Button>
            <Button
              variante="principal"
              onClick={enregistrerCotisation}
              chargement={envoi}
              disabled={!peutCotiser}
            >
              Enregistrer
            </Button>
          </>
        }
      >
        <form onSubmit={enregistrerCotisation} noValidate>
          {erreurFormulaire && (
            <Alert type="erreur" champs={erreurFormulaire.champs}>
              {erreurFormulaire.message}
            </Alert>
          )}

          <InfoItem cle="Membre">
            {monAdhesion ? monAdhesion.nomComplet : '—'}
          </InfoItem>

          {!monAdhesion && (
            <Alert type="attention">
              Vous n'êtes pas membre de cette tontine : seul un de ses membres peut y
              enregistrer une cotisation, pour son propre compte.
            </Alert>
          )}
          {monAdhesion && monAdhesion.statut !== 'ACTIF' && (
            <Alert type="attention">Votre adhésion est suspendue : vous ne pouvez pas cotiser.</Alert>
          )}
          {monAdhesion && dejaCotise && (
            <Alert type="attention">Vous avez déjà enregistré une cotisation sur ce cycle.</Alert>
          )}

          <Field
            label="Montant (FCFA)"
            nom="montant"
            type="number"
            min="1"
            step="500"
            valeur={formulaire.montant}
            onChange={(e) => setFormulaire((f) => ({ ...f, montant: e.target.value }))}
            erreur={erreurFormulaire?.champs?.montant}
            aide="Laissez vide pour appliquer le montant de la tontine."
          />
        </form>
      </Modal>

      <ConfirmDialog
        ouverte={confirmation}
        titre="Clôturer le cycle"
        message={`Clôturer le cycle n°${cycle.numero} ?`}
        detail="Le serveur refuse la clôture tant que tous les membres actifs n'ont pas une cotisation payée."
        libelleConfirmation="Clôturer"
        chargement={traitement}
        onConfirmer={cloturer}
        onAnnuler={() => setConfirmation(false)}
      />
    </>
  )
}
