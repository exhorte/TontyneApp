import { LIBELLES_STATUT, VARIANTES_STATUT } from '../utils/constants.js'

/**
 * Pastille de statut. Traduit les statuts techniques de l'API
 * (EN_ATTENTE, PAYEE, CONFIRME...) en libelle francais colore.
 */
export default function Badge({ statut, variante, libelle }) {
  const cle = statut ?? ''
  const ton = variante || VARIANTES_STATUT[cle] || 'neutre'
  const texte = libelle || LIBELLES_STATUT[cle] || cle || '—'
  return <span className={`badge badge--${ton}`}>{texte}</span>
}
