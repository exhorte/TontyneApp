package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Premier facteur : numero de telephone et code PIN. */
public record LoginRequest(
        @NotBlank(message = "Le numero de telephone est obligatoire.")
        @Pattern(regexp = "^[+0-9][0-9 .\\-()]{6,19}$",
                 message = "Numero de telephone invalide.")
        String telephone,

        @NotBlank(message = "Le code PIN est obligatoire.")
        @Pattern(regexp = "\\d{4}", message = "Le code PIN doit comporter exactement 4 chiffres.")
        String codePin) {}
