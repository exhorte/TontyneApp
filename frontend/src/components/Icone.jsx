/**
 * Icones SVG au trait, dessinees en interne (aucune dependance externe).
 * Style unifie : viewBox 24x24, trait 1.75, extremites arrondies, currentColor.
 * Utilisation : <Icone nom="tontines" taille={20} />
 */

const TRACES = {
  // --- Navigation ---
  tableau: <><rect x="3" y="3" width="7" height="9" rx="1.5" /><rect x="14" y="3" width="7" height="5" rx="1.5" /><rect x="14" y="12" width="7" height="9" rx="1.5" /><rect x="3" y="16" width="7" height="5" rx="1.5" /></>,
  tontines: <><path d="M16 20v-1.5a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4V20" /><circle cx="9" cy="6.5" r="3.5" /><path d="M22 20v-1.5a4 4 0 0 0-3-3.87" /><path d="M16.5 3.3a4 4 0 0 1 0 7.4" /></>,
  membre: <><circle cx="12" cy="7.5" r="4" /><path d="M4.5 20.5v-1a5 5 0 0 1 5-5h5a5 5 0 0 1 5 5v1" /></>,
  cycles: <><path d="M20.5 12a8.5 8.5 0 0 1-14.6 5.9" /><path d="M3.5 12a8.5 8.5 0 0 1 14.6-5.9" /><path d="M18.5 2.5v4h-4" /><path d="M5.5 21.5v-4h4" /></>,
  cotisations: <><ellipse cx="12" cy="6.5" rx="8" ry="3.5" /><path d="M4 6.5v11c0 1.9 3.6 3.5 8 3.5s8-1.6 8-3.5v-11" /><path d="M4 12c0 1.9 3.6 3.5 8 3.5s8-1.6 8-3.5" /></>,
  paiements: <><rect x="2.5" y="5" width="19" height="14" rx="2.5" /><path d="M2.5 10h19" /><path d="M6.5 15h3" /></>,
  recus: <><path d="M5 21V4.5A1.5 1.5 0 0 1 6.5 3h11A1.5 1.5 0 0 1 19 4.5V21l-2.3-1.6L14.4 21l-2.4-1.6L9.6 21l-2.3-1.6z" /><path d="M9 8h6" /><path d="M9 12h6" /></>,
  notifications: <><path d="M18 8.5a6 6 0 0 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 14.5 18 8.5" /><path d="M13.7 19.5a2 2 0 0 1-3.4 0" /></>,
  assistant: <><path d="M4 5.5h16a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5H9l-4.5 4v-4H4A1.5 1.5 0 0 1 2.5 16V7A1.5 1.5 0 0 1 4 5.5z" /><path d="M7.5 10h9" /><path d="M7.5 13.2h5.5" /></>,

  // --- Actions ---
  plus: <><path d="M12 5v14" /><path d="M5 12h14" /></>,
  actualiser: <><path d="M20.5 12a8.5 8.5 0 1 1-2.5-6" /><path d="M20.5 4v5h-5" /></>,
  imprimer: <><path d="M6.5 9V3.5h11V9" /><path d="M6.5 18H5a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-1.5" /><rect x="6.5" y="14" width="11" height="6.5" rx="1" /></>,
  oeil: <><path d="M2 12s3.8-6.5 10-6.5S22 12 22 12s-3.8 6.5-10 6.5S2 12 2 12" /><circle cx="12" cy="12" r="2.75" /></>,
  crayon: <><path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3z" /><path d="M14.5 5.5l3 3" /></>,
  corbeille: <><path d="M3.5 6h17" /><path d="M8.5 6V4.5A1.5 1.5 0 0 1 10 3h4a1.5 1.5 0 0 1 1.5 1.5V6" /><path d="M6 6l1 13a2 2 0 0 0 2 1.9h6a2 2 0 0 0 2-1.9L18 6" /><path d="M10.5 10.5v6" /><path d="M13.5 10.5v6" /></>,
  cadenas: <><rect x="4.5" y="10.5" width="15" height="10" rx="2" /><path d="M8 10.5V7a4 4 0 0 1 8 0v3.5" /></>,
  pause: <><circle cx="12" cy="12" r="9" /><path d="M10 9v6" /><path d="M14 9v6" /></>,
  lecture: <><circle cx="12" cy="12" r="9" /><path d="M10 8.5l5.5 3.5L10 15.5z" /></>,
  deconnexion: <><path d="M9.5 20.5H6a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2h3.5" /><path d="M15.5 16.5l4.5-4.5-4.5-4.5" /><path d="M20 12H9.5" /></>,
  calendrierPlus: <><rect x="3.5" y="5" width="17" height="16" rx="2" /><path d="M3.5 10h17" /><path d="M8 3v4" /><path d="M16 3v4" /><path d="M12 13.5v5" /><path d="M9.5 16h5" /></>,
  envoyer: <><path d="M21 3L10.5 13.5" /><path d="M21 3l-6.8 18-3.7-7.5L3 9.8z" /></>,

  // --- Etats & retours ---
  succes: <><circle cx="12" cy="12" r="9" /><path d="M8 12.2l2.7 2.7L16 9.5" /></>,
  erreur: <><circle cx="12" cy="12" r="9" /><path d="M12 7.5v5" /><path d="M12 16.2v.1" /></>,
  info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5.5" /><path d="M12 7.8v.1" /></>,
  alerte: <><path d="M10.3 3.9L2.5 17.5A2 2 0 0 0 4.2 20.5h15.6a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" /><path d="M12 9v4" /><path d="M12 16.7v.1" /></>,
  fermer: <><path d="M6 6l12 12" /><path d="M18 6L6 18" /></>,
  recherche: <><circle cx="11" cy="11" r="6.5" /><path d="M16 16l4.5 4.5" /></>,
  chevronDroite: <path d="M9.5 5.5l6.5 6.5-6.5 6.5" />,
  chevronGauche: <path d="M14.5 5.5L8 12l6.5 6.5" />,
  chevronBas: <path d="M5.5 9.5l6.5 6.5 6.5-6.5" />,
  boussole: <><circle cx="12" cy="12" r="9" /><path d="M15.5 8.5l-2 5-5 2 2-5z" /></>,
  bouclier: <><path d="M12 21.5s7.5-3.4 7.5-9.2V5.8L12 2.8 4.5 5.8v6.5c0 5.8 7.5 9.2 7.5 9.2z" /></>,
  menu: <><path d="M4 7h16" /><path d="M4 12h16" /><path d="M4 17h16" /></>,
};

export default function Icone({ nom, taille = 20, className = '', style, ...reste }) {
  const trace = TRACES[nom];
  if (!trace) return null;
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      width={taille}
      height={taille}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={`icone ${className}`.trim()}
      style={{ flexShrink: 0, ...style }}
      aria-hidden="true"
      focusable="false"
      {...reste}
    >
      {trace}
    </svg>
  );
}

/** Pastille ronde bleue contenant une icone blanche (listes d'actions). */
export function IconePastille({ nom, taille = 40 }) {
  return (
    <span className="icone-pastille" style={{ width: taille, height: taille }}>
      <Icone nom={nom} taille={Math.round(taille * 0.5)} />
    </span>
  );
}

/** Cercle gris clair contenant une icone (etats vides). */
export function IconeCercle({ nom, taille = 64 }) {
  return (
    <span className="icone-cercle" style={{ width: taille, height: taille }}>
      <Icone nom={nom} taille={Math.round(taille * 0.44)} />
    </span>
  );
}
