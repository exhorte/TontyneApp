import Field from './Field.jsx'
import Button from './Button.jsx'
import Alert from './Alert.jsx'
import Icone from './Icone.jsx'

/**
 * Recherche d'un compte par numero de telephone : remplace la selection dans
 * un annuaire complet. `recherche` est la valeur renvoyee par le hook
 * useRechercheTelephone.
 */
export default function RechercheTelephone({ recherche, label = 'Numéro de téléphone du membre' }) {
  const { telephone, setTelephone, utilisateur, recherche: enCours, erreur, rechercher } = recherche

  const surSoumission = (evenement) => {
    evenement.preventDefault()
    rechercher()
  }

  return (
    <div className="champ">
      <form className="barre-filtres" onSubmit={surSoumission} style={{ marginBottom: 8 }}>
        <Field
          label={label}
          nom="telephoneRecherche"
          type="tel"
          valeur={telephone}
          onChange={(e) => setTelephone(e.target.value)}
          placeholder="+221 77 000 00 00"
          aide="Le numéro doit déjà être inscrit sur Tontyn."
        />
        <Button type="submit" chargement={enCours}>
          {<><Icone nom="recherche" taille={16} /> Vérifier</>}
        </Button>
      </form>

      {erreur && <Alert type="erreur">{erreur}</Alert>}

      {utilisateur && (
        <Alert type="succes">
          <strong>{`${utilisateur.prenom ?? ''} ${utilisateur.nom ?? ''}`.trim() || utilisateur.telephone}</strong>
          {' '}— {utilisateur.telephone}
        </Alert>
      )}
    </div>
  )
}
