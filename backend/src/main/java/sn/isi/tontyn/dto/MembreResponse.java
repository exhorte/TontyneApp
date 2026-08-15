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
                             String tontineNom,
                             /**
                              * Score de fiabilite de paiement (0-100), ou {@code null} si non
                              * encore calculable (membre trop recent, voir ScoreFiabiliteService)
                              * ou si l'appelant courant n'a pas le droit de le consulter : le
                              * score d'un membre ne regarde que le gestionnaire de sa tontine et
                              * le membre lui-meme.
                              */
                             Integer score,
                             /** Niveau de confiance du score ci-dessus ; null dans les memes cas. */
                             String niveauConfiance) {

    /** Sans score : reponse utilisee quand l'appelant n'est pas autorise a le voir. */
    public static MembreResponse from(Membre m) {
        return from(m, null, null);
    }

    /** Avec score, reserve au gestionnaire de la tontine concernee ou au membre lui-meme. */
    public static MembreResponse from(Membre m, Integer score, String niveauConfiance) {
        var u = m.getUtilisateur();
        return new MembreResponse(m.getId(), m.getDateAdhesion(), m.getRoleGroupe(),
                m.getOrdreTour(), m.getStatut(),
                u.getId(), u.getPrenom() + " " + u.getNom(), u.getTelephone(),
                m.getTontine().getId(), m.getTontine().getNom(),
                score, niveauConfiance);
    }
}
