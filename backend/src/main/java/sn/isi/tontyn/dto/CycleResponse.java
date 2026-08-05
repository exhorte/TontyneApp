package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.Cycle;

import java.time.LocalDate;

public record CycleResponse(Long id,
                            int numero,
                            LocalDate dateDebut,
                            LocalDate dateFin,
                            String statut,
                            Long tontineId,
                            String tontineNom,
                            Long beneficiaireId,
                            String beneficiaireNom,
                            double montantCollecte) {

    public static CycleResponse from(Cycle c, double montantCollecte) {
        var b = c.getBeneficiaire();
        return new CycleResponse(c.getId(), c.getNumero(), c.getDateDebut(), c.getDateFin(),
                c.getStatut(), c.getTontine().getId(), c.getTontine().getNom(),
                b != null ? b.getId() : null,
                b != null ? b.getUtilisateur().getPrenom() + " " + b.getUtilisateur().getNom() : null,
                montantCollecte);
    }
}
