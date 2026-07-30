// Formatage aux conventions francaises / senegalaises (FCFA).

const FORMAT_MONTANT = new Intl.NumberFormat('fr-FR', {
  maximumFractionDigits: 0,
})

/** 25000 -> "25 000 FCFA" */
export function formaterMontant(montant) {
  if (montant === null || montant === undefined || Number.isNaN(Number(montant))) return '—'
  return `${FORMAT_MONTANT.format(Number(montant))} FCFA`
}

/** "2026-07-27" -> "27/07/2026" */
export function formaterDate(valeur) {
  if (!valeur) return '—'
  const date = new Date(valeur)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

/** "2026-07-27T16:20:45" -> "27/07/2026 à 16:20" */
export function formaterDateHeure(valeur) {
  if (!valeur) return '—'
  const date = new Date(valeur)
  if (Number.isNaN(date.getTime())) return '—'
  return `${date.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })} à ${date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`
}

/** Date du jour au format attendu par <input type="date"> (AAAA-MM-JJ). */
export function dateDuJourIso() {
  const maintenant = new Date()
  const decalage = maintenant.getTimezoneOffset() * 60000
  return new Date(maintenant.getTime() - decalage).toISOString().slice(0, 10)
}

/** Initiales pour l'avatar de la barre superieure. */
export function initiales(texte) {
  if (!texte) return '?'
  const parties = texte.replace(/@.*/, '').split(/[\s.\-_]+/).filter(Boolean)
  return parties.slice(0, 2).map((p) => p[0].toUpperCase()).join('') || '?'
}
