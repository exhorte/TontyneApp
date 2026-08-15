package sn.isi.tontyn.model;

/**
 * Niveau de verification d'un compte, dont depend le plafond applicable.
 *
 * <p>Le dispositif transpose a l'echelle d'une tontine le principe retenu par
 * la BCEAO pour la monnaie electronique : les plafonds ne sont releves qu'au
 * benefice des personnes regulierement identifiees, et le sont par paliers
 * (Instruction n°003-03-2025 du 18 mars 2025). L'utilisateur ne subit ainsi un
 * controle qu'a la mesure de ce qu'il engage.</p>
 *
 * <p>Les montants associes ne figurent pas ici : ils relevent de la
 * configuration, et non du domaine.</p>
 */
public enum NiveauVerification {

    /** Compte cree, numero de telephone confirme. Plafond le plus bas. */
    NON_VERIFIE(0),

    /** Etat civil declare et portrait fourni. Plafond intermediaire. */
    SIMPLE(1),

    /** Piece d'identite produite et validee. Aucun plafond. */
    COMPLETE(2);

    private final int rang;

    NiveauVerification(int rang) {
        this.rang = rang;
    }

    public int getRang() {
        return rang;
    }

    /** Ce niveau atteint-il au moins celui exige ? */
    public boolean auMoins(NiveauVerification exige) {
        return exige != null && this.rang >= exige.rang;
    }

    public String getLibelle() {
        return switch (this) {
            case NON_VERIFIE -> "Non vérifié";
            case SIMPLE -> "Vérification simple";
            case COMPLETE -> "Vérification complète";
        };
    }
}
