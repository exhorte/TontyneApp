import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { IconePastille } from '../components/Icone.jsx'

const FRICTIONS = [
  {
    titre: 'Opacité',
    texte:
      "Qui a payé ? Qui doit encore cotiser ? Sans registre partagé, seule la personne qui tient le carnet connaît vraiment la situation.",
  },
  {
    titre: 'Litiges',
    texte:
      "Un montant mal noté, une page abîmée ou perdue, un souvenir qui diverge d'un membre à l'autre : les désaccords sont fréquents et difficiles à trancher.",
  },
  {
    titre: 'Charge du gestionnaire',
    texte:
      "Tout repose sur une seule personne : relancer les retardataires, calculer les tours, tenir les comptes à jour — souvent bénévolement.",
  },
]

const GARANTIES = [
  {
    icone: 'cadenas',
    titre: 'Authentification à deux facteurs',
    texte:
      "Chaque connexion est confirmée par un code envoyé par SMS. Même votre code PIN ne suffit pas à accéder à votre compte sans votre téléphone.",
  },
  {
    icone: 'bouclier',
    titre: "Vérification progressive de l'identité",
    texte:
      "Votre identité est vérifiée par étapes, et vos plafonds de cotisation augmentent au fur et à mesure. Impossible d'engager de grosses sommes sans être identifié.",
  },
  {
    icone: 'recus',
    titre: 'Traçabilité intégrale',
    texte:
      "Chaque cotisation et chaque paiement génère un reçu automatique. L'historique complet reste consultable à tout moment par les membres concernés.",
  },
]

const ETAPES = [
  {
    icone: 'membre',
    titre: 'Créez votre compte',
    texte: 'Inscrivez-vous avec votre numéro de téléphone et un code PIN à 4 chiffres.',
  },
  {
    icone: 'tontines',
    titre: 'Rejoignez ou créez une tontine',
    texte: 'Retrouvez une tontine existante ou créez la vôtre en quelques minutes.',
  },
  {
    icone: 'cotisations',
    titre: 'Cotisez à chaque cycle',
    texte: 'Réglez votre cotisation à la date prévue et suivez le cycle en temps réel.',
  },
  {
    icone: 'recus',
    titre: 'Consultez vos reçus',
    texte: "Chaque paiement est enregistré : retrouvez l'historique complet à tout moment.",
  },
]

/** Page d'accueil publique : vitrine de Tontyn pour les visiteurs non connectés. */
export default function Accueil() {
  const { estAuthentifie } = useAuth()

  // Deja connecte : on ne montre pas la vitrine, on va directement a l'application.
  if (estAuthentifie) {
    return <Navigate to="/tableau-de-bord" replace />
  }

  return (
    <div className="accueil">
      <header className="accueil-entete">
        <Link to="/" className="accueil-marque">
          <span className="logo-pastille" aria-hidden="true">T</span>
          <span>Tontyn</span>
        </Link>
        <nav className="accueil-entete__actions" aria-label="Authentification">
          <Link to="/login" className="btn btn--secondaire">Se connecter</Link>
          <Link to="/register" className="btn btn--principal">S'inscrire</Link>
        </nav>
      </header>

      <main id="contenu-principal">
        {/* Hero */}
        <section className="accueil-hero">
          <h1 className="accueil-hero__titre">
            Gérez vos tontines en toute confiance, du premier au dernier centime.
          </h1>
          <p className="accueil-hero__sous-titre">
            Tontyn remplace le carnet papier par un suivi numérique clair : chaque cotisation
            est enregistrée, chaque paiement est prouvé, et tous les membres voient la même
            vérité.
          </p>
          <div className="accueil-hero__actions">
            <Link to="/register" className="btn btn--principal btn--grand">S'inscrire</Link>
            <Link to="/login" className="btn btn--secondaire btn--grand">Se connecter</Link>
          </div>
        </section>

        {/* Le problème */}
        <section className="accueil-section accueil-probleme">
          <h2 className="accueil-section__titre">Le carnet papier a fait son temps</h2>
          <p className="accueil-section__sous-titre">
            La tontine est une pratique d'épargne solidaire qui repose sur la confiance —
            mais gérée sur papier, cette confiance est fragile.
          </p>
          <div className="accueil-grille accueil-grille--3">
            {FRICTIONS.map((f) => (
              <div key={f.titre} className="accueil-carte-simple">
                <h3>{f.titre}</h3>
                <p>{f.texte}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Garanties (mecanismes de confiance) */}
        <section className="accueil-section accueil-garanties">
          <h2 className="accueil-section__titre">Nos garanties</h2>
          <p className="accueil-section__sous-titre">
            Trois mécanismes protègent chaque cotisation, du premier versement à la clôture
            du cycle.
          </p>
          <div className="accueil-grille accueil-grille--3">
            {GARANTIES.map((g) => (
              <div key={g.titre} className="carte accueil-carte-garantie">
                <IconePastille nom={g.icone} taille={44} />
                <h3>{g.titre}</h3>
                <p>{g.texte}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Comment ca marche */}
        <section className="accueil-section accueil-etapes">
          <h2 className="accueil-section__titre">Comment ça marche</h2>
          <p className="accueil-section__sous-titre">
            Quatre étapes suffisent pour démarrer, sans compétence technique particulière.
          </p>
          <div className="accueil-grille accueil-grille--4">
            {ETAPES.map((e, i) => (
              <div key={e.titre} className="accueil-etape">
                <div className="accueil-etape__icone">
                  <IconePastille nom={e.icone} taille={40} />
                  <span className="accueil-etape__numero" aria-hidden="true">{i + 1}</span>
                </div>
                <h3>{e.titre}</h3>
                <p>{e.texte}</p>
              </div>
            ))}
          </div>
        </section>

        {/* CTA finale */}
        <section className="accueil-cta-finale">
          <h2>Prêt à organiser votre tontine sans carnet ni malentendu ?</h2>
          <p>Créez votre compte gratuitement et invitez les membres de votre tontine.</p>
          <Link to="/register" className="btn btn--principal btn--grand">S'inscrire</Link>
        </section>
      </main>

      <footer className="accueil-pied">
        <div className="accueil-pied__marque">
          <span className="logo-pastille" aria-hidden="true">T</span>
          <span>Tontyn</span>
        </div>
        <p className="accueil-pied__accroche">
          Plateforme sécurisée de gestion des tontines communautaires.
        </p>
        <nav className="accueil-pied__liens" aria-label="Liens utiles">
          <Link to="/login">Se connecter</Link>
          <Link to="/register">S'inscrire</Link>
        </nav>
        <p className="accueil-pied__copyright">© {new Date().getFullYear()} Tontyn</p>
      </footer>
    </div>
  )
}
