package sn.isi.tontyn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Association d'une adresse electronique au compte, depuis le profil.
 *
 * <p>L'adresse n'est pas retenue immediatement : elle demeure en attente
 * jusqu'a ce que le code qui lui a ete adresse soit confirme. Sans cette
 * precaution, un utilisateur pourrait declarer l'adresse d'un tiers.</p>
 */
public final class EmailRequest {

    private EmailRequest() {
    }

    /** Demande d'association : declenche l'envoi d'un code a l'adresse indiquee. */
    public record Ajout(
            @NotBlank(message = "L'adresse electronique est obligatoire.")
            @Email(message = "Format d'adresse electronique invalide.")
            String email) {}

    /** Confirmation de l'adresse en attente. */
    public record Confirmation(
            @NotBlank(message = "Le code de verification est obligatoire.")
            @Pattern(regexp = "\\d{6}", message = "Le code doit comporter 6 chiffres.")
            String code) {}
}
