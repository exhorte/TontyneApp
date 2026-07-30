import { IconeCercle } from './Icone.jsx'

/**
 * Etat vide : affiche pourquoi la liste est vide et l'action a mener.
 * `icone` est le nom d'une icone du composant Icone (ex. "tontines", "cotisations").
 */
export default function EmptyState({ icone = 'info', titre, texte, action }) {
  return (
    <div className="etat-vide">
      <div className="etat-vide__icone" aria-hidden="true">
        <IconeCercle nom={icone} taille={64} />
      </div>
      <p className="etat-vide__titre">{titre}</p>
      {texte && <p className="etat-vide__texte">{texte}</p>}
      {action}
    </div>
  )
}
