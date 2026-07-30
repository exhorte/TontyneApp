package sn.isi.tontinesafe.exception;

/** Levee quand une regle de gestion interdit l'operation -> HTTP 409. */
public class ConflitMetierException extends RuntimeException {

    public ConflitMetierException(String message) {
        super(message);
    }
}
