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
    /** EN_ATTENTE, PAYEE, ou EN_RETARD (echeance du cycle depassee, non payee). */
    private String statut = "EN_ATTENTE";

    /**
     * Date d'envoi de la derniere relance au membre. Permet a {@code RelanceService}
     * d'espacer les rappels au lieu d'en envoyer un chaque jour tant que le retard dure.
     */
    private LocalDateTime derniereRelance;

    /**
     * Penalite de retard effectivement portee au debit du membre, en francs CFA.
     *
     * <p>Elle est calculee une seule fois, au passage en {@code EN_RETARD}, a
     * partir du taux fixe sur la tontine. Le montant est fige plutot que
     * recalcule a la volee : le membre doit pouvoir constater ce qu'il doit
     * sans que la somme varie si le gestionnaire modifie le bareme par la
     * suite. Le gestionnaire peut la lever
     * ({@code CotisationService.leverPenalite}).</p>
     */
    private double penalite = 0;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cycle_id")
    private Cycle cycle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "membre_id")
    private Membre membre;

    /** Somme reellement exigible : cotisation majoree de l'eventuelle penalite. */
    public double montantDu() {
        return montant + penalite;
    }
}
