package sn.isi.tontinesafe.exception;

/** Levee quand une entite demandee n'existe pas -> HTTP 404. */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }

    public RessourceIntrouvableException(String entite, Long id) {
        super(entite + " introuvable pour l'identifiant " + id + ".");
    }
}
