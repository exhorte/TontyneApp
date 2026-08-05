package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Enregistrement d'une cotisation d'un membre pour un cycle. */
public record CotisationRequest(
        @NotNull(message = "L'identifiant du cycle est obligatoire.")
        Long cycleId,

        @NotNull(message = "L'identifiant du membre est obligatoire.")
        Long membreId,

        /** Optionnel : par defaut le montant de cotisation de la tontine. */
        @Positive(message = "Le montant doit etre strictement positif.")
        Double montant
) {}
