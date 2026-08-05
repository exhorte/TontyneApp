package sn.isi.tontyn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequest(
        @NotBlank(message = "L'e-mail est obligatoire.")
        @Email(message = "Format d'e-mail invalide.")
        String email,

        @NotBlank(message = "Le code de verification est obligatoire.")
        @Pattern(regexp = "\\d{6}", message = "Le code doit comporter 6 chiffres.")
        String code) {}
