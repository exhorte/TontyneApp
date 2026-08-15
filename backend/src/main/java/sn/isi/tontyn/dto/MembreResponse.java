package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.Membre;

import java.time.LocalDate;

public record MembreResponse(Long id,
                             LocalDate dateAdhesion,
                             String roleGroupe,
                             int ordreTour,
                             String statut,
                             Long utilisateurId,
                             String nomComplet,
                             String telephone,
                             Long tontineId,
                             String tontineNom) {

    public static MembreResponse from(Membre m) {
        var u = m.getUtilisateur();
        return new MembreResponse(m.getId(), m.getDateAdhesion(), m.getRoleGroupe(),
                m.getOrdreTour(), m.getStatut(),
                u.getId(), u.getPrenom() + " " + u.getNom(), u.getTelephone(),
                m.getTontine().getId(), m.getTontine().getNom());
    }
}
