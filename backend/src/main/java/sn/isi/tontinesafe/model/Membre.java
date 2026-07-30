package sn.isi.tontinesafe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "membre")
@Getter @Setter @NoArgsConstructor
public class Membre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateAdhesion = LocalDate.now();
    private String roleGroupe;        // GESTIONNAIRE ou MEMBRE
    private int ordreTour;
    private String statut = "ACTIF";

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;
}
