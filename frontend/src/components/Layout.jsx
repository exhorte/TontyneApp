import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { notificationsApi } from '../api/notifications.js'
import { ROLES_GESTION } from '../utils/constants.js'
import { initiales } from '../utils/format.js'
import Button from './Button.jsx'
import Icone from './Icone.jsx'

const ENTREES_MENU = [
  { chemin: '/tableau-de-bord', libelle: 'Tableau de bord', icone: 'tableau' },
  { chemin: '/tontines', libelle: 'Tontines', icone: 'tontines' },
  { chemin: '/membres', libelle: 'Membres', icone: 'membre' },
  { chemin: '/cycles', libelle: 'Cycles', icone: 'cycles' },
  { chemin: '/cotisations', libelle: 'Cotisations', icone: 'cotisations' },
  { chemin: '/paiements', libelle: 'Paiements', icone: 'paiements' },
  { chemin: '/recus', libelle: 'Reçus', icone: 'recus' },
  { chemin: '/notifications', libelle: 'Notifications', icone: 'notifications', compteur: true },
]

const LIBELLES_ROLE = {
  ADMINISTRATEUR: 'Administrateur',
  GESTIONNAIRE: 'Gestionnaire',
  MEMBRE: 'Membre',
}

/** Coquille applicative : barre superieure + menu lateral + contenu de la route. */
export default function Layout() {
  const { utilisateur, utilisateurId, role, deconnexion } = useAuth()
  const navigate = useNavigate()
  const emplacement = useLocation()
  const [menuOuvert, setMenuOuvert] = useState(false)
  const [nonLues, setNonLues] = useState(0)

  // Le menu mobile se referme a chaque changement de page.
  useEffect(() => {
    setMenuOuvert(false)
  }, [emplacement.pathname])

  // Compteur de notifications non lues, rafraichi a chaque navigation.
  useEffect(() => {
    if (!utilisateurId) {
      setNonLues(0)
      return
    }
    let annule = false
    notificationsApi
      .compterNonLues(utilisateurId)
      .then((n) => {
        if (!annule) setNonLues(n)
      })
      .catch(() => {
        if (!annule) setNonLues(0)
      })
    return () => {
      annule = true
    }
  }, [utilisateurId, emplacement.pathname])

  const seDeconnecter = () => {
    deconnexion()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell">
      <a className="lien-evitement" href="#contenu-principal">
        Aller au contenu principal
      </a>

      <header className="barre-sup">
        <button
          type="button"
          className="bouton-menu"
          onClick={() => setMenuOuvert((o) => !o)}
          aria-label={menuOuvert ? 'Fermer le menu' : 'Ouvrir le menu'}
          aria-expanded={menuOuvert}
        >
          <Icone nom="menu" taille={20} />
        </button>

        <Link to="/tableau-de-bord" className="barre-sup__marque">
          <span className="logo-pastille" aria-hidden="true">TS</span>
          <span>TontineSafe</span>
        </Link>

        <div className="barre-sup__espace" />

        <div className="barre-sup__utilisateur">
          <div className="avatar" aria-hidden="true">{initiales(utilisateur?.email)}</div>
          <div className="barre-sup__identite">
            <span className="barre-sup__email" title={utilisateur?.email}>
              {utilisateur?.email}
            </span>
            <span className="barre-sup__role">{LIBELLES_ROLE[role] || role}</span>
          </div>
          <Button variante="secondaire" taille="petit" onClick={seDeconnecter}>
            Déconnexion
          </Button>
        </div>
      </header>

      <div
        className={`voile-menu${menuOuvert ? ' voile-menu--visible' : ''}`}
        onClick={() => setMenuOuvert(false)}
        aria-hidden="true"
      />

      <nav
        className={`menu-lateral${menuOuvert ? ' menu-lateral--ouvert' : ''}`}
        aria-label="Navigation principale"
      >
        <div className="menu-lateral__titre">Navigation</div>
        {ENTREES_MENU.map((entree) => (
          <NavLink
            key={entree.chemin}
            to={entree.chemin}
            className={({ isActive }) => `menu-lien${isActive ? ' menu-lien--actif' : ''}`}
          >
            <span className="menu-lien__icone" aria-hidden="true"><Icone nom={entree.icone} taille={20} /></span>
            <span>{entree.libelle}</span>
            {entree.compteur && nonLues > 0 && (
              <span className="menu-lien__compteur" aria-label={`${nonLues} non lues`}>
                {nonLues}
              </span>
            )}
          </NavLink>
        ))}

        <div className="menu-lateral__titre">Session</div>
        <div style={{ padding: '4px 10px 12px' }}>
          <span className="badge-role">{LIBELLES_ROLE[role] || role}</span>
          <p className="texte-discret" style={{ marginTop: 8, fontSize: '0.76rem' }}>
            {ROLES_GESTION.includes(role)
              ? 'Vous pouvez créer et modifier les tontines, membres et cycles.'
              : 'Vous pouvez cotiser et régler vos paiements.'}
          </p>
        </div>
      </nav>

      <main className="contenu" id="contenu-principal">
        <Outlet />
      </main>
    </div>
  )
}
