package sn.isi.tontyn.model;

/** Etat d'instruction d'une demande de verification d'identite. */
public enum StatutVerification {

    /** Piece soumise, en attente d'examen par l'administrateur de la plateforme. */
    EN_ATTENTE,

    /** Piece acceptee : le compte est desormais deplafonne. */
    VALIDEE,

    /** Piece refusee. Le motif est communique a l'utilisateur, qui peut recommencer. */
    REJETEE
}
