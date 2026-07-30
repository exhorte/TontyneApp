/** En-tete de page : titre, sous-titre explicatif et zone d'actions. */
export default function PageHeader({ titre, sousTitre, actions, filAriane }) {
  return (
    <div className="entete-page">
      <div>
        {filAriane && <div className="fil-ariane">{filAriane}</div>}
        <h1>{titre}</h1>
        {sousTitre && <p className="entete-page__sous-titre">{sousTitre}</p>}
      </div>
      {actions && <div className="entete-page__actions">{actions}</div>}
    </div>
  )
}

/** Carte de synthese chiffree utilisee sur le tableau de bord. */
export function StatCard({ libelle, valeur, detail }) {
  return (
    <div className="stat">
      <div className="stat__libelle">{libelle}</div>
      <div className="stat__valeur">{valeur}</div>
      {detail && <div className="stat__detail">{detail}</div>}
    </div>
  )
}

/** Paire cle / valeur pour les pages de detail. */
export function InfoItem({ cle, children }) {
  return (
    <div>
      <div className="liste-infos__cle">{cle}</div>
      <p className="liste-infos__valeur">{children ?? '—'}</p>
    </div>
  )
}
