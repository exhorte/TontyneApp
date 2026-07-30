/**
 * Bouton unifie.
 * variante : principal | secondaire | danger | danger-plein | discret
 * `chargement` bloque le bouton et affiche un spinner (evite les doubles soumissions).
 */
export default function Button({
  children,
  variante = 'secondaire',
  taille,
  chargement = false,
  bloc = false,
  type = 'button',
  className = '',
  disabled,
  ...reste
}) {
  const classes = [
    'btn',
    `btn--${variante}`,
    taille === 'petit' ? 'btn--petit' : '',
    bloc ? 'btn--bloc' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  const spinnerClair = variante === 'principal' || variante === 'danger-plein'

  return (
    <button
      type={type}
      className={classes}
      disabled={disabled || chargement}
      aria-busy={chargement || undefined}
      {...reste}
    >
      {chargement && (
        <span
          className={`spinner spinner--petit${spinnerClair ? ' spinner--bouton' : ''}`}
          aria-hidden="true"
        />
      )}
      {children}
    </button>
  )
}
