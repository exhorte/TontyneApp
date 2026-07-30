import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { recusApi } from '../api/recus.js'
import { membresApi } from '../api/membres.js'
import RoleGate from '../auth/RoleGate.jsx'
import useRequete from '../hooks/useRequete.js'
import { useToast } from '../components/Toast.jsx'
import { normaliserErreur } from '../utils/errors.js'
import PageHeader from '../components/PageHeader.jsx'
import Table from '../components/Table.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Modal, { ConfirmDialog } from '../components/Modal.jsx'
import RecuVue from '../components/RecuVue.jsx'
import { formaterDateHeure, formaterMontant } from '../utils/format.js'
import { LIBELLES_METHODE, ROLES } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

/** Consultation des reçus émis pour les paiements confirmés. */
export default function Recus() {
  const [membreId, setMembreId] = useState('')
  const toast = useToast()

  const { donnees: membres } = useRequete(() => membresApi.lister(), [], { valeurInitiale: [] })

  const charger = useCallback(
    () => recusApi.lister(membreId ? { membreId } : {}),
    [membreId],
  )
  const { donnees: recus, chargement, erreur, recharger } = useRequete(charger, [membreId], {
    valeurInitiale: [],
  })

  const [recuAffiche, setRecuAffiche] = useState(null)
  const [confirmation, setConfirmation] = useState(null)
  const [traitement, setTraitement] = useState(false)

  const supprimer = async () => {
    setTraitement(true)
    try {
      await recusApi.supprimer(confirmation.id)
      toast.succes(`Reçu ${confirmation.numero} supprimé.`)
      setConfirmation(null)
      recharger()
    } catch (e) {
      toast.erreur(normaliserErreur(e).message)
      setConfirmation(null)
    } finally {
      setTraitement(false)
    }
  }

  const total = (recus ?? []).reduce((somme, r) => somme + (r.montant || 0), 0)

  return (
    <>
      <PageHeader
        titre="Reçus"
        sousTitre={`${recus?.length ?? 0} reçu(s) émis, pour un total de ${formaterMontant(total)}.`}
      />

      <div className="barre-filtres">
        <Field
          label="Filtrer par membre"
          nom="filtreMembre"
          type="select"
          valeur={membreId}
          onChange={(e) => setMembreId(e.target.value)}
          options={[
            { valeur: '', libelle: 'Tous les membres' },
            ...(membres ?? []).map((m) => ({
              valeur: String(m.id),
              libelle: `${m.nomComplet} — ${m.tontineNom}`,
            })),
          ]}
        />
        <Button onClick={recharger}>{<><Icone nom="actualiser" taille={18} /> Actualiser</>}</Button>
      </div>

      <div className="carte">
        <Table
          legende="Liste des reçus"
          chargement={chargement}
          erreur={erreur}
          donnees={recus}
          colonnes={[
            {
              cle: 'numero',
              entete: 'Numéro',
              rendu: (r) => <span className="texte-mono cellule-principale">{r.numero}</span>,
            },
            {
              cle: 'membreNom',
              entete: 'Membre',
              rendu: (r) => (
                <>
                  <div>{r.membreNom}</div>
                  <div className="cellule-secondaire">
                    {r.tontineNom} — cycle n°{r.cycleNumero}
                  </div>
                </>
              ),
            },
            {
              cle: 'montant',
              entete: 'Montant',
              align: 'num',
              rendu: (r) => formaterMontant(r.montant),
            },
            {
              cle: 'methode',
              entete: 'Méthode',
              rendu: (r) => LIBELLES_METHODE[r.methode] || r.methode,
            },
            {
              cle: 'dateEmission',
              entete: 'Émis le',
              rendu: (r) => formaterDateHeure(r.dateEmission),
            },
            {
              cle: 'actions',
              entete: 'Actions',
              align: 'actions',
              rendu: (r) => (
                <div className="groupe-actions">
                  <Button taille="petit" onClick={() => setRecuAffiche(r)}>
                    {<><Icone nom="oeil" taille={18} /> Consulter</>}
                  </Button>
                  <RoleGate roles={[ROLES.ADMINISTRATEUR]}>
                    <Button taille="petit" variante="danger" onClick={() => setConfirmation(r)}>
                      Supprimer
                    </Button>
                  </RoleGate>
                </div>
              ),
            },
          ]}
          vide={
            <EmptyState
              icone="recus"
              titre="Aucun reçu"
              texte="Un reçu est émis automatiquement dès qu'un paiement est confirmé."
              action={
                <Link className="btn btn--principal" to="/paiements">
                  Voir les paiements
                </Link>
              }
            />
          }
        />
      </div>

      <Modal
        ouverte={Boolean(recuAffiche)}
        large
        titre="Reçu de paiement"
        onFermer={() => setRecuAffiche(null)}
        pied={
          <>
            <Button onClick={() => setRecuAffiche(null)}>Fermer</Button>
            <Button variante="principal" onClick={() => window.print()}>{<><Icone nom="imprimer" taille={18} /> Imprimer</>}</Button>
          </>
        }
      >
        <RecuVue recu={recuAffiche} />
      </Modal>

      <ConfirmDialog
        ouverte={Boolean(confirmation)}
        titre="Supprimer le reçu"
        message={`Supprimer le reçu ${confirmation?.numero} ?`}
        detail="Cette action est irréversible et retire le justificatif du membre."
        libelleConfirmation="Supprimer"
        dangereux
        chargement={traitement}
        onConfirmer={supprimer}
        onAnnuler={() => setConfirmation(null)}
      />
    </>
  )
}
