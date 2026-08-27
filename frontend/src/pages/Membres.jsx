import { useCallback, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { membresApi } from '../api/membres.js'
import { tontinesApi } from '../api/tontines.js'
import useRequete from '../hooks/useRequete.js'
import useRechercheTelephone from '../hooks/useRechercheTelephone.js'
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
import { ROLES_GROUPE } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

/** Membres, filtrables par tontine. Inclut l'historique des cotisations. */
export default function Membres() {
  const [parametres, setParametres] = useSearchParams()
  const tontineId = parametres.get('tontineId') || ''
  const toast = useToast()

  const { donnees: tontines } = useRequete(() => tontinesApi.lister(), [], { valeurInitiale: [] })
  // Tontines dont l'utilisateur courant est gestionnaire (voir TontineResponse.administrateur) :
  // seules celles-ci autorisent l'ajout ou le retrait d'un membre.
  const tontinesGerees = (tontines ?? []).filter((t) => t.administrateur)
  const peutGerer = tontinesGerees.length > 0
  const gereLaTontine = (tontineId) =>
    tontines?.some((t) => t.id === tontineId && t.administrateur) ?? false

  const charger = useCallback(
    () => membresApi.lister(tontineId ? { tontineId } : {}),
    [tontineId],
  )
  const { donnees: membres, chargement, erreur, recharger } = useRequete(charger, [tontineId], {
    valeurInitiale: [],
  })

  const majFiltre = (valeur) => {
    const suivants = new URLSearchParams(parametres)
    if (valeur) suivants.set('tontineId', valeur)
    else suivants.delete('tontineId')
    setParametres(suivants, { replace: true })
  }

  // --- Ajout d'un membre --------------------------------------------------
  const [modaleAjout, setModaleAjout] = useState(false)
  const recherche = useRechercheTelephone()
  const [formulaire, setFormulaire] = useState({
    tontineId: tontineId || '',
    roleGroupe: 'MEMBRE',
    ordreTour: '',
  })
  const [erreurFormulaire, setErreurFormulaire] = useState(null)
  const [envoi, setEnvoi] = useState(false)

  const ouvrirAjout = () => {
    setFormulaire({
      tontineId: tontineId || '',
      roleGroupe: 'MEMBRE',
      ordreTour: '',
    })
    recherche.reinitialiser()
    setErreurFormulaire(null)
    setModaleAjout(true)
  }

  const enregistrer = async (evenement) => {
    evenement.preventDefault()
    setErreurFormulaire(null)
    setEnvoi(true)
    try {
      const utilisateur = await recherche.rechercher()
      const membre = await membresApi.creer({
        tontineId: Number(formulaire.tontineId),
        utilisateurId: utilisateur.id,
        roleGroupe: formulaire.roleGroupe,
        ordreTour: formulaire.ordreTour ? Number(formulaire.ordreTour) : null,
      })
      toast.succes(`${membre.nomComplet} a rejoint « ${membre.tontineNom} » et a été notifié(e).`)
      setModaleAjout(false)
      recharger()
    } catch (e) {
      setErreurFormulaire(e?.message !== undefined && e?.champs !== undefined ? e : normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  // --- Historique des cotisations ----------------------------------------
  const [historique, setHistorique] = useState(null) // { membre, cotisations }
  const [chargementHistorique, setChargementHistorique] = useState(false)

  const ouvrirHistorique = async (membre) => {
    setHistorique({ membre, cotisations: null })
    setChargementHistorique(true)
    try {
      const cotisations = await membresApi.listerCotisations(membre.id)
      setHistorique({ membre, cotisations })
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
      setHistorique(null)
    } finally {
      setChargementHistorique(false)
    }
  }

  // --- Actions sensibles --------------------------------------------------
  const [confirmation, setConfirmation] = useState(null)
  const [traitement, setTraitement] = useState(false)

  const executerConfirmation = async () => {
    if (!confirmation) return
    setTraitement(true)
    try {
      const { type, membre } = confirmation
      if (type === 'suspendre') {
        await membresApi.suspendre(membre.id)
        toast.succes(`${membre.nomComplet} a été suspendu.`)
      } else if (type === 'reactiver') {
        await membresApi.reactiver(membre.id)
        toast.succes(`${membre.nomComplet} a été réactivé.`)
      } else {
        await membresApi.supprimer(membre.id)
        toast.succes(`${membre.nomComplet} a été retiré.`)
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

  const textes = {
    suspendre: {
      titre: 'Suspendre le membre',
      message: `Suspendre ${confirmation?.membre?.nomComplet} ? Il ne pourra plus cotiser.`,
      bouton: 'Suspendre',
      dangereux: false,
    },
    reactiver: {
      titre: 'Réactiver le membre',
      message: `Réactiver ${confirmation?.membre?.nomComplet} ?`,
      bouton: 'Réactiver',
      dangereux: false,
    },
    retirer: {
      titre: 'Retirer le membre',
      message: `Retirer ${confirmation?.membre?.nomComplet} de « ${confirmation?.membre?.tontineNom} » ?`,
      detail: 'Le serveur refuse le retrait si des cotisations lui sont rattachées.',
      bouton: 'Retirer',
      dangereux: true,
    },
  }[confirmation?.type] || {}

  return (
    <>
      <PageHeader
        titre="Membres"
        sousTitre="Adhésions des utilisateurs aux tontines, avec leur ordre de passage."
        actions={
          peutGerer && (
            <Button variante="principal" onClick={ouvrirAjout}>{<><Icone nom="plus" taille={18} /> Ajouter un membre</>}</Button>
          )
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
          legende="Liste des membres"
          chargement={chargement}
          erreur={erreur}
          donnees={membres}
          colonnes={[
            {
              cle: 'nomComplet',
              entete: 'Membre',
              rendu: (m) => (
                <>
                  <div className="cellule-principale">{m.nomComplet}</div>
                  <div className="cellule-secondaire">{m.telephone}</div>
                </>
              ),
            },
            {
              cle: 'tontineNom',
              entete: 'Tontine',
              rendu: (m) => <Link to={`/tontines/${m.tontineId}`}>{m.tontineNom}</Link>,
            },
            { cle: 'ordreTour', entete: 'Tour', align: 'num', rendu: (m) => `n°${m.ordreTour}` },
            {
              cle: 'roleGroupe',
              entete: 'Rôle',
              rendu: (m) =>
                ROLES_GROUPE.find((r) => r.valeur === m.roleGroupe)?.libelle || m.roleGroupe || '—',
            },
            {
              cle: 'dateAdhesion',
              entete: 'Adhésion',
              rendu: (m) => formaterDate(m.dateAdhesion),
            },
            { cle: 'statut', entete: 'Statut', rendu: (m) => <Badge statut={m.statut} /> },
            {
              cle: 'actions',
              entete: 'Actions',
              align: 'actions',
              rendu: (m) => (
                <div className="groupe-actions">
                  <Button taille="petit" onClick={() => ouvrirHistorique(m)}>
                    Historique
                  </Button>
                  {gereLaTontine(m.tontineId) && (
                    <>
                      {m.statut === 'ACTIF' ? (
                        <Button
                          taille="petit"
                          onClick={() => setConfirmation({ type: 'suspendre', membre: m })}
                        >
                          Suspendre
                        </Button>
                      ) : (
                        <Button
                          taille="petit"
                          onClick={() => setConfirmation({ type: 'reactiver', membre: m })}
                        >
                          Réactiver
                        </Button>
                      )}
                      <Button
                        taille="petit"
                        variante="danger"
                        onClick={() => setConfirmation({ type: 'retirer', membre: m })}
                      >
                        Retirer
                      </Button>
                    </>
                  )}
                </div>
              ),
            },
          ]}
          vide={
            <EmptyState
              icone="membre"
              titre="Aucun membre"
              texte={
                tontineId
                  ? "Cette tontine n'a pas encore de membre."
                  : "Aucune adhésion n'est enregistrée."
              }
              action={
                peutGerer ? (
                  <Button variante="principal" onClick={ouvrirAjout}>{<><Icone nom="plus" taille={18} /> Ajouter un membre</>}</Button>
                ) : null
              }
            />
          }
        />
      </div>

      {/* Modale : ajout */}
      <Modal
        ouverte={modaleAjout}
        titre="Ajouter un membre"
        onFermer={() => setModaleAjout(false)}
        pied={
          <>
            <Button onClick={() => setModaleAjout(false)} disabled={envoi}>Annuler</Button>
            <Button
              variante="principal"
              onClick={enregistrer}
              chargement={envoi}
              disabled={!formulaire.tontineId || !recherche.telephone.trim()}
            >
              Ajouter
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
            onChange={(e) => setFormulaire((f) => ({ ...f, tontineId: e.target.value }))}
            erreur={erreurFormulaire?.champs?.tontineId}
            options={[
              { valeur: '', libelle: '— Choisir une tontine —' },
              ...tontinesGerees.map((t) => ({ valeur: String(t.id), libelle: t.nom })),
            ]}
            requis
          />

          <Field
            label="Numéro de téléphone du membre"
            nom="telephoneRecherche"
            type="tel"
            valeur={recherche.telephone}
            onChange={(e) => recherche.setTelephone(e.target.value)}
            placeholder="+221 77 000 00 00"
            aide="Le numéro doit déjà être inscrit sur Tontyn : vérifié automatiquement à l'ajout."
            requis
          />

          <div className="grille-champs">
            <Field
              label="Rôle dans le groupe"
              nom="roleGroupe"
              type="select"
              valeur={formulaire.roleGroupe}
              onChange={(e) => setFormulaire((f) => ({ ...f, roleGroupe: e.target.value }))}
              erreur={erreurFormulaire?.champs?.roleGroupe}
              options={ROLES_GROUPE}
            />
            <Field
              label="Ordre de tour"
              nom="ordreTour"
              type="number"
              min="1"
              valeur={formulaire.ordreTour}
              onChange={(e) => setFormulaire((f) => ({ ...f, ordreTour: e.target.value }))}
              erreur={erreurFormulaire?.champs?.ordreTour}
              aide="Vide = attribution automatique."
            />
          </div>
        </form>
      </Modal>

      {/* Modale : historique des cotisations */}
      <Modal
        ouverte={Boolean(historique)}
        large
        titre="Historique des cotisations"
        sousTitre={historique?.membre?.nomComplet}
        onFermer={() => setHistorique(null)}
        pied={<Button onClick={() => setHistorique(null)}>Fermer</Button>}
      >
        <Table
          legende="Cotisations du membre"
          chargement={chargementHistorique}
          donnees={historique?.cotisations ?? []}
          colonnes={[
            { cle: 'cycleNumero', entete: 'Cycle', rendu: (c) => `n°${c.cycleNumero}` },
            { cle: 'tontineNom', entete: 'Tontine' },
            {
              cle: 'montant',
              entete: 'Montant',
              align: 'num',
              rendu: (c) => formaterMontant(c.montant),
            },
            { cle: 'date', entete: 'Enregistrée le', rendu: (c) => formaterDate(c.date) },
            { cle: 'statut', entete: 'Statut', rendu: (c) => <Badge statut={c.statut} /> },
          ]}
          vide={
            <EmptyState
              icone="cotisations"
              titre="Aucune cotisation"
              texte="Ce membre n'a encore enregistré aucune cotisation."
            />
          }
        />
      </Modal>

      <ConfirmDialog
        ouverte={Boolean(confirmation)}
        titre={textes.titre}
        message={textes.message}
        detail={textes.detail}
        libelleConfirmation={textes.bouton}
        dangereux={textes.dangereux}
        chargement={traitement}
        onConfirmer={executerConfirmation}
        onAnnuler={() => setConfirmation(null)}
      />
    </>
  )
}
