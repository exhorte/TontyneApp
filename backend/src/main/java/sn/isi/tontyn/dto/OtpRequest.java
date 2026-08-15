package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Second facteur : code a usage unique recu par message court. */
public record OtpRequest(
        @NotBlank(message = "Le numero de telephone est obligatoire.")
        @Pattern(regexp = "^[+0-9][0-9 .\\-()]{6,19}$",
                 message = "Numero de telephone invalide.")
        String telephone,

        @NotBlank(message = "Le code de verification est obligatoire.")
        @Pattern(regexp = "\\d{6}", message = "Le code doit comporter 6 chiffres.")
        String code) {}
