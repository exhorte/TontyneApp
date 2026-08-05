package sn.isi.tontyn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.isi.tontyn.model.MethodePaiement;

/** Initiation d'un paiement pour une cotisation. */
public record PaiementRequest(
        @NotNull(message = "L'identifiant de la cotisation est obligatoire.")
        Long cotisationId,

        @NotNull(message = "La methode de paiement est obligatoire (ORANGE_MONEY ou WAVE).")
        MethodePaiement methode,

        /** Optionnel : reference de l'operateur, generee si absente. */
        @Size(max = 50, message = "La reference ne doit pas depasser 50 caracteres.")
        String reference
) {}
