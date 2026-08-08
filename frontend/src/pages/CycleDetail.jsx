import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cyclesApi } from '../api/cycles.js'
import { membresApi } from '../api/membres.js'
import { cotisationsApi } from '../api/cotisations.js'
import RoleGate from '../auth/RoleGate.jsx'
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
import { ROLES_ACTION } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

/** Fiche d'un cycle : periode, beneficiaire et cotisations rattachees. */
export default function CycleDetail() {
  const { id } = useParams()
  const toast = useToast()

  const chargerTout = useCallback(async () => {
    const cycle = await cyclesApi.obtenir(id)
    const [cotisations, membres] = await Promise.all([
      cyclesApi.listerCotisations(id),
      membresApi.lister({ tontineId: cycle.tontineId }),
    ])
    return { cycle, cotisations, membres }
  }, [id])

  const { donnees, chargement, erreur, recharger } = useRequete(chargerTout, [id])

  const [modaleCotisation, setModaleCotisation] = useState(false)
  const [formulaire, setFormulaire] = useState({ membreId: '', montant: '' })
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

  const { cycle, cotisations, membres } = donnees

  const membresActifs = membres.filter((m) => m.statut === 'ACTIF')
  const idsAyantCotise = new Set(cotisations.map((c) => c.membreId))
  const membresSansCotisation = membresActifs.filter((m) => !idsAyantCotise.has(m.id))
  const payees = cotisations.filter((c) => c.statut === 'PAYEE')

  const enregistrerCotisation = async (evenement) => {
    evenement.preventDefault()
    setErreurFormulaire(null)
    setEnvoi(true)
    try {
      await cotisationsApi.creer({
        cycleId: Number(id),
        membreId: Number(formulaire.membreId),
        // Vide = montant de cotisation de la tontine (valeur par defaut du backend).
        montant: formulaire.montant ? Number(formulaire.montant) : null,
      })
      toast.succes('Cotisation enregistrée. Elle peut désormais être réglée.')
      setModaleCotisation(false)
      setFormulaire({ membreId: '', montant: '' })
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
              disabled={cycle.statut === 'CLOTURE'}
              title={cycle.statut === 'CLOTURE' ? 'Le cycle est clôturé.' : undefined}
            >
              {<><Icone nom="plus" taille={18} /> Enregistrer une cotisation</>}
            </Button>
            <RoleGate roles={ROLES_ACTION}>
              {cycle.statut !== 'CLOTURE' && (
                <Button onClick={() => setConfirmation(true)}>Clôturer le cycle</Button>
              )}
            </RoleGate>
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
                cycle.statut !== 'CLOTURE' ? (
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
              disabled={!formulaire.membreId}
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

          <Field
            label="Membre"
            nom="membreId"
            type="select"
            valeur={formulaire.membreId}
            onChange={(e) => setFormulaire((f) => ({ ...f, membreId: e.target.value }))}
            erreur={erreurFormulaire?.champs?.membreId}
            options={[
              { valeur: '', libelle: '— Choisir un membre —' },
              ...membresActifs.map((m) => ({
                valeur: String(m.id),
                libelle: idsAyantCotise.has(m.id)
                  ? `${m.nomComplet} (a déjà cotisé)`
                  : m.nomComplet,
              })),
            ]}
            aide="Seuls les membres actifs de la tontine peuvent cotiser."
            requis
          />

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
