package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Ajout d'un membre via la tontine (POST /api/tontines/{tontineId}/membres). */
public record AjoutMembreRequest(
        @NotNull(message = "L'identifiant de l'utilisateur est obligatoire.")
        Long utilisateurId,

        @Pattern(regexp = "GESTIONNAIRE|MEMBRE",
                 message = "Role attendu dans le groupe : GESTIONNAIRE ou MEMBRE.")
        String roleGroupe,

        /** Optionnel : attribue automatiquement si absent. */
        @Positive(message = "L'ordre de tour doit etre un entier positif.")
        Integer ordreTour
) {}
