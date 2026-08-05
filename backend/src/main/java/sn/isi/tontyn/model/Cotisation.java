package sn.isi.tontyn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotisation")
@Getter @Setter @NoArgsConstructor
public class Cotisation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double montant;
    private LocalDateTime date = LocalDateTime.now();
    private String statut = "EN_ATTENTE";

    @ManyToOne(optional = false)
    @JoinColumn(name = "cycle_id")
    private Cycle cycle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "membre_id")
    private Membre membre;
}
