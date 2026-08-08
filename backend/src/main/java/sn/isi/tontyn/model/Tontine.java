package sn.isi.tontyn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "tontine")
@Getter @Setter @NoArgsConstructor
public class Tontine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String description;
    private double montantCotisation;
    private String periodicite;       // ex: MENSUELLE, HEBDOMADAIRE
    private int nombreMembres;
    /** Date d'enregistrement dans la plateforme. */
    private LocalDate dateCreation = LocalDate.now();

    /**
     * Date a laquelle la tontine commence effectivement a fonctionner.
     * Elle sert de point de depart au premier cycle de cotisation et se
     * distingue de la date de creation : une tontine peut etre preparee
     * plusieurs semaines avant son demarrage, ou saisie apres coup.
     */
    private LocalDate dateDebut;
    private String statut = "ACTIVE";
}
