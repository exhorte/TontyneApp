import { useCallback, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { cotisationsApi } from '../api/cotisations.js'
import { cyclesApi } from '../api/cycles.js'
import { tontinesApi } from '../api/tontines.js'
import { membresApi } from '../api/membres.js'
import RoleGate from '../auth/RoleGate.jsx'
import useRequete from '../hooks/useRequete.js'
import { useToast } from '../components/Toast.jsx'
import { normaliserErreur } from '../utils/errors.js'
import PageHeader from '../components/PageHeader.jsx'
import Table from '../components/Table.jsx'
import Badge from '../components/Badge.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Alert from '../components/Alert.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Modal, { ConfirmDialog } from '../components/Modal.jsx'
import { formaterDateHeure, formaterMontant } from '../utils/format.js'
import { ROLES_ACTION } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

/**
 * Cotisations : suivi et enregistrement.
 * La creation est ouverte a tout utilisateur authentifie (POST /cotisations).
 */
export default function Cotisations() {
  const [parametres, setParametres] = useSearchParams()
  const tontineId = parametres.get('tontineId') || ''
  const cycleId = parametres.get('cycleId') || ''
  const membreId = parametres.get('membreId') || ''
  const toast = useToast()

  const { donnees: tontines } = useRequete(() => tontinesApi.lister(), [], { valeurInitiale: [] })

  // Le backend applique un seul filtre, dans l'ordre cycleId > membreId > tontineId.
  const charger = useCallback(() => {
    if (cycleId) return cotisationsApi.lister({ cycleId })
    if (membreId) return cotisationsApi.lister({ membreId })
    if (tontineId) return cotisationsApi.lister({ tontineId })
    return cotisationsApi.lister()
  }, [cycleId, membreId, tontineId])

  const { donnees: cotisations, chargement, erreur, recharger } = useRequete(
    charger,
    [cycleId, membreId, tontineId],
    { valeurInitiale: [] },
  )

  const majFiltre = (cle, valeur) => {
    const suivants = new URLSearchParams(parametres)
    // Les filtres sont exclusifs cote backend : on ne garde que le dernier choisi.
    suivants.delete('tontineId')
    suivants.delete('cycleId')
    suivants.delete('membreId')
    if (valeur) suivants.set(cle, valeur)
    setParametres(suivants, { replace: true })
  }

  // --- Enregistrement d'une cotisation ------------------------------------
  const [modale, setModale] = useState(false)
  const [formulaire, setFormulaire] = useState({ tontineId: '', cycleId: '', membreId: '', montant: '' })
  const [erreurFormulaire, setErreurFormulaire] = useState(null)
  const [envoi, setEnvoi] = useState(false)

  const { donnees: cyclesTontine } = useRequete(
    () => (formulaire.tontineId ? cyclesApi.lister({ tontineId: formulaire.tontineId }) : []),
    [formulaire.tontineId],
    { valeurInitiale: [], actif: Boolean(formulaire.tontineId) },
  )
  const { donnees: membresTontine } = useRequete(
    () => (formulaire.tontineId ? membresApi.lister({ tontineId: formulaire.tontineId }) : []),
    [formulaire.tontineId],
    { valeurInitiale: [], actif: Boolean(formulaire.tontineId) },
  )

  const ouvrirCreation = () => {
    setFormulaire({ tontineId: tontineId || '', cycleId: '', membreId: '', montant: '' })
    setErreurFormulaire(null)
    setModale(true)
  }

  const enregistrer = async (evenement) => {
    evenement.preventDefault()
    setErreurFormulaire(null)
    setEnvoi(true)
    try {
      const cotisation = await cotisationsApi.creer({
        cycleId: Number(formulaire.cycleId),
        membreId: Number(formulaire.membreId),
        montant: formulaire.montant ? Number(formulaire.montant) : null,
      })
      toast.succes(
        `Cotisation de ${formaterMontant(cotisation.montant)} enregistrée pour ${cotisation.membreNom}.`,
      )
      setModale(false)
      recharger()
    } catch (e) {
      setErreurFormulaire(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  // --- Suppression ---------------------------------------------------------
  const [confirmation, setConfirmation] = useState(null)
  const [traitement, setTraitement] = useState(false)

  const supprimer = async () => {
    setTraitement(true)
    try {
      await cotisationsApi.supprimer(confirmation.id)
      toast.succes('Cotisation supprimée.')
      setConfirmation(null)
      recharger()
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
      setConfirmation(null)
    } finally {
      setTraitement(false)
    }
  }

  const enAttente = (cotisations ?? []).filter((c) => c.statut !== 'PAYEE')

  return (
    <>
      <PageHeader
        titre="Cotisations"
        sousTitre={`${cotisations?.length ?? 0} cotisation(s), dont ${enAttente.length} en attente de règlement.`}
        actions={
          <Button variante="principal" onClick={ouvrirCreation}>
            {<><Icone nom="plus" taille={18} /> Enregistrer une cotisation</>}
          </Button>
        }
      />

      <div className="barre-filtres">
        <Field
          label="Filtrer par tontine"
          nom="filtreTontine"
          type="select"
          valeur={tontineId}
          onChange={(e) => majFiltre('tontineId', e.target.value)}
          options={[
            { valeur: '', libelle: 'Toutes les tontines' },
            ...(tontines ?? []).map((t) => ({ valeur: String(t.id), libelle: t.nom })),
          ]}
        />
        {(cycleId || membreId) && (
          <Button onClick={() => setParametres(new URLSearchParams(), { replace: true })}>
            {<><Icone nom="fermer" taille={14} /> </>}Retirer le filtre {cycleId ? `cycle #${cycleId}` : `membre #${membreId}`}
          </Button>
        )}
        <Button onClick={recharger}>{<><Icone nom="actualiser" taille={18} /> Actualiser</>}</Button>
      </div>

      <div className="carte">
        <Table
          legende="Liste des cotisations"
          chargement={chargement}
          erreur={erreur}
          donnees={cotisations}
          colonnes={[
            {
              cle: 'membreNom',
              entete: 'Membre',
              rendu: (c) => (
                <>
                  <div className="cellule-principale">{c.membreNom}</div>
                  <div className="cellule-secondaire">{c.tontineNom}</div>
                </>
              ),
            },
            {
              cle: 'cycleNumero',
              entete: 'Cycle',
              rendu: (c) => <Link to={`/cycles/${c.cycleId}`}>Cycle n°{c.cycleNumero}</Link>,
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
              rendu: (c) => (
                <div className="groupe-actions">
                  {c.statut === 'PAYEE' ? (
                    <Link className="btn btn--secondaire btn--petit" to="/paiements">
                      Voir le paiement
                    </Link>
                  ) : (
                    <Link
                      className="btn btn--principal btn--petit"
                      to={`/paiements?cotisationId=${c.id}`}
                    >
                      Payer
                    </Link>
                  )}
                  <RoleGate roles={ROLES_ACTION}>
                    <Button
                      taille="petit"
                      variante="danger"
                      onClick={() => setConfirmation(c)}
                    >
                      Supprimer
                    </Button>
                  </RoleGate>
                </div>
              ),
            },
          ]}
          vide={
            <EmptyState
              icone="cotisations"
              titre="Aucune cotisation"
              texte="Enregistrez une cotisation pour un membre sur un cycle en cours."
              action={
                <Button variante="principal" onClick={ouvrirCreation}>
                  {<><Icone nom="plus" taille={18} /> Enregistrer une cotisation</>}
                </Button>
              }
            />
          }
        />
      </div>

      <Modal
        ouverte={modale}
        titre="Enregistrer une cotisation"
        sousTitre="Étape 1 du parcours : cotisation → paiement → reçu."
        onFermer={() => setModale(false)}
        pied={
          <>
            <Button onClick={() => setModale(false)} disabled={envoi}>Annuler</Button>
            <Button
              variante="principal"
              onClick={enregistrer}
              chargement={envoi}
              disabled={!formulaire.cycleId || !formulaire.membreId}
            >
              Enregistrer
            </Button>
          </>
        }
      >
        <form onSubmit={enregistrer} noValidate>
          {erreurFormulaire && (
            <Alert type="erreur" champs={erreurFormulaire.champs}>
              {erreurFormulaire.message}
            </Alert>
          )}

          <Field
            label="Tontine"
            nom="tontineIdFormulaire"
            type="select"
            valeur={formulaire.tontineId}
            onChange={(e) =>
              setFormulaire({ tontineId: e.target.value, cycleId: '', membreId: '', montant: '' })
            }
            options={[
              { valeur: '', libelle: '— Choisir une tontine —' },
              ...(tontines ?? []).map((t) => ({ valeur: String(t.id), libelle: t.nom })),
            ]}
            aide="Sert uniquement à filtrer les cycles et les membres ci-dessous."
            requis
          />

          <Field
            label="Cycle"
            nom="cycleId"
            type="select"
            valeur={formulaire.cycleId}
            onChange={(e) => setFormulaire((f) => ({ ...f, cycleId: e.target.value }))}
            erreur={erreurFormulaire?.champs?.cycleId}
            options={[
              { valeur: '', libelle: '— Choisir un cycle —' },
              ...(cyclesTontine ?? [])
                .filter((c) => c.statut !== 'CLOTURE')
                .map((c) => ({
                  valeur: String(c.id),
                  libelle: `Cycle n°${c.numero} (${c.statut === 'EN_COURS' ? 'en cours' : 'planifié'})`,
                })),
            ]}
            aide="Un cycle clôturé n'accepte plus de cotisation."
            disabled={!formulaire.tontineId}
            requis
          />

          <Field
            label="Membre"
            nom="membreId"
            type="select"
            valeur={formulaire.membreId}
            onChange={(e) => setFormulaire((f) => ({ ...f, membreId: e.target.value }))}
            erreur={erreurFormulaire?.champs?.membreId}
            options={[
              { valeur: '', libelle: '— Choisir un membre —' },
              ...(membresTontine ?? [])
                .filter((m) => m.statut === 'ACTIF')
                .map((m) => ({ valeur: String(m.id), libelle: m.nomComplet })),
            ]}
            disabled={!formulaire.tontineId}
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
            aide="Laissez vide pour appliquer le montant défini par la tontine."
          />
        </form>
      </Modal>

      <ConfirmDialog
        ouverte={Boolean(confirmation)}
        titre="Supprimer la cotisation"
        message={`Supprimer la cotisation de ${confirmation?.membreNom} (${formaterMontant(confirmation?.montant)}) ?`}
        detail="Le serveur refuse la suppression si un paiement y est rattaché."
        libelleConfirmation="Supprimer"
        dangereux
        chargement={traitement}
        onConfirmer={supprimer}
        onAnnuler={() => setConfirmation(null)}
      />
    </>
  )
}
