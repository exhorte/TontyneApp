package sn.isi.tontyn.model;

/**
 * Roles internes a une tontine, portes par l'entite {@code Membre}.
 *
 * <p>Ces valeurs sont volontairement conservees sous forme de chaines afin de
 * ne pas modifier le contrat des DTO existants. Elles delimitent la portee des
 * droits : un administrateur de tontine ne dispose d'aucun pouvoir sur les
 * tontines auxquelles il n'appartient pas.</p>
 */
public final class RoleGroupe {

    /** Createur ou co-responsable de la tontine : tous les droits sur celle-ci. */
    public static final String ADMINISTRATEUR = "ADMINISTRATEUR";

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
        return ADMINISTRATEUR.equals(v) ? ADMINISTRATEUR : MEMBRE;
    }

    public static boolean estAdministrateur(String valeur) {
        return ADMINISTRATEUR.equalsIgnoreCase(valeur == null ? null : valeur.trim());
    }
}
