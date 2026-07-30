package sn.isi.tontinesafe.dto;

import sn.isi.tontinesafe.model.Tontine;

import java.time.LocalDate;

public record TontineResponse(Long id,
                              String nom,
                              String description,
                              double montantCotisation,
                              String periodicite,
                              int nombreMembres,
                              long nombreMembresInscrits,
                              long nombreCycles,
                              LocalDate dateCreation,
                              String statut) {

    public static TontineResponse from(Tontine t, long membresInscrits, long cycles) {
        return new TontineResponse(t.getId(), t.getNom(), t.getDescription(),
                t.getMontantCotisation(), t.getPeriodicite(), t.getNombreMembres(),
                membresInscrits, cycles, t.getDateCreation(), t.getStatut());
    }
}
