package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.Cotisation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CotisationResponse(Long id,
                                 double montant,
                                 /** Penalite de retard appliquee ; 0 si la cotisation est a jour. */
                                 double penalite,
                                 /** Somme exigible : montant + penalite. */
                                 double montantDu,
                                 LocalDateTime date,
                                 String statut,
                                 /** Echeance de la cotisation, soit la fin du cycle. */
                                 LocalDate echeance,
                                 Long cycleId,
                                 int cycleNumero,
                                 Long membreId,
                                 String membreNom,
                                 Long tontineId,
                                 String tontineNom) {

    public static CotisationResponse from(Cotisation c) {
        var m = c.getMembre();
        var t = c.getCycle().getTontine();
        return new CotisationResponse(c.getId(), c.getMontant(), c.getPenalite(), c.montantDu(),
                c.getDate(), c.getStatut(), c.getCycle().getDateFin(),
                c.getCycle().getId(), c.getCycle().getNumero(),
                m.getId(), m.getUtilisateur().getPrenom() + " " + m.getUtilisateur().getNom(),
                t.getId(), t.getNom());
    }
}
