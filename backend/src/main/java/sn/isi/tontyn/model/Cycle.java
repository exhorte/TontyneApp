package sn.isi.tontyn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "cycle")
@Getter @Setter @NoArgsConstructor
public class Cycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int numero;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut = "EN_COURS";

    @ManyToOne(optional = false)
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @ManyToOne
    @JoinColumn(name = "beneficiaire_id")
    private Membre beneficiaire;
}
