package sn.isi.tontyn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 50, message = "Le nom ne doit pas depasser 50 caracteres.")
        String nom,

        @NotBlank(message = "Le prenom est obligatoire.")
        @Size(max = 50, message = "Le prenom ne doit pas depasser 50 caracteres.")
        String prenom,

        @NotBlank(message = "L'e-mail est obligatoire.")
        @Email(message = "Format d'e-mail invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres.")
        String motDePasse,

        @Pattern(regexp = "^$|^[+0-9][0-9 ]{7,19}$",
                 message = "Numero de telephone invalide.")
        String telephone) {}
