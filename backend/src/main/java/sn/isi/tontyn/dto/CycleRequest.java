package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** Creation ou mise a jour manuelle d'un cycle. */
public record CycleRequest(
        @NotNull(message = "L'identifiant de la tontine est obligatoire.")
        Long tontineId,

        @Positive(message = "Le numero de cycle doit etre un entier positif.")
        int numero,

        @NotNull(message = "La date de debut est obligatoire.")
        LocalDate dateDebut,

        LocalDate dateFin,

        /** Membre beneficiaire du cycle (optionnel a la creation). */
        Long beneficiaireId,

        @Pattern(regexp = "PLANIFIE|EN_COURS|CLOTURE",
                 message = "Statut attendu : PLANIFIE, EN_COURS ou CLOTURE.")
        String statut
) {}
