import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { tontinesApi } from '../api/tontines.js'
import { useAuth } from '../auth/AuthContext.jsx'
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
import { formaterDate, formaterMontant } from '../utils/format.js'
import { PERIODICITES, ROLES, ROLES_ACTION, STATUTS_TONTINE } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

const FORMULAIRE_VIDE = {
  nom: '',
  description: '',
  montantCotisation: '',
  periodicite: 'MENSUELLE',
  nombreMembres: '',
  dateDebut: new Date().toISOString().slice(0, 10),
  statut: 'ACTIVE',
}

/** Liste des tontines : consultation pour tous, creation ouverte a tout compte. */
export default function Tontines() {
  const { aRole } = useAuth()
  const toast = useToast()
  const [filtreStatut, setFiltreStatut] = useState('')

  const charger = useCallback(
    () => tontinesApi.lister(filtreStatut ? { statut: filtreStatut } : {}),
    [filtreStatut],
  )
  const { donnees: tontines, chargement, erreur, recharger } = useRequete(charger, [filtreStatut], {
    valeurInitiale: [],
  })

  const [modale, setModale] = useState(null) // { mode: 'creation'|'edition', tontine }
  const [formulaire, setFormulaire] = useState(FORMULAIRE_VIDE)
  const [erreurFormulaire, setErreurFormulaire] = useState(null)
  const [envoi, setEnvoi] = useState(false)
  const [confirmation, setConfirmation] = useState(null) // { type, tontine }
  const [traitement, setTraitement] = useState(false)

  const ouvrirCreation = () => {
    setFormulaire(FORMULAIRE_VIDE)
    setErreurFormulaire(null)
    setModale({ mode: 'creation' })
  }

  const ouvrirEdition = (tontine) => {
    setFormulaire({
      nom: tontine.nom ?? '',
      description: tontine.description ?? '',
      montantCotisation: String(tontine.montantCotisation ?? ''),
      periodicite: tontine.periodicite ?? 'MENSUELLE',
      nombreMembres: String(tontine.nombreMembres ?? ''),
      dateDebut: tontine.dateDebut ?? '',
      statut: tontine.statut ?? 'ACTIVE',
    })
    setErreurFormulaire(null)
    setModale({ mode: 'edition', tontine })
  }

  const majChamp = (nom) => (evenement) =>
    setFormulaire((f) => ({ ...f, [nom]: evenement.target.value }))

  const enregistrer = async (evenement) => {
    evenement.preventDefault()
    setErreurFormulaire(null)
    setEnvoi(true)

    // Les champs numeriques sont convertis : le backend attend double / int.
    const charge = {
      nom: formulaire.nom.trim(),
      description: formulaire.description.trim(),
      montantCotisation: Number(formulaire.montantCotisation),
      periodicite: formulaire.periodicite,
      nombreMembres: Number(formulaire.nombreMembres),
      dateDebut: formulaire.dateDebut || null,
      statut: formulaire.statut,
    }

    try {
      if (modale.mode === 'creation') {
        await tontinesApi.creer(charge)
        toast.succes(`Tontine « ${charge.nom} » créée.`)
      } else {
        await tontinesApi.modifier(modale.tontine.id, charge)
        toast.succes(`Tontine « ${charge.nom} » mise à jour.`)
      }
      setModale(null)
      recharger()
    } catch (e) {
      setErreurFormulaire(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  const executerConfirmation = async () => {
    if (!confirmation) return
    setTraitement(true)
    try {
      if (confirmation.type === 'cloture') {
        await tontinesApi.cloturer(confirmation.tontine.id)
        toast.succes(`Tontine « ${confirmation.tontine.nom} » clôturée.`)
      } else {
        await tontinesApi.supprimer(confirmation.tontine.id)
        toast.succes(`Tontine « ${confirmation.tontine.nom} » supprimée.`)
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

  const peutGerer = aRole(ROLES_ACTION)

  return (
    <>
      <PageHeader
        titre="Tontines"
        sousTitre="Groupes d'épargne rotative : cotisations, membres et cycles."
        actions={
          <RoleGate roles={ROLES_ACTION}>
            <Button variante="principal" onClick={ouvrirCreation}>
              {<><Icone nom="plus" taille={18} /> Nouvelle tontine</>}
            </Button>
          </RoleGate>
        }
      />

      <div className="barre-filtres">
        <Field
          label="Filtrer par statut"
          nom="filtreStatut"
          type="select"
          valeur={filtreStatut}
          onChange={(e) => setFiltreStatut(e.target.value)}
          options={[{ valeur: '', libelle: 'Tous les statuts' }, ...STATUTS_TONTINE]}
        />
        <Button onClick={recharger}>{<><Icone nom="actualiser" taille={18} /> Actualiser</>}</Button>
      </div>

      <div className="carte">
        <Table
          legende="Liste des tontines"
          chargement={chargement}
          erreur={erreur}
          donnees={tontines}
          colonnes={[
            {
              cle: 'nom',
              entete: 'Nom',
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
              cle: 'periodicite',
              entete: 'Périodicité',
              rendu: (t) =>
                PERIODICITES.find((p) => p.valeur === t.periodicite)?.libelle || t.periodicite,
            },
            {
              cle: 'membres',
              entete: 'Membres',
              align: 'num',
              rendu: (t) => (
                <>
                  <div>{`${t.nombreMembresInscrits} / ${t.nombreMembres}`}</div>
                  <div className="cellule-secondaire">{t.nombreCycles} cycle(s)</div>
                </>
              ),
            },
            {
              cle: 'dateCreation',
              entete: 'Créée le',
              rendu: (t) => formaterDate(t.dateCreation),
            },
            {
              cle: 'statut',
              entete: 'Statut',
              rendu: (t) => <Badge statut={t.statut} />,
            },
            {
              cle: 'actions',
              entete: 'Actions',
              align: 'actions',
              rendu: (t) => (
                <div className="groupe-actions">
                  <Link className="btn btn--secondaire btn--petit" to={`/tontines/${t.id}`}>
                    Détail
                  </Link>
                  {t.administrateur && (
                    <>
                      <Button taille="petit" onClick={() => ouvrirEdition(t)}>
                        Modifier
                      </Button>
                      {t.statut !== 'CLOTUREE' && (
                        <Button
                          taille="petit"
                          onClick={() => setConfirmation({ type: 'cloture', tontine: t })}
                        >
                          Clôturer
                        </Button>
                      )}
                    </>
                  )}
                  <RoleGate roles={[ROLES.ADMINISTRATEUR]}>
                    <Button
                      taille="petit"
                      variante="danger"
                      onClick={() => setConfirmation({ type: 'suppression', tontine: t })}
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
              icone="tontines"
              titre="Aucune tontine"
              texte={
                peutGerer
                  ? 'Créez une première tontine pour commencer à enregistrer des membres.'
                  : "Aucune tontine n'est enregistrée pour le moment."
              }
              action={
                peutGerer ? (
                  <Button variante="principal" onClick={ouvrirCreation}>
                    {<><Icone nom="plus" taille={18} /> Nouvelle tontine</>}
                  </Button>
                ) : null
              }
            />
          }
        />
      </div>

      {/* Formulaire de creation / edition */}
      <Modal
        ouverte={Boolean(modale)}
        titre={modale?.mode === 'edition' ? 'Modifier la tontine' : 'Nouvelle tontine'}
        sousTitre="Les champs marqués d'une astérisque sont obligatoires."
        onFermer={() => setModale(null)}
        pied={
          <>
            <Button onClick={() => setModale(null)} disabled={envoi}>
              Annuler
            </Button>
            <Button variante="principal" onClick={enregistrer} chargement={envoi}>
              {modale?.mode === 'edition' ? 'Enregistrer' : 'Créer la tontine'}
            </Button>
          </>
        }
      >
        <form id="formulaire-tontine" onSubmit={enregistrer} noValidate>
          {erreurFormulaire && (
            <Alert type="erreur" champs={erreurFormulaire.champs}>
              {erreurFormulaire.message}
            </Alert>
          )}

          <Field
            label="Nom de la tontine"
            nom="nom"
            valeur={formulaire.nom}
            onChange={majChamp('nom')}
            erreur={erreurFormulaire?.champs?.nom}
            placeholder="Tontine des commerçantes de Sandaga"
            maxLength={100}
            requis
          />

          <Field
            label="Description"
            nom="description"
            type="textarea"
            valeur={formulaire.description}
            onChange={majChamp('description')}
            erreur={erreurFormulaire?.champs?.description}
            aide="500 caractères maximum."
            maxLength={500}
          />

          <div className="grille-champs">
            <Field
              label="Montant de la cotisation (FCFA)"
              nom="montantCotisation"
              type="number"
              min="1"
              step="500"
              valeur={formulaire.montantCotisation}
              onChange={majChamp('montantCotisation')}
              erreur={erreurFormulaire?.champs?.montantCotisation}
              placeholder="25000"
              requis
            />
            <Field
              label="Nombre de membres prévu"
              nom="nombreMembres"
              type="number"
              min="2"
              valeur={formulaire.nombreMembres}
              onChange={majChamp('nombreMembres')}
              erreur={erreurFormulaire?.champs?.nombreMembres}
              aide="2 membres au minimum."
              placeholder="10"
              requis
            />
          </div>

          <div className="grille-champs">
            <Field
              label="Périodicité"
              nom="periodicite"
              type="select"
              valeur={formulaire.periodicite}
              onChange={majChamp('periodicite')}
              erreur={erreurFormulaire?.champs?.periodicite}
              options={PERIODICITES}
              requis
            />
            <Field
              label="Date de début"
              nom="dateDebut"
              type="date"
              valeur={formulaire.dateDebut}
              onChange={majChamp('dateDebut')}
              erreur={erreurFormulaire?.champs?.dateDebut}
              aide="Point de départ du premier cycle de cotisation."
            />
          </div>

          <div className="grille-champs">
            <Field
              label="Statut"
              nom="statut"
              type="select"
              valeur={formulaire.statut}
              onChange={majChamp('statut')}
              erreur={erreurFormulaire?.champs?.statut}
              options={STATUTS_TONTINE}
            />
          </div>
        </form>
      </Modal>

      {/* Confirmations des actions sensibles */}
      <ConfirmDialog
        ouverte={Boolean(confirmation)}
        titre={
          confirmation?.type === 'cloture' ? 'Clôturer la tontine' : 'Supprimer la tontine'
        }
        message={
          confirmation?.type === 'cloture'
            ? `Clôturer définitivement « ${confirmation?.tontine?.nom} » ? Aucune nouvelle cotisation ne pourra y être rattachée.`
            : `Supprimer « ${confirmation?.tontine?.nom} » ? Cette action est irréversible.`
        }
        detail={
          confirmation?.type === 'suppression'
            ? 'La suppression est refusée par le serveur si des membres ou des cycles y sont rattachés.'
            : undefined
        }
        libelleConfirmation={confirmation?.type === 'cloture' ? 'Clôturer' : 'Supprimer'}
        dangereux={confirmation?.type === 'suppression'}
        chargement={traitement}
        onConfirmer={executerConfirmation}
        onAnnuler={() => setConfirmation(null)}
      />
    </>
  )
}
