package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.Utilisateur;

public record UtilisateurResponse(Long id,
                                  String nom,
                                  String prenom,
                                  String email,
                                  String telephone,
                                  String role) {

    public static UtilisateurResponse from(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getNom(), u.getPrenom(),
                u.getEmail(), u.getTelephone(), u.getRole().name());
    }
}
