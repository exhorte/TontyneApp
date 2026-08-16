package sn.isi.tontyn.dto;

/**
 * Reponse de l'assistant conversationnel.
 *
 * <p>{@code disponible} vaut {@code false} pour toute situation ou aucune
 * vraie reponse du modele n'a pu etre produite (assistant desactive, cle
 * d'API absente, question vide ou trop longue, limite d'appels atteinte,
 * echec de l'appel a DeepSeek) : {@code reponse} porte alors un message
 * explicite pour l'utilisateur, jamais une trace technique.</p>
 */
public record AssistantResponse(String reponse, boolean disponible) {}
