package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.MethodePaiement;
import sn.isi.tontyn.model.Paiement;

import java.time.LocalDateTime;

public record PaiementResponse(Long id,
                               double montant,
                               LocalDateTime date,
                               MethodePaiement methode,
                               String reference,
                               String statut,
                               Long cotisationId,
                               Long membreId,
                               String membreNom,
                               Long tontineId,
                               String tontineNom,
                               Long recuId) {

    public static PaiementResponse from(Paiement p, Long recuId) {
        var m = p.getCotisation().getMembre();
        var t = p.getCotisation().getCycle().getTontine();
        return new PaiementResponse(p.getId(), p.getMontant(), p.getDate(), p.getMethode(),
                p.getReference(), p.getStatut(), p.getCotisation().getId(),
                m.getId(), m.getUtilisateur().getPrenom() + " " + m.getUtilisateur().getNom(),
                t.getId(), t.getNom(), recuId);
    }
}
