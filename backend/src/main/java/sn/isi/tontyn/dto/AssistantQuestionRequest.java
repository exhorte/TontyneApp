package sn.isi.tontyn.dto;

/**
 * Question posee a l'assistant conversationnel (POST /api/assistant/question).
 *
 * <p>Volontairement sans annotation de validation : la longueur et le
 * caractere vide sont controles par {@code AssistantService}, qui renvoie un
 * message degrade plutot qu'une erreur 400 — une seule forme de reponse, plus
 * simple a afficher cote panneau de discussion.</p>
 *
 * <p>Ne porte aucun identifiant d'utilisateur ou de membre : la portee de la
 * reponse est exclusivement celle du jeton JWT de la requete.</p>
 */
public record AssistantQuestionRequest(String question) {}
