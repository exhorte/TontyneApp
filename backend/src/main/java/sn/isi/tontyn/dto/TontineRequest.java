package sn.isi.tontyn.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TontineRequest(
        @NotBlank(message = "Le nom de la tontine est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres.")
        String nom,

        @Size(max = 500, message = "La description ne doit pas depasser 500 caracteres.")
        String description,

        @Positive(message = "Le montant de cotisation doit etre strictement positif.")
        double montantCotisation,

        @NotBlank(message = "La periodicite est obligatoire.")
        @Pattern(regexp = "QUOTIDIENNE|HEBDOMADAIRE|BIMENSUELLE|MENSUELLE|TRIMESTRIELLE",
                 message = "Periodicite attendue : QUOTIDIENNE, HEBDOMADAIRE, BIMENSUELLE, "
                         + "MENSUELLE ou TRIMESTRIELLE.")
        String periodicite,

        @Min(value = 2, message = "Une tontine compte au minimum 2 membres.")
        int nombreMembres,

        @Pattern(regexp = "ACTIVE|SUSPENDUE|CLOTUREE",
                 message = "Statut attendu : ACTIVE, SUSPENDUE ou CLOTUREE.")
        String statut
) {}
