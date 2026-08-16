package sn.isi.tontyn.exception;

/**
 * Echec interne de l'appel a l'assistant conversationnel : DeepSeek injoignable,
 * en erreur, ou reponse invalide/vide.
 *
 * <p>Cette exception ne remonte jamais jusqu'au client HTTP : elle est
 * systematiquement interceptee par {@code AssistantService}, qui renvoie une
 * reponse HTTP 200 avec un message degrade plutot que de faire tomber la
 * requete. Elle existe pour que {@code ClientDeepSeek} ait un contrat simple :
 * soit une reponse exploitable, soit cette exception — jamais un retour
 * {@code null} silencieux.</p>
 */
public class AssistantIndisponibleException extends RuntimeException {

    public AssistantIndisponibleException(String message) {
        super(message);
    }

    public AssistantIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
