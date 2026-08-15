package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inscription : le numero de telephone tient lieu d'identifiant, le code PIN
 * de mot de passe. L'adresse electronique n'est pas demandee a ce stade ;
 * l'utilisateur pourra l'ajouter ensuite depuis son profil.
 */
public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 50, message = "Le nom ne doit pas depasser 50 caracteres.")
        String nom,

        @NotBlank(message = "Le prenom est obligatoire.")
        @Size(max = 50, message = "Le prenom ne doit pas depasser 50 caracteres.")
        String prenom,

        @NotBlank(message = "Le numero de telephone est obligatoire.")
        @Pattern(regexp = "^[+0-9][0-9 .\\-()]{6,19}$",
                 message = "Numero de telephone invalide.")
        String telephone,

        @NotBlank(message = "Le code PIN est obligatoire.")
        @Pattern(regexp = "\\d{4}", message = "Le code PIN doit comporter exactement 4 chiffres.")
        String codePin) {}
