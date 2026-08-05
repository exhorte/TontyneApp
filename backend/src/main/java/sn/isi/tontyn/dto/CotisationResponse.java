package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.Cotisation;

import java.time.LocalDateTime;

public record CotisationResponse(Long id,
                                 double montant,
                                 LocalDateTime date,
                                 String statut,
                                 Long cycleId,
                                 int cycleNumero,
                                 Long membreId,
                                 String membreNom,
                                 Long tontineId,
                                 String tontineNom) {

    public static CotisationResponse from(Cotisation c) {
        var m = c.getMembre();
        var t = c.getCycle().getTontine();
        return new CotisationResponse(c.getId(), c.getMontant(), c.getDate(), c.getStatut(),
                c.getCycle().getId(), c.getCycle().getNumero(),
                m.getId(), m.getUtilisateur().getPrenom() + " " + m.getUtilisateur().getNom(),
                t.getId(), t.getNom());
    }
}
