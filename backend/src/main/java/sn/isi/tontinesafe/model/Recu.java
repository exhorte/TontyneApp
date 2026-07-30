package sn.isi.tontinesafe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "recu")
@Getter @Setter @NoArgsConstructor
public class Recu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;
    private LocalDateTime dateEmission = LocalDateTime.now();
    private double montant;

    @OneToOne(optional = false)
    @JoinColumn(name = "paiement_id", unique = true)
    private Paiement paiement;
}
