package sn.isi.tontinesafe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Envoi d'une notification a un utilisateur. */
public record NotificationRequest(
        @NotNull(message = "L'identifiant du destinataire est obligatoire.")
        Long utilisateurId,

        @NotBlank(message = "Le type de notification est obligatoire.")
        @Size(max = 50, message = "Le type ne doit pas depasser 50 caracteres.")
        String type,

        @NotBlank(message = "Le message est obligatoire.")
        @Size(max = 1000, message = "Le message ne doit pas depasser 1000 caracteres.")
        String message,

        @Pattern(regexp = "EMAIL|SMS|PUSH",
                 message = "Canal attendu : EMAIL, SMS ou PUSH.")
        String canal
) {}
