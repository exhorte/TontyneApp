package sn.isi.tontinesafe.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Generation automatique des cycles d'une tontine : un cycle par membre actif,
 * chaque membre etant beneficiaire selon son ordre de tour.
 */
public record GenerationCyclesRequest(
        @NotNull(message = "La date de debut du premier cycle est obligatoire.")
        LocalDate dateDebut
) {}
