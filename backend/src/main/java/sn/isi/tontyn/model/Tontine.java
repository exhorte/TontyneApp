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
    private LocalDate dateCreation = LocalDate.now();
    private String statut = "ACTIVE";
}
