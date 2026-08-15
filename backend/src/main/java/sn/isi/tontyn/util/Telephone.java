package sn.isi.tontyn.util;

/**
 * Normalisation des numeros de telephone.
 *
 * <p>Le numero servant d'identifiant de compte, il doit etre stocke sous une
 * forme unique : sans quoi « 77 123 45 67 », « 771234567 » et
 * « +221771234567 » designeraient trois comptes distincts. Toutes les saisies
 * sont donc ramenees au format international E.164.</p>
 */
public final class Telephone {

    /** Indicatif applique lorsqu'aucun n'est fourni (Senegal). */
    public static final String INDICATIF_DEFAUT = "+221";

    private Telephone() {
        // classe utilitaire
    }

    /**
     * Ramene une saisie libre au format international.
     *
     * @return le numero normalise, ou {@code null} si la saisie est vide
     */
    public static String normaliser(String saisie) {
        if (saisie == null) {
            return null;
        }
        // On ne conserve que les chiffres et un eventuel plus initial.
        String n = saisie.trim().replaceAll("[\\s.\\-()]", "");
        if (n.isEmpty()) {
            return null;
        }
        if (n.startsWith("00")) {
            n = "+" + n.substring(2);
        }
        if (n.startsWith("+")) {
            return "+" + n.substring(1).replaceAll("\\D", "");
        }
        n = n.replaceAll("\\D", "");
        if (n.isEmpty()) {
            return null;
        }
        // Numero national : on prefixe par l'indicatif par defaut.
        return INDICATIF_DEFAUT + n;
    }

    /** Controle sommaire de vraisemblance : indicatif suivi de 8 a 14 chiffres. */
    public static boolean estValide(String normalise) {
        return normalise != null && normalise.matches("\\+\\d{9,15}");
    }

    /**
     * Masque le numero pour l'affichage dans un message ou un journal :
     * {@code +221771234567} devient {@code +221 ** ** * 67}.
     */
    public static String masquer(String normalise) {
        if (normalise == null || normalise.length() < 4) {
            return "***";
        }
        return normalise.substring(0, 4) + " ** ** * " + normalise.substring(normalise.length() - 2);
    }
}
