package sn.isi.tontyn.model;

/**
 * Roles internes a une tontine, portes par l'entite {@code Membre}.
 *
 * <p>Il importe de ne pas confondre ce role avec celui de l'entite
 * {@code Utilisateur} : le premier vaut a l'interieur d'un groupe determine,
 * le second a l'echelle de la plateforme. Le gestionnaire anime sa tontine ;
 * il n'exerce aucun pouvoir sur celles auxquelles il n'appartient pas.</p>
 *
 * <p>Ces valeurs sont volontairement conservees sous forme de chaines afin de
 * ne pas modifier le contrat des DTO existants.</p>
 */
public final class RoleGroupe {

    /** Createur ou co-responsable de la tontine : tous les droits sur celle-ci. */
    public static final String GESTIONNAIRE = "GESTIONNAIRE";

    /** Participant simple : cotise et consulte. */
    public static final String MEMBRE = "MEMBRE";

    private RoleGroupe() {
        // classe utilitaire
    }

    /** Normalise une valeur recue, en retombant sur MEMBRE si elle est absente ou inconnue. */
    public static String normaliser(String valeur) {
        if (valeur == null) {
            return MEMBRE;
        }
        String v = valeur.trim().toUpperCase();
        return GESTIONNAIRE.equals(v) ? GESTIONNAIRE : MEMBRE;
    }

    public static boolean estGestionnaire(String valeur) {
        return GESTIONNAIRE.equalsIgnoreCase(valeur == null ? null : valeur.trim());
    }
}
