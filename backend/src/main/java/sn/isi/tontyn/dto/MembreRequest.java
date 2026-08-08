package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Ajout d'un membre a une tontine (POST /api/membres). */
public record MembreRequest(
        @NotNull(message = "L'identifiant de la tontine est obligatoire.")
        Long tontineId,

        @NotNull(message = "L'identifiant de l'utilisateur est obligatoire.")
        Long utilisateurId,

        @Pattern(regexp = "ADMINISTRATEUR|MEMBRE",
                 message = "Role attendu dans le groupe : ADMINISTRATEUR ou MEMBRE.")
        String roleGroupe,

        /** Optionnel : attribue automatiquement si absent. */
        @Positive(message = "L'ordre de tour doit etre un entier positif.")
        Integer ordreTour
) {}
