import { formaterDateHeure, formaterMontant } from '../utils/format.js'
import { LIBELLES_METHODE } from '../utils/constants.js'
import { InfoItem } from './PageHeader.jsx'

/** Justificatif de paiement, mis en forme pour l'affichage et l'impression. */
export default function RecuVue({ recu }) {
  if (!recu) return null

  return (
    <div className="recu">
      <div className="recu__entete">
        <div>
          <div className="rangee" style={{ gap: 8 }}>
            <span className="logo-pastille" aria-hidden="true">T</span>
            <strong style={{ fontSize: '1.05rem' }}>Tontyn</strong>
          </div>
          <p className="texte-discret" style={{ marginTop: 4 }}>
            Reçu de cotisation
          </p>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="recu__numero">{recu.numero}</div>
          <p className="texte-discret">Émis le {formaterDateHeure(recu.dateEmission)}</p>
        </div>
      </div>

      <div className="liste-infos">
        <InfoItem cle="Membre">{recu.membreNom}</InfoItem>
        <InfoItem cle="Tontine">{recu.tontineNom}</InfoItem>
        <InfoItem cle="Cycle">n°{recu.cycleNumero}</InfoItem>
        <InfoItem cle="Méthode de paiement">
          {LIBELLES_METHODE[recu.methode] || recu.methode}
        </InfoItem>
        <InfoItem cle="Référence de l'opérateur">
          <span className="texte-mono">{recu.referencePaiement}</span>
        </InfoItem>
        <InfoItem cle="Paiement">#{recu.paiementId}</InfoItem>
      </div>

      <div className="recu__total">
        <span>Montant réglé</span>
        <span>{formaterMontant(recu.montant)}</span>
      </div>
    </div>
  )
}
