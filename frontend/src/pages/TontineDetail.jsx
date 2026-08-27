import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { tontinesApi } from '../api/tontines.js'
import { membresApi } from '../api/membres.js'
import { cyclesApi } from '../api/cycles.js'
import { useAuth } from '../auth/AuthContext.jsx'
import useRequete from '../hooks/useRequete.js'
import useRechercheTelephone from '../hooks/useRechercheTelephone.js'
import { useToast } from '../components/Toast.jsx'
import { normaliserErreur } from '../utils/errors.js'
import PageHeader, { InfoItem } from '../components/PageHeader.jsx'
import Table from '../components/Table.jsx'
import Badge from '../components/Badge.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Alert from '../components/Alert.jsx'
import Loader from '../components/Loader.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Modal, { ConfirmDialog } from '../components/Modal.jsx'
import { dateDuJourIso, formaterDate, formaterMontant } from '../utils/format.js'
import { PERIODICITES, ROLES_GROUPE } from '../utils/constants.js'
import Icone from '../components/Icone.jsx'

/** Fiche d'une tontine : informations, membres et cycles. */
export default function TontineDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { rafraichirUtilisateurId } = useAuth()

  const [onglet, setOnglet] = useState('membres')

  const chargerTout = useCallback(async () => {
    const [tontine, membres, cycles] = await Promise.all([
      tontinesApi.obtenir(id),
      tontinesApi.listerMembres(id),
      tontinesApi.listerCycles(id),
    ])
    return { tontine, membres, cycles }
  }, [id])

  const { donnees, chargement, erreur, recharger } = useRequete(chargerTout, [id])

  // --- Ajout d'un membre -------------------------------------------------
  const [modaleMembre, setModaleMembre] = useState(false)
  const recherche = useRechercheTelephone()
  const [formMembre, setFormMembre] = useState({
    roleGroupe: 'MEMBRE',
    ordreTour: '',
  })
  const [erreurMembre, setErreurMembre] = useState(null)

  const ouvrirAjoutMembre = () => {
    recherche.reinitialiser()
    setFormMembre({ roleGroupe: 'MEMBRE', ordreTour: '' })
    setErreurMembre(null)
    setModaleMembre(true)
  }

  // --- Generation des cycles ---------------------------------------------
  const [modaleCycles, setModaleCycles] = useState(false)
  const [dateDebut, setDateDebut] = useState(dateDuJourIso())
  const [erreurCycles, setErreurCycles] = useState(null)

  const [envoi, setEnvoi] = useState(false)
  const [confirmation, setConfirmation] = useState(null)
  const [traitement, setTraitement] = useState(false)

  if (chargement) return <Loader message="Chargement de la tontine..." />
  if (erreur) {
    return (
      <>
        <Alert type="erreur">{erreur}</Alert>
        <Link className="btn btn--secondaire" to="/tontines">{<><Icone nom="chevronGauche" taille={16} /> Retour aux tontines</>}</Link>
      </>
    )
  }

  const { tontine, membres, cycles } = donnees
  // Droits de gestion propres a CETTE tontine (voir TontineResponse.administrateur) :
  // gestionnaire du groupe, ou administrateur de la plateforme.
  const peutGerer = tontine.administrateur

  const ajouterMembre = async (evenement) => {
    evenement.preventDefault()
    setErreurMembre(null)
    setEnvoi(true)
    try {
      const utilisateur = await recherche.rechercher()
      const charge = {
        utilisateurId: utilisateur.id,
        roleGroupe: formMembre.roleGroupe,
        // L'ordre de tour est attribue automatiquement par le backend si absent.
        ordreTour: formMembre.ordreTour ? Number(formMembre.ordreTour) : null,
      }
      const membre = await tontinesApi.ajouterMembre(id, charge)
      toast.succes(`${membre.nomComplet} a rejoint la tontine et a été notifié(e).`)
      setModaleMembre(false)
      setFormMembre({ roleGroupe: 'MEMBRE', ordreTour: '' })
      // L'utilisateur courant vient peut-etre d'etre rattache a sa 1re tontine.
      rafraichirUtilisateurId()
      recharger()
    } catch (e) {
      setErreurMembre(e?.message !== undefined && e?.champs !== undefined ? e : normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  const genererCycles = async (evenement) => {
    evenement.preventDefault()
    setErreurCycles(null)
    setEnvoi(true)
    try {
      const generes = await tontinesApi.genererCycles(id, { dateDebut })
      toast.succes(`${generes.length} cycle(s) généré(s).`)
      setModaleCycles(false)
      setOnglet('cycles')
      recharger()
    } catch (e) {
      setErreurCycles(normaliserErreur(e))
    } finally {
      setEnvoi(false)
    }
  }

  const executerConfirmation = async () => {
    if (!confirmation) return
    setTraitement(true)
    try {
      const { type, cible } = confirmation
      if (type === 'suspendre') {
        await membresApi.suspendre(cible.id)
        toast.succes(`${cible.nomComplet} a été suspendu.`)
      } else if (type === 'reactiver') {
        await membresApi.reactiver(cible.id)
        toast.succes(`${cible.nomComplet} a été réactivé.`)
      } else if (type === 'retirerMembre') {
        await membresApi.supprimer(cible.id)
        toast.succes(`${cible.nomComplet} a été retiré de la tontine.`)
      } else if (type === 'cloturerCycle') {
        await cyclesApi.cloturer(cible.id)
        toast.succes(`Cycle n°${cible.numero} clôturé.`)
      } else if (type === 'supprimerCycle') {
        await cyclesApi.supprimer(cible.id)
        toast.succes(`Cycle n°${cible.numero} supprimé.`)
      } else if (type === 'cloturerTontine') {
        await tontinesApi.cloturer(id)
        toast.succes('Tontine clôturée.')
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

  const membresActifs = membres.filter((m) => m.statut === 'ACTIF')
  const totalCollecte = cycles.reduce((somme, c) => somme + (c.montantCollecte || 0), 0)

  const LIBELLES_CONFIRMATION = {
    suspendre: {
      titre: 'Suspendre le membre',
      message: `Suspendre ${confirmation?.cible?.nomComplet} ? Ce membre ne pourra plus cotiser tant qu'il n'est pas réactivé.`,
      bouton: 'Suspendre',
      dangereux: false,
    },
    reactiver: {
      titre: 'Réactiver le membre',
      message: `Réactiver ${confirmation?.cible?.nomComplet} ? Il pourra de nouveau cotiser.`,
      bouton: 'Réactiver',
      dangereux: false,
    },
    retirerMembre: {
      titre: 'Retirer le membre',
      message: `Retirer définitivement ${confirmation?.cible?.nomComplet} de cette tontine ?`,
      detail: 'Le serveur refuse le retrait si des cotisations lui sont déjà rattachées.',
      bouton: 'Retirer',
      dangereux: true,
    },
    cloturerCycle: {
      titre: 'Clôturer le cycle',
      message: `Clôturer le cycle n°${confirmation?.cible?.numero} ?`,
      detail: 'La clôture est refusée tant que tous les membres actifs n\'ont pas réglé leur cotisation.',
      bouton: 'Clôturer',
      dangereux: false,
    },
    supprimerCycle: {
      titre: 'Supprimer le cycle',
      message: `Supprimer le cycle n°${confirmation?.cible?.numero} ?`,
      detail: 'Refusé par le serveur si des cotisations y sont rattachées.',
      bouton: 'Supprimer',
      dangereux: true,
    },
    cloturerTontine: {
      titre: 'Clôturer la tontine',
      message: `Clôturer définitivement « ${tontine.nom} » ?`,
      bouton: 'Clôturer',
      dangereux: false,
    },
  }
  const textesConfirmation = confirmation ? LIBELLES_CONFIRMATION[confirmation.type] : null

  return (
    <>
      <PageHeader
        filAriane={<Link to="/tontines">{<><Icone nom="chevronGauche" taille={14} /> Tontines</>}</Link>}
        titre={tontine.nom}
        sousTitre={tontine.description || 'Aucune description renseignée.'}
        actions={
          peutGerer && (
            <>
              <Button onClick={ouvrirAjoutMembre} disabled={tontine.statut === 'CLOTUREE'}>
                {<><Icone nom="plus" taille={18} /> Ajouter un membre</>}
              </Button>
              <Button
                variante="principal"
                onClick={() => setModaleCycles(true)}
                disabled={cycles.length > 0 || membresActifs.length === 0}
                title={
                  cycles.length > 0
                    ? 'Les cycles ont déjà été générés.'
                    : membresActifs.length === 0
                      ? 'Ajoutez au moins un membre actif.'
                      : undefined
                }
              >
                {<><Icone nom="calendrierPlus" taille={18} /> Générer les cycles</>}
              </Button>
              {tontine.statut !== 'CLOTUREE' && (
                <Button onClick={() => setConfirmation({ type: 'cloturerTontine' })}>
                  Clôturer
                </Button>
              )}
            </>
          )
        }
      />

      {/* Fiche d'identite */}
      <div className="carte">
        <div className="carte__corps">
          <div className="liste-infos">
            <InfoItem cle="Statut"><Badge statut={tontine.statut} /></InfoItem>
            <InfoItem cle="Cotisation par cycle">
              {formaterMontant(tontine.montantCotisation)}
            </InfoItem>
            <InfoItem cle="Périodicité">
              {PERIODICITES.find((p) => p.valeur === tontine.periodicite)?.libelle
                || tontine.periodicite}
            </InfoItem>
            <InfoItem cle="Membres inscrits">
              {tontine.nombreMembresInscrits} / {tontine.nombreMembres}
              <span className="cellule-secondaire"> ({membresActifs.length} actif(s))</span>
            </InfoItem>
            <InfoItem cle="Cycles">{tontine.nombreCycles}</InfoItem>
            <InfoItem cle="Montant collecté">{formaterMontant(totalCollecte)}</InfoItem>
            <InfoItem cle="Date de création">{formaterDate(tontine.dateCreation)}</InfoItem>
          </div>
        </div>
      </div>

      {cycles.length === 0 && membresActifs.length > 0 && peutGerer && (
        <Alert type="info" titre="Cycles non générés">
          Cette tontine compte {membresActifs.length} membre(s) actif(s) mais aucun cycle.
          Utilisez « Générer les cycles » pour créer automatiquement un tour par membre,
          selon l'ordre de passage.
        </Alert>
      )}

      {/* Onglets Membres / Cycles */}
      <div className="onglets mt-16" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={onglet === 'membres'}
          className={`onglet${onglet === 'membres' ? ' onglet--actif' : ''}`}
          onClick={() => setOnglet('membres')}
        >
          Membres ({membres.length})
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={onglet === 'cycles'}
          className={`onglet${onglet === 'cycles' ? ' onglet--actif' : ''}`}
          onClick={() => setOnglet('cycles')}
        >
          Cycles ({cycles.length})
        </button>
      </div>

      {onglet === 'membres' ? (
        <div className="carte">
          <Table
            legende="Membres de la tontine"
            donnees={membres}
            colonnes={[
              {
                cle: 'ordreTour',
                entete: 'Tour',
                align: 'num',
                rendu: (m) => `n°${m.ordreTour}`,
              },
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
                cle: 'roleGroupe',
                entete: 'Rôle',
                rendu: (m) =>
                  ROLES_GROUPE.find((r) => r.valeur === m.roleGroupe)?.libelle
                  || m.roleGroupe || '—',
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
                    <Link
                      className="btn btn--secondaire btn--petit"
                      to={`/cotisations?membreId=${m.id}`}
                    >
                      Cotisations
                    </Link>
                    {peutGerer && (
                      <>
                        {m.statut === 'ACTIF' ? (
                          <Button
                            taille="petit"
                            onClick={() => setConfirmation({ type: 'suspendre', cible: m })}
                          >
                            Suspendre
                          </Button>
                        ) : (
                          <Button
                            taille="petit"
                            onClick={() => setConfirmation({ type: 'reactiver', cible: m })}
                          >
                            Réactiver
                          </Button>
                        )}
                        <Button
                          taille="petit"
                          variante="danger"
                          onClick={() => setConfirmation({ type: 'retirerMembre', cible: m })}
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
                texte="Ajoutez des membres avant de générer les cycles de la tontine."
                action={
                  peutGerer ? (
                    <Button variante="principal" onClick={ouvrirAjoutMembre}>
                      {<><Icone nom="plus" taille={18} /> Ajouter un membre</>}
                    </Button>
                  ) : null
                }
              />
            }
          />
        </div>
      ) : (
        <div className="carte">
          <Table
            legende="Cycles de la tontine"
            donnees={cycles}
            colonnes={[
              {
                cle: 'numero',
                entete: 'Cycle',
                rendu: (c) => (
                  <Link className="cellule-principale" to={`/cycles/${c.id}`}>
                    Cycle n°{c.numero}
                  </Link>
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
                rendu: (c) => c.beneficiaireNom || <span className="texte-discret">Non désigné</span>,
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
                    {peutGerer && (
                      <>
                        {c.statut !== 'CLOTURE' && (
                          <Button
                            taille="petit"
                            onClick={() => setConfirmation({ type: 'cloturerCycle', cible: c })}
                          >
                            Clôturer
                          </Button>
                        )}
                        <Button
                          taille="petit"
                          variante="danger"
                          onClick={() => setConfirmation({ type: 'supprimerCycle', cible: c })}
                        >
                          Supprimer
                        </Button>
                      </>
                    )}
                  </div>
                ),
              },
            ]}
            vide={
              <EmptyState
                icone="cycles"
                titre="Aucun cycle"
                texte="Générez les cycles : un tour sera créé par membre actif, dans l'ordre de passage."
                action={
                  peutGerer && membresActifs.length > 0 ? (
                    <Button variante="principal" onClick={() => setModaleCycles(true)}>
                      {<><Icone nom="calendrierPlus" taille={18} /> Générer les cycles</>}
                    </Button>
                  ) : null
                }
              />
            }
          />
        </div>
      )}

      {/* Modale : ajout d'un membre */}
      <Modal
        ouverte={modaleMembre}
        titre="Ajouter un membre"
        sousTitre={`Tontine « ${tontine.nom} »`}
        onFermer={() => setModaleMembre(false)}
        pied={
          <>
            <Button onClick={() => setModaleMembre(false)} disabled={envoi}>Annuler</Button>
            <Button
              variante="principal"
              onClick={ajouterMembre}
              chargement={envoi}
              disabled={!recherche.telephone.trim()}
            >
              Ajouter
            </Button>
          </>
        }
      >
        <form onSubmit={ajouterMembre} noValidate>
          {erreurMembre && (
            <Alert type="erreur" champs={erreurMembre.champs}>{erreurMembre.message}</Alert>
          )}

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
              valeur={formMembre.roleGroupe}
              onChange={(e) => setFormMembre((f) => ({ ...f, roleGroupe: e.target.value }))}
              erreur={erreurMembre?.champs?.roleGroupe}
              options={ROLES_GROUPE}
            />
            <Field
              label="Ordre de tour"
              nom="ordreTour"
              type="number"
              min="1"
              valeur={formMembre.ordreTour}
              onChange={(e) => setFormMembre((f) => ({ ...f, ordreTour: e.target.value }))}
              erreur={erreurMembre?.champs?.ordreTour}
              aide="Laissez vide pour une attribution automatique."
            />
          </div>
        </form>
      </Modal>

      {/* Modale : generation des cycles */}
      <Modal
        ouverte={modaleCycles}
        titre="Générer les cycles"
        sousTitre={`${membresActifs.length} cycle(s) seront créés, un par membre actif.`}
        onFermer={() => setModaleCycles(false)}
        pied={
          <>
            <Button onClick={() => setModaleCycles(false)} disabled={envoi}>Annuler</Button>
            <Button variante="principal" onClick={genererCycles} chargement={envoi}>
              Générer
            </Button>
          </>
        }
      >
        <form onSubmit={genererCycles} noValidate>
          {erreurCycles && (
            <Alert type="erreur" champs={erreurCycles.champs}>{erreurCycles.message}</Alert>
          )}

          <Alert type="info">
            Chaque membre actif devient bénéficiaire du cycle correspondant à son ordre de tour.
            Les dates s'enchaînent selon la périodicité de la tontine
            {' ('}
            {PERIODICITES.find((p) => p.valeur === tontine.periodicite)?.libelle
              || tontine.periodicite}
            {'). '}
            Le premier cycle démarre « en cours », les suivants sont « planifiés ».
          </Alert>

          <Field
            label="Date de début du premier cycle"
            nom="dateDebut"
            type="date"
            valeur={dateDebut}
            onChange={(e) => setDateDebut(e.target.value)}
            erreur={erreurCycles?.champs?.dateDebut}
            requis
          />
        </form>
      </Modal>

      <ConfirmDialog
        ouverte={Boolean(confirmation)}
        titre={textesConfirmation?.titre}
        message={textesConfirmation?.message}
        detail={textesConfirmation?.detail}
        libelleConfirmation={textesConfirmation?.bouton}
        dangereux={textesConfirmation?.dangereux}
        chargement={traitement}
        onConfirmer={executerConfirmation}
        onAnnuler={() => setConfirmation(null)}
      />
    </>
  )
}
