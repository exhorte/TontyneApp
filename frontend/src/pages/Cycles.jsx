import { useCallback, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
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
import { dateDuJourIso, formaterDate, formaterMontant } from '../utils/format.js'
import { ROLES_ACTION, STATUTS_CYCLE } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

const FORMULAIRE_VIDE = {
  tontineId: '',
  numero: '',
  dateDebut: dateDuJourIso(),
  dateFin: '',
  beneficiaireId: '',
  statut: 'EN_COURS',
}

/** Cycles (tours) de toutes les tontines, filtrables par tontine. */
export default function Cycles() {
  const [parametres, setParametres] = useSearchParams()
  const tontineId = parametres.get('tontineId') || ''
  const toast = useToast()

  const { donnees: tontines } = useRequete(() => tontinesApi.lister(), [], { valeurInitiale: [] })

  const charger = useCallback(
    () => cyclesApi.lister(tontineId ? { tontineId } : {}),
    [tontineId],
  )
  const { donnees: cycles, chargement, erreur, recharger } = useRequete(charger, [tontineId], {
    valeurInitiale: [],
  })

  const majFiltre = (valeur) => {
    const suivants = new URLSearchParams(parametres)
    if (valeur) suivants.set('tontineId', valeur)
    else suivants.delete('tontineId')
    setParametres(suivants, { replace: true })
  }

  // --- Creation / edition d'un cycle --------------------------------------
  const [modale, setModale] = useState(null) // { mode, cycle }
  const [formulaire, setFormulaire] = useState(FORMULAIRE_VIDE)
  const [erreurFormulaire, setErreurFormulaire] = useState(null)
  const [envoi, setEnvoi] = useState(false)

  // Les beneficiaires possibles sont les membres de la tontine choisie.
  const { donnees: membresTontine } = useRequete(
    () => (formulaire.tontineId ? membresApi.lister({ tontineId: formulaire.tontineId }) : []),
    [formulaire.tontineId],
    { valeurInitiale: [], actif: Boolean(formulaire.tontineId) },
  )

  const ouvrirCreation = () => {
    setFormulaire({ ...FORMULAIRE_VIDE, tontineId: tontineId || '' })
    setErreurFormulaire(null)
    setModale({ mode: 'creation' })
  }

  const ouvrirEdition = (cycle) => {
    setFormulaire({
      tontineId: String(cycle.tontineId),
      numero: String(cycle.numero),
      dateDebut: cycle.dateDebut ?? dateDuJourIso(),
      dateFin: cycle.dateFin ?? '',
      beneficiaireId: cycle.beneficiaireId ? String(cycle.beneficiaireId) : '',
      statut: cycle.statut ?? 'EN_COURS',
    })
    setErreurFormulaire(null)
    setModale({ mode: 'edition', cycle })
  }

  const majChamp = (nom) => (e) => setFormulaire((f) => ({ ...f, [nom]: e.target.value }))

  const enregistrer = async (evenement) => {
    evenement.preventDefault()
    setErreurFormulaire(null)
    setEnvoi(true)

    const charge = {
      tontineId: Number(formulaire.tontineId),
      numero: Number(formulaire.numero),
      dateDebut: formulaire.dateDebut,
      // Champs facultatifs : on envoie null plutot qu'une chaine vide.
      dateFin: formulaire.dateFin || null,
      beneficiaireId: formulaire.beneficiaireId ? Number(formulaire.beneficiaireId) : null,
      statut: formulaire.statut,
    }

    try {
      if (modale.mode === 'creation') {
        await cyclesApi.creer(charge)
        toast.succes(`Cycle n°${charge.numero} créé.`)
      } else {
        await cyclesApi.modifier(modale.cycle.id, charge)
        toast.succes(`Cycle n°${charge.numero} mis à jour.`)
      }
      setModale(null)
      recharger()
    } catch (e) {
      setErreurFormulaire(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  // --- Actions sensibles --------------------------------------------------
  const [confirmation, setConfirmation] = useState(null)
  const [traitement, setTraitement] = useState(false)

  const executerConfirmation = async () => {
    if (!confirmation) return
    setTraitement(true)
    try {
      if (confirmation.type === 'cloturer') {
        await cyclesApi.cloturer(confirmation.cycle.id)
        toast.succes(`Cycle n°${confirmation.cycle.numero} clôturé.`)
      } else {
        await cyclesApi.supprimer(confirmation.cycle.id)
        toast.succes(`Cycle n°${confirmation.cycle.numero} supprimé.`)
      }
      setConfirmation(null)
      recharger()
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
      setConfirmation(null)
    } finally {
      setTraitement(false)
    }
  }

  return (
    <>
      <PageHeader
        titre="Cycles"
        sousTitre="Chaque cycle correspond au tour d'un membre bénéficiaire."
        actions={
          <RoleGate roles={ROLES_ACTION}>
            <Button variante="principal" onClick={ouvrirCreation}>{<><Icone nom="plus" taille={18} /> Nouveau cycle</>}</Button>
          </RoleGate>
        }
      />

      <div className="barre-filtres">
        <Field
          label="Filtrer par tontine"
          nom="filtreTontine"
          type="select"
          valeur={tontineId}
          onChange={(e) => majFiltre(e.target.value)}
          options={[
            { valeur: '', libelle: 'Toutes les tontines' },
            ...(tontines ?? []).map((t) => ({ valeur: String(t.id), libelle: t.nom })),
          ]}
        />
        <Button onClick={recharger}>{<><Icone nom="actualiser" taille={18} /> Actualiser</>}</Button>
      </div>

      <div className="carte">
        <Table
          legende="Liste des cycles"
          chargement={chargement}
          erreur={erreur}
          donnees={cycles}
          colonnes={[
            {
              cle: 'numero',
              entete: 'Cycle',
              rendu: (c) => (
                <>
                  <Link className="cellule-principale" to={`/cycles/${c.id}`}>
                    Cycle n°{c.numero}
                  </Link>
                  <div className="cellule-secondaire">{c.tontineNom}</div>
                </>
              ),
            },
            {
              cle: 'periode',
              entete: 'Période',
              rendu: (c) => `${formaterDate(c.dateDebut)} → ${formaterDate(c.dateFin)}`,
            },
            {
              cle: 'beneficiaireNom',
              entete: 'Bénéficiaire',
              rendu: (c) =>
                c.beneficiaireNom || <span className="texte-discret">Non désigné</span>,
            },
            {
              cle: 'montantCollecte',
              entete: 'Collecté',
              align: 'num',
              rendu: (c) => formaterMontant(c.montantCollecte),
            },
            { cle: 'statut', entete: 'Statut', rendu: (c) => <Badge statut={c.statut} /> },
            {
              cle: 'actions',
              entete: 'Actions',
              align: 'actions',
              rendu: (c) => (
                <div className="groupe-actions">
                  <Link className="btn btn--secondaire btn--petit" to={`/cycles/${c.id}`}>
                    Détail
                  </Link>
                  <RoleGate roles={ROLES_ACTION}>
                    <Button taille="petit" onClick={() => ouvrirEdition(c)}>Modifier</Button>
                    {c.statut !== 'CLOTURE' && (
                      <Button
                        taille="petit"
                        onClick={() => setConfirmation({ type: 'cloturer', cycle: c })}
                      >
                        Clôturer
                      </Button>
                    )}
                    <Button
                      taille="petit"
                      variante="danger"
                      onClick={() => setConfirmation({ type: 'supprimer', cycle: c })}
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
              icone="cycles"
              titre="Aucun cycle"
              texte="Générez les cycles depuis la fiche d'une tontine, ou créez-en un manuellement."
            />
          }
        />
      </div>

      <Modal
        ouverte={Boolean(modale)}
        titre={modale?.mode === 'edition' ? 'Modifier le cycle' : 'Nouveau cycle'}
        onFermer={() => setModale(null)}
        pied={
          <>
            <Button onClick={() => setModale(null)} disabled={envoi}>Annuler</Button>
            <Button
              variante="principal"
              onClick={enregistrer}
              chargement={envoi}
              disabled={!formulaire.tontineId || !formulaire.numero}
            >
              {modale?.mode === 'edition' ? 'Enregistrer' : 'Créer le cycle'}
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
            nom="tontineId"
            type="select"
            valeur={formulaire.tontineId}
            onChange={majChamp('tontineId')}
            erreur={erreurFormulaire?.champs?.tontineId}
            options={[
              { valeur: '', libelle: '— Choisir une tontine —' },
              ...(tontines ?? []).map((t) => ({ valeur: String(t.id), libelle: t.nom })),
            ]}
            disabled={modale?.mode === 'edition'}
            requis
          />

          <div className="grille-champs">
            <Field
              label="Numéro du cycle"
              nom="numero"
              type="number"
              min="1"
              valeur={formulaire.numero}
              onChange={majChamp('numero')}
              erreur={erreurFormulaire?.champs?.numero}
              requis
            />
            <Field
              label="Statut"
              nom="statut"
              type="select"
              valeur={formulaire.statut}
              onChange={majChamp('statut')}
              erreur={erreurFormulaire?.champs?.statut}
              options={STATUTS_CYCLE}
            />
          </div>

          <div className="grille-champs">
            <Field
              label="Date de début"
              nom="dateDebut"
              type="date"
              valeur={formulaire.dateDebut}
              onChange={majChamp('dateDebut')}
              erreur={erreurFormulaire?.champs?.dateDebut}
              requis
            />
            <Field
              label="Date de fin"
              nom="dateFin"
              type="date"
              valeur={formulaire.dateFin}
              onChange={majChamp('dateFin')}
              erreur={erreurFormulaire?.champs?.dateFin}
              aide="Vide = calculée selon la périodicité."
            />
          </div>

          <Field
            label="Bénéficiaire"
            nom="beneficiaireId"
            type="select"
            valeur={formulaire.beneficiaireId}
            onChange={majChamp('beneficiaireId')}
            erreur={erreurFormulaire?.champs?.beneficiaireId}
            options={[
              { valeur: '', libelle: '— Non désigné —' },
              ...(membresTontine ?? []).map((m) => ({
                valeur: String(m.id),
                libelle: `${m.nomComplet} (tour n°${m.ordreTour})`,
              })),
            ]}
            aide={
              formulaire.tontineId
                ? 'Le bénéficiaire doit appartenir à la tontine choisie.'
                : "Choisissez d'abord une tontine."
            }
            disabled={!formulaire.tontineId}
          />
        </form>
      </Modal>

      <ConfirmDialog
        ouverte={Boolean(confirmation)}
        titre={
          confirmation?.type === 'cloturer' ? 'Clôturer le cycle' : 'Supprimer le cycle'
        }
        message={
          confirmation?.type === 'cloturer'
            ? `Clôturer le cycle n°${confirmation?.cycle?.numero} ?`
            : `Supprimer le cycle n°${confirmation?.cycle?.numero} ?`
        }
        detail={
          confirmation?.type === 'cloturer'
            ? "La clôture est refusée tant que tous les membres actifs n'ont pas réglé leur cotisation."
            : 'Refusé par le serveur si des cotisations y sont rattachées.'
        }
        libelleConfirmation={confirmation?.type === 'cloturer' ? 'Clôturer' : 'Supprimer'}
        dangereux={confirmation?.type === 'supprimer'}
        chargement={traitement}
        onConfirmer={executerConfirmation}
        onAnnuler={() => setConfirmation(null)}
      />
    </>
  )
}
