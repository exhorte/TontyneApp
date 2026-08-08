import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { paiementsApi } from '../api/paiements.js'
import { cotisationsApi } from '../api/cotisations.js'
import { recusApi } from '../api/recus.js'
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
import RecuVue from '../components/RecuVue.jsx'
import { formaterDateHeure, formaterMontant } from '../utils/format.js'
import { LIBELLES_METHODE, METHODES_PAIEMENT, ROLES, ROLES_ACTION } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

const STATUTS_PAIEMENT = [
  { valeur: 'INITIE', libelle: 'Initié' },
  { valeur: 'CONFIRME', libelle: 'Confirmé' },
  { valeur: 'ANNULE', libelle: 'Annulé' },
]

/**
 * Paiements Orange Money / Wave.
 * Parcours complet : cotisation → initiation du paiement → confirmation
 * (qui solde la cotisation, émet le reçu et notifie le membre) → consultation du reçu.
 */
export default function Paiements() {
  const [parametres, setParametres] = useSearchParams()
  const cotisationIdUrl = parametres.get('cotisationId')
  const statut = parametres.get('statut') || ''
  const toast = useToast()

  const charger = useCallback(
    () => paiementsApi.lister(statut ? { statut } : {}),
    [statut],
  )
  const { donnees: paiements, chargement, erreur, recharger } = useRequete(charger, [statut], {
    valeurInitiale: [],
  })

  const { donnees: cotisations, recharger: rechargerCotisations } = useRequete(
    () => cotisationsApi.lister(),
    [],
    { valeurInitiale: [] },
  )

  // --- Initiation d'un paiement -------------------------------------------
  const [modale, setModale] = useState(false)
  const [formulaire, setFormulaire] = useState({
    cotisationId: '',
    methode: 'WAVE',
    reference: '',
  })
  const [erreurFormulaire, setErreurFormulaire] = useState(null)
  const [envoi, setEnvoi] = useState(false)

  // Ouverture automatique quand on arrive depuis « Payer » (?cotisationId=…).
  useEffect(() => {
    if (cotisationIdUrl) {
      setFormulaire({ cotisationId: cotisationIdUrl, methode: 'WAVE', reference: '' })
      setErreurFormulaire(null)
      setModale(true)
    }
  }, [cotisationIdUrl])

  const fermerModale = () => {
    setModale(false)
    if (cotisationIdUrl) {
      const suivants = new URLSearchParams(parametres)
      suivants.delete('cotisationId')
      setParametres(suivants, { replace: true })
    }
  }

  const initier = async (evenement) => {
    evenement.preventDefault()
    setErreurFormulaire(null)
    setEnvoi(true)
    try {
      const paiement = await paiementsApi.initier({
        cotisationId: Number(formulaire.cotisationId),
        methode: formulaire.methode,
        reference: formulaire.reference.trim() || null,
      })
      toast.succes(
        `Paiement ${LIBELLES_METHODE[paiement.methode]} initié (réf. ${paiement.reference}). `
        + 'Il doit maintenant être confirmé.',
      )
      fermerModale()
      recharger()
      rechargerCotisations()
    } catch (e) {
      setErreurFormulaire(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  // --- Confirmation / annulation / suppression ----------------------------
  const [confirmation, setConfirmation] = useState(null)
  const [traitement, setTraitement] = useState(false)

  const executerConfirmation = async () => {
    if (!confirmation) return
    setTraitement(true)
    try {
      const { type, paiement } = confirmation
      if (type === 'confirmer') {
        const resultat = await paiementsApi.confirmer(paiement.id)
        toast.succes(
          `Paiement confirmé : la cotisation est soldée, le reçu est émis et le membre a été notifié.`,
        )
        // On enchaine directement sur l'affichage du recu genere.
        if (resultat.recuId) ouvrirRecu(paiement.id)
      } else if (type === 'annuler') {
        await paiementsApi.annuler(paiement.id)
        toast.succes('Paiement annulé.')
      } else {
        await paiementsApi.supprimer(paiement.id)
        toast.succes('Paiement supprimé.')
      }
      setConfirmation(null)
      recharger()
      rechargerCotisations()
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
      setConfirmation(null)
    } finally {
      setTraitement(false)
    }
  }

  // --- Consultation du recu ------------------------------------------------
  const [recu, setRecu] = useState(null)
  const [chargementRecu, setChargementRecu] = useState(false)

  const ouvrirRecu = async (paiementId) => {
    setChargementRecu(true)
    setRecu({ enAttente: true })
    try {
      const donnees = await paiementsApi.obtenirRecu(paiementId)
      setRecu(donnees)
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
      setRecu(null)
    } finally {
      setChargementRecu(false)
    }
  }

  /** Rattrapage : émet le reçu d'un paiement confirmé qui n'en aurait pas. */
  const genererRecu = async (paiement) => {
    try {
      await recusApi.genererPourPaiement(paiement.id)
      toast.succes('Reçu émis.')
      recharger()
      ouvrirRecu(paiement.id)
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
    }
  }

  const cotisationsPayables = (cotisations ?? []).filter((c) => c.statut !== 'PAYEE')
  const cotisationChoisie = (cotisations ?? []).find(
    (c) => String(c.id) === String(formulaire.cotisationId),
  )

  const textesConfirmation = {
    confirmer: {
      titre: 'Confirmer le paiement',
      message: `Confirmer le paiement de ${formaterMontant(confirmation?.paiement?.montant)} de ${confirmation?.paiement?.membreNom} ?`,
      detail: 'La cotisation passera à « payée », un reçu sera émis et le membre sera notifié par e-mail.',
      bouton: 'Confirmer le paiement',
      dangereux: false,
    },
    annuler: {
      titre: 'Annuler le paiement',
      message: `Annuler le paiement ${confirmation?.paiement?.reference} ?`,
      detail: 'Un paiement annulé ne peut plus être confirmé : il faudra en initier un nouveau.',
      bouton: 'Annuler le paiement',
      dangereux: true,
    },
    supprimer: {
      titre: 'Supprimer le paiement',
      message: `Supprimer définitivement le paiement ${confirmation?.paiement?.reference} ?`,
      detail: 'Refusé par le serveur si un reçu a déjà été émis.',
      bouton: 'Supprimer',
      dangereux: true,
    },
  }[confirmation?.type] || {}

  return (
    <>
      <PageHeader
        titre="Paiements"
        sousTitre="Règlement des cotisations par Orange Money ou Wave."
        actions={
          <Button
            variante="principal"
            onClick={() => {
              setFormulaire({ cotisationId: '', methode: 'WAVE', reference: '' })
              setErreurFormulaire(null)
              setModale(true)
            }}
          >
            {<><Icone nom="plus" taille={18} /> Initier un paiement</>}
          </Button>
        }
      />

      <Alert type="info" titre="Enchaînement du parcours">
        Une cotisation est d'abord enregistrée, puis un paiement est initié (statut « initié »).
        La confirmation par un administrateur de la tontine solde la cotisation, génère automatiquement le reçu
        et envoie une notification au membre.
      </Alert>

      <div className="barre-filtres">
        <Field
          label="Filtrer par statut"
          nom="filtreStatut"
          type="select"
          valeur={statut}
          onChange={(e) => {
            const suivants = new URLSearchParams(parametres)
            if (e.target.value) suivants.set('statut', e.target.value)
            else suivants.delete('statut')
            setParametres(suivants, { replace: true })
          }}
          options={[{ valeur: '', libelle: 'Tous les statuts' }, ...STATUTS_PAIEMENT]}
        />
        <Button onClick={recharger}>{<><Icone nom="actualiser" taille={18} /> Actualiser</>}</Button>
      </div>

      <div className="carte">
        <Table
          legende="Liste des paiements"
          chargement={chargement}
          erreur={erreur}
          donnees={paiements}
          colonnes={[
            {
              cle: 'reference',
              entete: 'Référence',
              rendu: (p) => (
                <>
                  <div className="cellule-principale texte-mono">{p.reference}</div>
                  <div className="cellule-secondaire">
                    {LIBELLES_METHODE[p.methode] || p.methode}
                  </div>
                </>
              ),
            },
            {
              cle: 'membreNom',
              entete: 'Membre',
              rendu: (p) => (
                <>
                  <div>{p.membreNom}</div>
                  <div className="cellule-secondaire">Cotisation #{p.cotisationId}</div>
                </>
              ),
            },
            {
              cle: 'montant',
              entete: 'Montant',
              align: 'num',
              rendu: (p) => formaterMontant(p.montant),
            },
            { cle: 'date', entete: 'Date', rendu: (p) => formaterDateHeure(p.date) },
            { cle: 'statut', entete: 'Statut', rendu: (p) => <Badge statut={p.statut} /> },
            {
              cle: 'actions',
              entete: 'Actions',
              align: 'actions',
              rendu: (p) => (
                <div className="groupe-actions">
                  {p.statut === 'INITIE' && (
                    <RoleGate
                      roles={ROLES_ACTION}
                      remplacement={
                        <span className="texte-discret">En attente de confirmation</span>
                      }
                    >
                      <Button
                        taille="petit"
                        variante="principal"
                        onClick={() => setConfirmation({ type: 'confirmer', paiement: p })}
                      >
                        Confirmer
                      </Button>
                      <Button
                        taille="petit"
                        onClick={() => setConfirmation({ type: 'annuler', paiement: p })}
                      >
                        Annuler
                      </Button>
                    </RoleGate>
                  )}

                  {p.statut === 'CONFIRME' && (
                    p.recuId ? (
                      <Button taille="petit" onClick={() => ouvrirRecu(p.id)}>
                        {<><Icone nom="recus" taille={18} /> Voir le reçu</>}
                      </Button>
                    ) : (
                      <RoleGate roles={ROLES_ACTION}>
                        <Button taille="petit" onClick={() => genererRecu(p)}>
                          Émettre le reçu
                        </Button>
                      </RoleGate>
                    )
                  )}

                  <RoleGate roles={[ROLES.ADMINISTRATEUR]}>
                    <Button
                      taille="petit"
                      variante="danger"
                      onClick={() => setConfirmation({ type: 'supprimer', paiement: p })}
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
              icone="paiements"
              titre="Aucun paiement"
              texte="Initiez un paiement à partir d'une cotisation en attente."
              action={
                <Link className="btn btn--principal" to="/cotisations">
                  Voir les cotisations
                </Link>
              }
            />
          }
        />
      </div>

      {/* Modale : initiation du paiement */}
      <Modal
        ouverte={modale}
        titre="Initier un paiement"
        sousTitre="Étape 2 du parcours : cotisation → paiement → reçu."
        onFermer={fermerModale}
        pied={
          <>
            <Button onClick={fermerModale} disabled={envoi}>Annuler</Button>
            <Button
              variante="principal"
              onClick={initier}
              chargement={envoi}
              disabled={!formulaire.cotisationId}
            >
              Initier le paiement
            </Button>
          </>
        }
      >
        <form onSubmit={initier} noValidate>
          {erreurFormulaire && (
            <Alert type="erreur" champs={erreurFormulaire.champs}>
              {erreurFormulaire.message}
            </Alert>
          )}

          <Field
            label="Cotisation à régler"
            nom="cotisationId"
            type="select"
            valeur={formulaire.cotisationId}
            onChange={(e) => setFormulaire((f) => ({ ...f, cotisationId: e.target.value }))}
            erreur={erreurFormulaire?.champs?.cotisationId}
            options={[
              { valeur: '', libelle: '— Choisir une cotisation —' },
              ...cotisationsPayables.map((c) => ({
                valeur: String(c.id),
                libelle: `#${c.id} — ${c.membreNom} — ${c.tontineNom} (cycle n°${c.cycleNumero}) — ${formaterMontant(c.montant)}`,
              })),
              // La cotisation ciblee par l'URL peut deja etre payee : on l'affiche
              // quand meme pour que le message d'erreur du serveur reste explicite.
              ...(cotisationChoisie && cotisationChoisie.statut === 'PAYEE'
                ? [{
                  valeur: String(cotisationChoisie.id),
                  libelle: `#${cotisationChoisie.id} — ${cotisationChoisie.membreNom} (déjà payée)`,
                }]
                : []),
            ]}
            requis
          />

          {cotisationChoisie && (
            <Alert type={cotisationChoisie.statut === 'PAYEE' ? 'attention' : 'info'}>
              {cotisationChoisie.statut === 'PAYEE'
                ? 'Cette cotisation est déjà réglée : le serveur refusera un nouveau paiement.'
                : `Montant à régler : ${formaterMontant(cotisationChoisie.montant)} — ${cotisationChoisie.membreNom}, ${cotisationChoisie.tontineNom}, cycle n°${cotisationChoisie.cycleNumero}.`}
            </Alert>
          )}

          <Field
            label="Méthode de paiement"
            nom="methode"
            type="select"
            valeur={formulaire.methode}
            onChange={(e) => setFormulaire((f) => ({ ...f, methode: e.target.value }))}
            erreur={erreurFormulaire?.champs?.methode}
            options={METHODES_PAIEMENT}
            requis
          />

          <Field
            label="Référence de l'opérateur"
            nom="reference"
            valeur={formulaire.reference}
            onChange={(e) => setFormulaire((f) => ({ ...f, reference: e.target.value }))}
            erreur={erreurFormulaire?.champs?.reference}
            aide="Facultatif : une référence est générée automatiquement si ce champ reste vide."
            maxLength={50}
            placeholder="WV-XXXXXXXXXX"
          />
        </form>
      </Modal>

      {/* Modale : reçu */}
      <Modal
        ouverte={Boolean(recu)}
        large
        titre="Reçu de paiement"
        onFermer={() => setRecu(null)}
        pied={
          <>
            <Button onClick={() => setRecu(null)}>Fermer</Button>
            {!recu?.enAttente && (
              <Button variante="principal" onClick={() => window.print()}>
                {<><Icone nom="imprimer" taille={18} /> Imprimer</>}
              </Button>
            )}
          </>
        }
      >
        {chargementRecu || recu?.enAttente ? (
          <p className="texte-discret">Chargement du reçu...</p>
        ) : (
          <RecuVue recu={recu} />
        )}
      </Modal>

      <ConfirmDialog
        ouverte={Boolean(confirmation)}
        titre={textesConfirmation.titre}
        message={textesConfirmation.message}
        detail={textesConfirmation.detail}
        libelleConfirmation={textesConfirmation.bouton}
        dangereux={textesConfirmation.dangereux}
        chargement={traitement}
        onConfirmer={executerConfirmation}
        onAnnuler={() => setConfirmation(null)}
      />
    </>
  )
}
