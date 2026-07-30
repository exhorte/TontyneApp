import { useId } from 'react'

/**
 * Champ de formulaire accessible (label lie, aria-invalid, message d'erreur
 * annonce). `erreur` provient directement de ApiError.champs[nom].
 */
export default function Field({
  label,
  nom,
  type = 'text',
  valeur,
  onChange,
  erreur,
  aide,
  requis = false,
  options,
  children,
  ...reste
}) {
  const idAuto = useId()
  const id = `champ-${nom || idAuto}`
  const idAide = `${id}-aide`
  const idErreur = `${id}-erreur`

  const decrit = [aide ? idAide : null, erreur ? idErreur : null].filter(Boolean).join(' ')

  const proprietesCommunes = {
    id,
    name: nom,
    value: valeur ?? '',
    onChange,
    className: `champ__controle${erreur ? ' champ__controle--erreur' : ''}`,
    'aria-invalid': erreur ? true : undefined,
    'aria-describedby': decrit || undefined,
    required: requis || undefined,
    ...reste,
  }

  return (
    <div className="champ">
      {label && (
        <label className="champ__label" htmlFor={id}>
          {label}
          {requis && <span className="champ__requis" aria-hidden="true">*</span>}
        </label>
      )}

      {children ? (
        // Cas particulier (groupe de boutons radio, contenu sur mesure).
        children
      ) : type === 'select' ? (
        <select {...proprietesCommunes}>
          {options?.map((option) => (
            <option key={option.valeur} value={option.valeur}>
              {option.libelle}
            </option>
          ))}
        </select>
      ) : type === 'textarea' ? (
        <textarea {...proprietesCommunes} />
      ) : (
        <input type={type} {...proprietesCommunes} />
      )}

      {aide && !erreur && (
        <p className="champ__aide" id={idAide}>{aide}</p>
      )}
      {erreur && (
        <p className="champ__erreur" id={idErreur}>{erreur}</p>
      )}
    </div>
  )
}
