package sn.isi.tontyn.dto;

import sn.isi.tontyn.model.Tontine;

import java.time.LocalDate;

/**
 * Representation d'une tontine renvoyee au client.
 *
 * <p>Les deux derniers champs decrivent la position de l'utilisateur
 * <em>courant</em> vis-a-vis de cette tontine. Ils sont indispensables depuis la
 * refonte des roles : les droits de gestion n'etant plus portes par un role
 * global, l'interface ne peut plus deduire d'un simple jeton ce qu'elle doit
 * afficher. C'est donc le serveur qui l'indique, tontine par tontine.</p>
 */
public record TontineResponse(Long id,
                              String nom,
                              String description,
                              double montantCotisation,
                              String periodicite,
                              int nombreMembres,
                              long nombreMembresInscrits,
                              long nombreCycles,
                              LocalDate dateCreation,
                              LocalDate dateDebut,
                              String statut,
                              /** L'utilisateur courant administre-t-il cette tontine ? */
                              boolean administrateur,
                              /** L'utilisateur courant participe-t-il a cette tontine ? */
                              boolean membre) {

    public static TontineResponse from(Tontine t, long membresInscrits, long cycles,
                                       boolean administrateur, boolean membre) {
        return new TontineResponse(t.getId(), t.getNom(), t.getDescription(),
                t.getMontantCotisation(), t.getPeriodicite(), t.getNombreMembres(),
                membresInscrits, cycles, t.getDateCreation(), t.getDateDebut(), t.getStatut(),
                administrateur, membre);
    }
}
