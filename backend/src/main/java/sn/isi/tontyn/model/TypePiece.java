package sn.isi.tontyn.model;

/**
 * Pieces d'identite acceptees pour le deplafonnement d'un compte.
 *
 * <p>Chaque type impose ses propres faces a photographier. Le passeport se
 * suffit de sa page d'identite, qui porte l'ensemble des donnees d'etat civil
 * ainsi que la zone de lecture optique ; la carte nationale d'identite et le
 * permis de conduire repartissent au contraire ces informations entre le recto
 * et le verso, et exigent donc les deux.</p>
 */
public enum TypePiece {

    /** Carte nationale d'identite, delivree au Senegal par l'ANSD. */
    CNI("Carte nationale d'identité", true),

    /** Passeport : la seule page d'identite suffit. */
    PASSEPORT("Passeport", false),

    /** Permis de conduire, delivre au Senegal par l'ANASER. */
    PERMIS_CONDUIRE("Permis de conduire", true);

    private final String libelle;
    private final boolean versoRequis;

    TypePiece(String libelle, boolean versoRequis) {
        this.libelle = libelle;
        this.versoRequis = versoRequis;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Le verso doit-il etre photographie pour ce type de piece ? */
    public boolean exigeVerso() {
        return versoRequis;
    }
}
