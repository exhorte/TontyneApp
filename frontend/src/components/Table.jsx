import Loader from './Loader.jsx'
import EmptyState from './EmptyState.jsx'
import Alert from './Alert.jsx'

/**
 * Tableau generique gerant lui-meme les etats chargement / erreur / vide.
 *
 * colonnes : [{ cle, entete, rendu?: (ligne) => ReactNode, align?: 'num'|'actions' }]
 */
export default function Table({
  colonnes,
  donnees,
  cleLigne = (ligne) => ligne.id,
  chargement = false,
  erreur = null,
  vide,
  classeLigne,
  legende,
}) {
  if (chargement) return <Loader />
  if (erreur) return <div style={{ padding: 18 }}><Alert type="erreur">{erreur}</Alert></div>
  if (!donnees?.length) {
    return vide ?? <EmptyState titre="Aucune donnee a afficher." />
  }

  const classeAlignement = (align) =>
    align === 'num' ? 'col-num' : align === 'actions' ? 'col-actions' : undefined

  return (
    <div className="tableau-conteneur">
      <table className="tableau">
        {legende && <caption className="sr-only">{legende}</caption>}
        <thead>
          <tr>
            {colonnes.map((colonne) => (
              <th key={colonne.cle} scope="col" className={classeAlignement(colonne.align)}>
                {colonne.entete}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {donnees.map((ligne) => (
            <tr key={cleLigne(ligne)} className={classeLigne?.(ligne)}>
              {colonnes.map((colonne) => (
                <td key={colonne.cle} className={classeAlignement(colonne.align)}>
                  {colonne.rendu ? colonne.rendu(ligne) : ligne[colonne.cle] ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
