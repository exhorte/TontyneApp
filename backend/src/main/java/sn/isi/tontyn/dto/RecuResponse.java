package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.MethodePaiement;
import sn.isi.tontyn.model.Recu;

import java.time.LocalDateTime;

public record RecuResponse(Long id,
                           String numero,
                           LocalDateTime dateEmission,
                           double montant,
                           Long paiementId,
                           String referencePaiement,
                           MethodePaiement methode,
                           Long membreId,
                           String membreNom,
                           Long tontineId,
                           String tontineNom,
                           int cycleNumero) {

    public static RecuResponse from(Recu r) {
        var p = r.getPaiement();
        var c = p.getCotisation();
        var m = c.getMembre();
        var t = c.getCycle().getTontine();
        return new RecuResponse(r.getId(), r.getNumero(), r.getDateEmission(), r.getMontant(),
                p.getId(), p.getReference(), p.getMethode(),
                m.getId(), m.getUtilisateur().getPrenom() + " " + m.getUtilisateur().getNom(),
                t.getId(), t.getNom(), c.getCycle().getNumero());
    }
}
