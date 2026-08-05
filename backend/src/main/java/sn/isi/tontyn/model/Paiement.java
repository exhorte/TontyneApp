package sn.isi.tontyn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
@Getter @Setter @NoArgsConstructor
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double montant;
    private LocalDateTime date = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private MethodePaiement methode;

    private String reference;
    private String statut = "INITIE";

    @OneToOne(optional = false)
    @JoinColumn(name = "cotisation_id", unique = true)
    private Cotisation cotisation;
}
