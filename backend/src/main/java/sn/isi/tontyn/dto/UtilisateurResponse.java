package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.Utilisateur;

/**
 * Profil renvoye au client. Le numero de telephone est l'identifiant ;
 * l'adresse electronique, facultative, peut etre absente ou en attente de
 * confirmation.
 */
public record UtilisateurResponse(Long id,
                                  String nom,
                                  String prenom,
                                  String telephone,
                                  String email,
                                  boolean emailVerifie,
                                  String emailEnAttente,
                                  String role) {

    public static UtilisateurResponse from(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getNom(), u.getPrenom(),
                u.getTelephone(), u.getEmail(), u.isEmailVerifie(),
                u.getEmailEnAttente(), u.getRole().name());
    }
}
